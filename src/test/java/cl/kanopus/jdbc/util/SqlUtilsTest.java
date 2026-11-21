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
package cl.kanopus.jdbc.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SqlUtilsTest {

    @Test
    void identifiers() {
        Assertions.assertEquals("a", SqlUtils.requireIdentifier("a"));
        Assertions.assertEquals("t1.col_1", SqlUtils.requireIdentifier("t1.col_1"));
        Assertions.assertEquals("s.t.c$", SqlUtils.requireIdentifier("s.t.c$"));
        for (String invalid : new String[]{null, "", "1a", "a.", ".a", "a..b", "a b", "a-b", "a;b", "a'b", "a\"b", "a.b.c.d", "a(b)", "*"}) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> SqlUtils.requireIdentifier(invalid), String.valueOf(invalid));
        }
    }

    @Test
    void indexOfTopLevel() {
        String sql = "SELECT (SELECT x FROM y) AS z, 'FROM' FROM tbl WHERE a = 'where' ORDER  BY b";
        Assertions.assertEquals(sql.indexOf(" FROM tbl") + 1, SqlUtils.indexOfTopLevel(sql, "FROM"));
        Assertions.assertEquals(sql.indexOf("WHERE"), SqlUtils.indexOfTopLevel(sql, "where"));
        Assertions.assertEquals(sql.indexOf("ORDER"), SqlUtils.indexOfTopLevel(sql, "ORDER BY"));
        Assertions.assertEquals(-1, SqlUtils.indexOfTopLevel(sql, "GROUP BY"));
        Assertions.assertEquals(-1, SqlUtils.indexOfTopLevel("SELECT from_date FROM_X", "FROM"));
        Assertions.assertEquals(-1, SqlUtils.indexOfTopLevel(null, "FROM"));
        Assertions.assertEquals(-1, SqlUtils.indexOfTopLevel("SELECT 1", ""));
        Assertions.assertEquals(-1, SqlUtils.indexOfTopLevel("ORDERBY x", "ORDER BY"));
        Assertions.assertEquals(-1, SqlUtils.indexOfTopLevel("SELECT 'unterminated FROM", "FROM"));
    }

    @Test
    void transformOutsideLiterals() {
        Assertions.assertEquals("X 'a' X \"a\" X", SqlUtils.transformOutsideLiterals("a 'a' a \"a\" a", s -> s.replace("a", "X")));
        Assertions.assertEquals("", SqlUtils.transformOutsideLiterals("", s -> "x"));
        Assertions.assertNull(SqlUtils.transformOutsideLiterals(null, s -> "x"));
    }
}
