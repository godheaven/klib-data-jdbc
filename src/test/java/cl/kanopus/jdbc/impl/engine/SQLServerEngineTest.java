/*-
 * !--
 * For support and inquiries regarding this library, please contact:
 *   soporte@kanopus.cl
 *
 * Project website:
 *   https://www.kanopus.cl
 * %%
 * Copyright (C) 2025 Pablo Díaz Saavedra
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * --!
 */
package cl.kanopus.jdbc.impl.engine;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SQLServerEngineTest {

    @Test
    void testPrepareSQL2Engine() {

        String result = SQLServerEngine.getInstance().prepareSQL2Engine("SELECT count(*) FROM table");
        Assertions.assertEquals("SELECT count_big(*) FROM table", result);

        String result2 = SQLServerEngine.getInstance().prepareSQL2Engine("SELECT count(*) FROM table UNION SELECT count(*) FROM table2");
        Assertions.assertEquals("SELECT count_big(*) FROM table UNION SELECT count_big(*) FROM table2", result2);

        String result3 = SQLServerEngine.getInstance().prepareSQL2Engine("SELECT * FROM table WHERE field1 LIKE '%' || :param1 || '%' AND field2 LIKE '%'||:param2 LIMIT 1");
        Assertions.assertEquals("SELECT * FROM table WHERE field1 LIKE '%' + :param1 + '%' AND field2 LIKE '%'+:param2 LIMIT 1", result3);

        String result4 = SQLServerEngine.getInstance().prepareSQL2Engine("SELECT * FROM table WHERE field1::date = :param1 AND field_2::date >= :param2 AND field-3::date >= :param3");
        Assertions.assertEquals("SELECT * FROM table WHERE CAST(field1 as date) = :param1 AND CAST(field_2 as date) >= :param2 AND CAST(field-3 as date) >= :param3", result4);
    }

    @Test
    void prepareSQL2EngineDoesNotTouchStringLiterals() {
        String sql = "SELECT 'a||b' AS x, \"col||name\", 'count(*)', 'v::date' FROM t WHERE c = 'it''s || ok' AND d LIKE '%'||:p";
        Assertions.assertEquals("SELECT 'a||b' AS x, \"col||name\", 'count(*)', 'v::date' FROM t WHERE c = 'it''s || ok' AND d LIKE '%'+:p", SQLServerEngine.getInstance().prepareSQL2Engine(sql));
        Assertions.assertNull(SQLServerEngine.getInstance().prepareSQL2Engine(null));
    }

    @Test
    void prepareSQL2EngineTranslatesTimestampsAndDelete() {
        SQLServerEngine engine = SQLServerEngine.getInstance();
        Assertions.assertEquals("SELECT * FROM t WHERE c BETWEEN CONVERT(DATETIME2, :s, 23) AND CONVERT(DATETIME2, :e, 120)",
                engine.prepareSQL2Engine("SELECT * FROM t WHERE c BETWEEN TO_TIMESTAMP(:s, 'YYYY-MM-DD') AND TO_TIMESTAMP(:e, 'YYYY-MM-DD HH24:MI:SS')"));
        Assertions.assertEquals("DELETE t1 FROM tbl t1 WHERE t1.id=:id", engine.prepareSQL2Engine("DELETE FROM tbl t1 WHERE t1.id=:id"));
        Assertions.assertEquals("DELETE FROM tbl WHERE id=:id", engine.prepareSQL2Engine("DELETE FROM tbl WHERE id=:id"));
        // idempotent
        String once = engine.prepareSQL2Engine("DELETE FROM tbl t1 WHERE t1.a::date = TO_TIMESTAMP(:p, 'YYYY-MM-DD') AND x LIKE '%'||:q");
        Assertions.assertEquals(once, engine.prepareSQL2Engine(once));
    }

    @Test
    void paginationAddsOrderByWhenMissing() {
        SQLServerEngine engine = SQLServerEngine.getInstance();
        Assertions.assertEquals("SELECT * FROM t ORDER BY (SELECT NULL) offset 0 rows fetch next 10 rows only ", engine.createSqlPagination("SELECT * FROM t", 10, 0).toString());
        Assertions.assertEquals("SELECT * FROM t ORDER BY id offset 5 rows fetch next 10 rows only ", engine.createSqlPagination("SELECT * FROM t ORDER BY id", 10, 5).toString());
        // an ORDER BY inside a sub-select or a literal does not count
        Assertions.assertEquals("SELECT * FROM (SELECT TOP 5 * FROM t ORDER BY id) x WHERE n = 'order by' ORDER BY (SELECT NULL) offset 0 rows fetch next 1 rows only ",
                engine.createSqlPagination("SELECT * FROM (SELECT TOP 5 * FROM t ORDER BY id) x WHERE n = 'order by'", 1, 0).toString());
    }

    @Test
    void dialectFragments() {
        SQLServerEngine engine = SQLServerEngine.getInstance();
        Assertions.assertEquals("SELECT NEXT VALUE FOR seq", engine.createSqlNextval("seq"));
        Assertions.assertEquals("CAST(c AS DATE)", engine.castToDate("c"));
        Assertions.assertEquals("CONVERT(DATETIME2, :p, 23)", engine.toTimestamp("p", CustomEngine.DATE_PATTERN));
        Assertions.assertEquals("CONVERT(DATETIME2, :p, 120)", engine.toTimestamp("p", CustomEngine.DATETIME_PATTERN));
        Assertions.assertEquals("'%'+:p", engine.concat("'%'", ":p"));
        Assertions.assertEquals("{}", engine.toJsonParameter("{}"));
        Assertions.assertSame(engine, Engine.SQLSERVER.getCustomEngine());
    }
}
