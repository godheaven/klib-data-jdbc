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

}
