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

class DB2EngineTest {

    @Test
    void testCreateSqlPagination() {
        String sql = "SELECT * FROM table";

        // Test with limit and offset
        String result = DB2Engine.getInstance().createSqlPagination(sql, 10, 20).toString();
        Assertions.assertEquals("SELECT * FROM table OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY", result);

        // Test with limit only
        String result2 = DB2Engine.getInstance().createSqlPagination(sql, 10, 0).toString();
        Assertions.assertEquals("SELECT * FROM table FETCH NEXT 10 ROWS ONLY", result2);
    }

    @Test
    void testCreateSqlNextval() {
        String result = DB2Engine.getInstance().createSqlNextval("my_sequence");
        Assertions.assertEquals("VALUES NEXT VALUE FOR my_sequence", result);
    }

    @Test
    void testPrepareSQL2Engine() {
        String sql = "SELECT * FROM table";
        String result = DB2Engine.getInstance().prepareSQL2Engine(sql);
        Assertions.assertEquals(sql, result);
    }
}
