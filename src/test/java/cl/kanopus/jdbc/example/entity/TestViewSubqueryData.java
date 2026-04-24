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
package cl.kanopus.jdbc.example.entity;

import cl.kanopus.jdbc.entity.Mapping;
import cl.kanopus.jdbc.entity.annotation.Column;
import cl.kanopus.jdbc.entity.annotation.View;
import cl.kanopus.jdbc.example.entity.enums.Color;
import cl.kanopus.jdbc.util.parser.EnumParser;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity class representing the 'tmp_test_data' view. This class is used to map the database view to a Java object. It includes fields for various data types, including enums and JSON data. The class is annotated with Lombok annotations to generate boilerplate code such as getters, setters, equals,
 * and hashCode methods.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@View("SELECT * FROM (select d.td_login_id, d.td_color_id, d.td_date FROM tmp_test_data d INNER JOIN " + "tmp_test_type t ON d.fk_test_type = t.pk_test_type) AS subquery")
public class TestViewSubqueryData extends Mapping {

    @Column(name = "td_login_id", length = 10)
    private String loginId;

    @Column(name = "td_color_id", parser = EnumParser.class, parserResult = Color.class)
    private Color color;
}
