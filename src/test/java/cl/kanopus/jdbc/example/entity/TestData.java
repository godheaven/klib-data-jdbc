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
import cl.kanopus.jdbc.entity.annotation.ColumnGroup;
import cl.kanopus.jdbc.entity.annotation.JoinTable;
import cl.kanopus.jdbc.entity.annotation.Table;
import cl.kanopus.jdbc.example.entity.enums.Color;
import cl.kanopus.jdbc.example.entity.enums.Status;
import cl.kanopus.jdbc.util.parser.EnumParser;
import cl.kanopus.jdbc.util.parser.JsonListParser;
import cl.kanopus.jdbc.util.parser.JsonParser;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * Entity class representing the 'tmp_test_data' table.
 * This class is used to map the database table to a Java object.
 * It includes fields for various data types, including enums and JSON data.
 * The class is annotated with Lombok annotations to generate boilerplate code
 * such as getters, setters, equals, and hashCode methods.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Table(name = "tmp_test_data", sequence = "tmp_test_data_pk_test_data_seq", keys = {"pk_test_data"})
public class TestData extends Mapping {

    @Column(name = "pk_test_data", serial = true)
    private long id;

    @Column(name = "td_system_id")
    private long systemId;

    @Column(name = "td_login_id", length = 10)
    private String loginId;

    @Column(name = "td_date")
    private Date date;

    @Column(name = "td_local_date")
    private LocalDate localDate;

    @Column(name = "td_local_date_time")
    private LocalDateTime localDateTime;

    @Column(name = "td_status", parser = EnumParser.class, parserResult = Status.class)
    private Status status;

    @Column(name = "td_color_id", parser = EnumParser.class, parserResult = Color.class)
    private Color color;

    @Column(name = "td_color_json", parser = JsonListParser.class, parserResult = Color.class)
    private List<Color> colors;

    @Column(name = "td_data_json", parser = JsonParser.class, parserResult = JsonData.class)
    private JsonData json;

    @Column(name = "td_list_json", parser = JsonListParser.class, parserResult = JsonData.class)
    private List<JsonData> jsonList;

    @JoinTable(table = TestType.class, foreignKey = "fk_test_type", lazy = true)
    private TestType type;

    @ColumnGroup(result = TestDataGroup.class, nullable = true)
    private TestDataGroup group;


    @Data
    public static class TestDataGroup {

        @Column(name = "td_text", length = 100, auditable = false)
        private String text;

    }

    @Data
    public static class JsonData {

        private int id;
        private String text;
        private Date date;
        private boolean enabled;

    }
}
