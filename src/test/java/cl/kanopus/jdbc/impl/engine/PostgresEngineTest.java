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
import org.postgresql.util.PGobject;

class PostgresEngineTest {

    private final PostgresEngine engine = PostgresEngine.getInstance();

    @Test
    void pagination() {
        Assertions.assertEquals("SELECT * FROM t limit 10", engine.createSqlPagination("SELECT * FROM t", 10, 0).toString());
        Assertions.assertEquals("SELECT * FROM t limit 10 offset 5", engine.createSqlPagination("SELECT * FROM t", 10, 5).toString());
    }

    @Test
    void dialectFragments() {
        Assertions.assertEquals("SELECT nextval('seq')", engine.createSqlNextval("seq"));
        Assertions.assertEquals("SELECT a::date", engine.prepareSQL2Engine("SELECT a::date"));
        Assertions.assertEquals("c::date", engine.castToDate("c"));
        Assertions.assertEquals("TO_TIMESTAMP(:p, 'YYYY-MM-DD HH24:MI:SS')", engine.toTimestamp("p", CustomEngine.DATETIME_PATTERN));
        Assertions.assertEquals("a||b", engine.concat("a", "b"));
        Assertions.assertSame(engine, Engine.POSTGRES.getCustomEngine());
    }

    @Test
    void jsonIsBoundAsPgObject() {
        Object value = engine.toJsonParameter("{\"a\":1}");
        Assertions.assertInstanceOf(PGobject.class, value);
        Assertions.assertEquals("json", ((PGobject) value).getType());
        Assertions.assertEquals("{\"a\":1}", ((PGobject) value).getValue());
    }
}
