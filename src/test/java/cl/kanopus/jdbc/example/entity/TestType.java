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
import cl.kanopus.jdbc.entity.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Entity class representing the 'tmp_test_type' table.
 * This class is used to map the database table to a Java object.
 * It includes fields for the primary key and name.
 * The class is annotated with Lombok annotations to generate boilerplate code
 * such as getters, setters, equals, and hashCode methods.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "tmp_test_type", keys = {"pk_test_type"})
public class TestType extends Mapping {

    @Column(name = "pk_test_type")
    private long id;

    @Column(name = "name", alias = "tt_name", length = 10)
    private String name;

}
