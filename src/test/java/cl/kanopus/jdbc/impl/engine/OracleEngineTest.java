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

class OracleEngineTest {

    private final OracleEngine engine = OracleEngine.getInstance();

    @Test
    void pagination() {
        Assertions.assertEquals("SELECT * FROM (SELECT consulta.*, rownum rn FROM(SELECT * FROM t) consulta ) WHERE rn<= 10", engine.createSqlPagination("SELECT * FROM t", 10, 0).toString());
        Assertions.assertEquals("SELECT * FROM (SELECT consulta.*, rownum rn FROM(SELECT * FROM t) consulta ) WHERE rn<= 30 AND rn > 20",
                engine.createSqlPagination("SELECT * FROM t", 10, 20).toString());
    }

    @Test
    void nextval() {
        Assertions.assertEquals("SELECT my_seq.nextval FROM dual", engine.createSqlNextval("my_seq"));
    }

    @Test
    void prepareSQL2EngineTranslatesDateCastOutsideLiterals() {
        Assertions.assertEquals("SELECT * FROM t WHERE TRUNC(t1.c) = :p AND x = 'y::date' AND n LIKE '%'||:q",
                engine.prepareSQL2Engine("SELECT * FROM t WHERE t1.c::date = :p AND x = 'y::date' AND n LIKE '%'||:q"));
        Assertions.assertEquals("SELECT 1 FROM dual", engine.prepareSQL2Engine("SELECT 1 FROM dual"));
    }

    @Test
    void dialectFragments() {
        Assertions.assertEquals("TRUNC(c)", engine.castToDate("c"));
        Assertions.assertEquals("TO_TIMESTAMP(:p, 'YYYY-MM-DD')", engine.toTimestamp("p", CustomEngine.DATE_PATTERN));
        Assertions.assertEquals("'%'||:p||'%'", engine.concat("'%'", ":p", "'%'"));
        Assertions.assertEquals("[1]", engine.toJsonParameter("[1]"));
        Assertions.assertSame(engine, Engine.ORACLE.getCustomEngine());
    }
}
