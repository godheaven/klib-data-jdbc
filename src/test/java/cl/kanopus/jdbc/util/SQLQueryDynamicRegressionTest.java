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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cl.kanopus.jdbc.example.entity.TestData;
import cl.kanopus.jdbc.example.entity.TestViewSubqueryData;
import cl.kanopus.jdbc.example.entity.enums.Color;
import cl.kanopus.jdbc.impl.engine.Engine;
import cl.kanopus.jdbc.util.SQLQueryDynamic.Condition;
import cl.kanopus.jdbc.util.SQLQueryDynamic.MatchMode;
import cl.kanopus.jdbc.util.extension.DataType;
import cl.kanopus.jdbc.util.extension.GroupCondition;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for functional bugs of {@link SQLQueryDynamic} fixed in 4.20.0.
 */
class SQLQueryDynamicRegressionTest {

    @Test
    void stringConstructorAcceptsQualifiedColumns() {
        // used to throw NullPointerException because the alias map was null
        SQLQueryDynamic query = new SQLQueryDynamic("SELECT * FROM tbl t");
        query.addCondition("t.id", 1, Condition.EQUAL);
        query.setOrderBy("t.name");
        assertEquals("SELECT * FROM tbl t WHERE t.id=:t.id_0 ORDER BY t.name ASC", query.getSQL());
    }

    @Test
    void stringAndClassConstructorKeepsRawColumns() {
        // the custom SQL may alias the tables, so column names are used as written
        SQLQueryDynamic query = new SQLQueryDynamic("SELECT * FROM tmp_test_data d", TestData.class);
        query.addCondition("d.td_system_id", 5L, Condition.EQUAL);
        assertEquals("SELECT * FROM tmp_test_data d WHERE d.td_system_id=:d.td_system_id_0", query.getSQL());
        assertEquals(TestData.class, query.getClazz());
    }

    @Test
    void numericAndOtherTypesAreAppliedAsFilters() {
        SQLQueryDynamic query = new SQLQueryDynamic("SELECT * FROM tbl");
        query.addCondition("c_bigdecimal", new BigDecimal("10.5"), Condition.EQUAL);
        query.addCondition("c_double", 1.5d, Condition.EQUAL);
        query.addCondition("c_float", 2.5f, Condition.EQUAL);
        query.addCondition("c_short", (short) 3, Condition.EQUAL);
        query.addCondition("c_byte", (byte) 4, Condition.EQUAL);
        query.addCondition("c_biginteger", BigInteger.TEN, Condition.EQUAL);
        UUID uuid = UUID.randomUUID();
        query.addCondition("c_uuid", uuid, Condition.EQUAL);
        query.addCondition("c_offset", OffsetDateTime.now(), Condition.GREATER_THAN);
        Instant instant = Instant.now();
        query.addCondition("c_instant", instant, Condition.LESS_THAN);
        query.addCondition("c_enum", Color.BLACK, Condition.EQUAL);
        query.addConditionIn("c_set", new Object[]{1, 2});

        assertEquals(
                "SELECT * FROM tbl WHERE c_bigdecimal=:c_bigdecimal_0 AND c_double=:c_double_1 AND c_float=:c_float_2 AND c_short=:c_short_3 AND c_byte=:c_byte_4 AND c_biginteger=:c_biginteger_5 AND c_uuid=:c_uuid_6 AND c_offset>:c_offset_7 AND c_instant<:c_instant_8 AND c_enum=:c_enum_9 AND c_set IN (:c_set_10,:c_set_11)",
                query.getSQL());
        assertEquals(new BigDecimal("10.5"), query.getParams().get("c_bigdecimal_0"));
        assertEquals(uuid, query.getParams().get("c_uuid_6"));
        assertEquals(Timestamp.from(instant), query.getParams().get("c_instant_8"));
        assertEquals(2, query.getParams().get("c_enum_9"));
    }

    @Test
    void zeroValuesAreNotApplied() {
        SQLQueryDynamic query = new SQLQueryDynamic("SELECT * FROM tbl");
        query.addCondition("a", BigDecimal.ZERO, Condition.EQUAL);
        query.addCondition("b", 0d, Condition.EQUAL);
        query.addCondition("c", Double.NaN, Condition.EQUAL);
        query.addCondition("d", 0f, Condition.EQUAL);
        query.addCondition("e", (short) 0, Condition.EQUAL);
        query.addCondition("f", BigInteger.ZERO, Condition.EQUAL);
        query.addCondition("g", null, Condition.EQUAL);
        query.addCondition("h", new Object(), Condition.EQUAL);
        query.addCondition("i", Set.of(), Condition.EQUAL);
        assertEquals("SELECT * FROM tbl", query.getSQL());
        assertFalse(query.hasConditions());

        query.addCondition("g", null, Condition.EQUAL, true);
        assertEquals("SELECT * FROM tbl WHERE g=:g_0", query.getSQL());
    }

    @Test
    void countWithOnlyCustomParamsHasNoEmptyWhere() {
        SQLQueryDynamic query = new SQLQueryDynamic("SELECT * FROM tbl");
        query.addCustomParam("p", "x");
        assertEquals("SELECT count(*) FROM tbl", query.getSQLCount());
    }

    @Test
    void countKeepsSimpleQueriesAndWrapsComplexOnes() {
        SQLQueryDynamic simple = new SQLQueryDynamic(TestData.class);
        simple.addCondition("id", 1L, Condition.EQUAL);
        assertEquals("SELECT count(*) FROM tmp_test_data t1 WHERE t1.pk_test_data=:t1.pk_test_data_0", simple.getSQLCount());

        // the first " FROM " belongs to a sub-select inside the column list
        SQLQueryDynamic subquery = new SQLQueryDynamic("SELECT a.id, (SELECT max(b.x) FROM b WHERE b.a = a.id) AS m FROM a");
        subquery.addCondition("a.id", 3, Condition.EQUAL);
        assertEquals("SELECT count(*) FROM a WHERE a.id=:a.id_0", subquery.getSQLCount());

        SQLQueryDynamic distinct = new SQLQueryDynamic("SELECT DISTINCT a.name FROM a");
        distinct.addCondition("a.id", 3, Condition.EQUAL);
        assertEquals("SELECT count(*) FROM (SELECT DISTINCT a.name FROM a WHERE a.id=:a.id_0) count_query", distinct.getSQLCount());

        SQLQueryDynamic groupBy = new SQLQueryDynamic("SELECT a.name, count(*) FROM a GROUP BY a.name");
        assertEquals("SELECT count(*) FROM (SELECT a.name, count(*) FROM a GROUP BY a.name) count_query", groupBy.getSQLCount());

        SQLQueryDynamic view = new SQLQueryDynamic(TestViewSubqueryData.class);
        view.addCondition("loginId", "x", Condition.EQUAL);
        assertEquals(
                "SELECT count(*) FROM (select d.td_login_id, d.td_color_id, d.td_date FROM tmp_test_data d INNER JOIN tmp_test_type t ON d.fk_test_type = t.pk_test_type) AS subquery WHERE UPPER(td_login_id)=:td_login_id_0",
                view.getSQLCount());
    }

    @Test
    void emptyGroupConditionsDoNotBreakFollowingConditions() {
        SQLQueryDynamic query = new SQLQueryDynamic("SELECT * FROM tbl");
        GroupCondition empty = new GroupCondition();
        empty.addCondition(new GroupCondition.Condition("c1", 0, DataType.NUMERIC, MatchMode.EQUAL));
        query.addGroupCondition(empty);
        query.addGroupConditions(List.of(new GroupCondition(), empty));
        query.addCondition("c2", 2, Condition.EQUAL);
        assertEquals("SELECT * FROM tbl WHERE c2=:c2_0", query.getSQL());

        GroupCondition applied = new GroupCondition();
        applied.addCondition(new GroupCondition.Condition("c3", 3, DataType.NUMERIC, MatchMode.EQUAL));
        query.addGroupConditions(List.of(empty, applied, empty));
        assertEquals("SELECT * FROM tbl WHERE c2=:c2_0 AND ((c3=:c3_1))", query.getSQL());
    }

    @Test
    void groupConditionsWithInListAndOr() {
        SQLQueryDynamic query = new SQLQueryDynamic("SELECT * FROM tbl");
        GroupCondition.Condition in = new GroupCondition.Condition("c1", List.of(1, 2), DataType.NUMERIC, MatchMode.IN);
        GroupCondition.Condition or = new GroupCondition.Condition("c2", 0, DataType.NUMERIC, MatchMode.EQUAL);
        or.setConditionsOR(List.of(new GroupCondition.Condition("c3", 0, DataType.NUMERIC, MatchMode.EQUAL)));
        GroupCondition group = new GroupCondition(new java.util.ArrayList<>(List.of(in, or)));
        query.addGroupCondition(group);
        assertEquals("SELECT * FROM tbl WHERE ((c1 IN (:c1_0,:c1_1)))", query.getSQL());
    }

    @Test
    void orConditionWithAllValuesNotAppliedLeavesNoParentheses() {
        SQLQueryDynamic query = new SQLQueryDynamic("SELECT * FROM tbl");
        query.addCondition("c0", 1, Condition.EQUAL);
        query.addConditionOr(new String[]{"c1", "c2"}, new Object[]{0, ""}, new MatchMode[]{MatchMode.EQUAL, MatchMode.TEXT_CONTAINS});
        assertEquals("SELECT * FROM tbl WHERE c0=:c0_0", query.getSQL());
        assertThrows(IllegalArgumentException.class, () -> query.addConditionOr(new String[]{"c1"}, new Object[]{1}, new MatchMode[]{}));
    }

    @Test
    void groupConditionDateTypeValidation() {
        SQLQueryDynamic query = new SQLQueryDynamic("SELECT * FROM tbl");
        GroupCondition group = new GroupCondition();
        group.addCondition(new GroupCondition.Condition("c1", "not a date", DataType.DATE, MatchMode.EQUAL));
        assertThrows(IllegalArgumentException.class, () -> query.addGroupCondition(group));

        GroupCondition invalidMode = new GroupCondition();
        invalidMode.addCondition(new GroupCondition.Condition("c1", "x", DataType.ALPHANUMERIC, MatchMode.BETWEEN));
        assertThrows(IllegalArgumentException.class, () -> query.addGroupCondition(invalidMode));

        GroupCondition missing = new GroupCondition();
        missing.addCondition(new GroupCondition.Condition("c1", "x", null, MatchMode.EQUAL));
        assertThrows(IllegalArgumentException.class, () -> query.addGroupCondition(missing));
    }

    @Test
    void engineSpecificSqlIsGeneratedByTheEngine() {
        SQLQueryDynamic postgres = new SQLQueryDynamic("SELECT * FROM tbl");
        postgres.addCondition("d", LocalDate.of(2025, 1, 2), Condition.GREATER_OR_EQUAL);
        postgres.addConditionLike("n", "x");
        assertEquals("SELECT * FROM tbl WHERE d::date>=:d_0 AND UPPER(n) LIKE '%'||:n_1||'%'", postgres.getSQL());

        SQLQueryDynamic sqlServer = new SQLQueryDynamic("SELECT * FROM tbl");
        sqlServer.setEngine(Engine.SQLSERVER);
        sqlServer.addCondition("d", LocalDate.of(2025, 1, 2), Condition.GREATER_OR_EQUAL);
        sqlServer.addCondition("ts", LocalDateTime.of(2025, 1, 2, 3, 4, 5), Condition.LESS_THAN);
        sqlServer.addCondition("day", new Date(), Condition.EQUAL);
        sqlServer.addConditionLike("n", "x", false, true);
        assertEquals(
                "SELECT * FROM tbl WHERE CAST(d AS DATE)>=:d_0 AND ts<CONVERT(DATETIME2, :ts_1, 120) AND day BETWEEN CONVERT(DATETIME2, :day_2_start, 23) AND CONVERT(DATETIME2, :day_2_end, 120) AND UPPER(n) LIKE :n_3+'%'",
                sqlServer.getSQL());
        assertEquals("2025-01-02 03:04:05", sqlServer.getParams().get("ts_1"));

        SQLQueryDynamic oracle = new SQLQueryDynamic("SELECT * FROM tbl");
        oracle.setEngine(Engine.ORACLE);
        oracle.addCondition("d", new Date(), Condition.LESS_THAN);
        assertEquals("SELECT * FROM tbl WHERE TRUNC(d)<:d_0", oracle.getSQL());

        SQLQueryDynamic db2 = new SQLQueryDynamic("SELECT * FROM tbl");
        db2.setEngine(Engine.DB2);
        db2.addCondition("d", new Date(), Condition.LESS_THAN);
        assertEquals("SELECT * FROM tbl WHERE CAST(d AS DATE)<:d_0", db2.getSQL());

        assertThrows(IllegalStateException.class, () -> db2.setEngine(Engine.POSTGRES));
        SQLQueryDynamic reset = new SQLQueryDynamic("SELECT * FROM tbl");
        reset.setEngine((Engine) null);
        reset.addCondition("d", LocalDate.of(2025, 1, 2), Condition.EQUAL);
        assertTrue(reset.getSQL().contains("d::date=:d_0"));
    }

    @Test
    void dateEqualUsesLocalDay() {
        SQLQueryDynamic query = new SQLQueryDynamic("SELECT * FROM tbl");
        query.addCondition("d", java.sql.Date.valueOf(LocalDate.of(2024, 2, 29)), Condition.EQUAL);
        assertEquals("2024-02-29", query.getParams().get("d_0_start"));
        assertEquals("2024-02-29 23:59:59", query.getParams().get("d_0_end"));
    }

    @Test
    void customParamsUseRootLocaleUppercase() {
        java.util.Locale previous = java.util.Locale.getDefault();
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("tr-TR"));
            SQLQueryDynamic query = new SQLQueryDynamic("SELECT * FROM tbl");
            query.addCustomParam("p", "title");
            query.addConditionLike("c", "istanbul");
            assertEquals("TITLE", query.getParams().get("p"));
            assertEquals("ISTANBUL", query.getParams().get("c_0"));
        } finally {
            java.util.Locale.setDefault(previous);
        }
    }

    @Test
    void likesSmartIgnoresNullColumns() {
        SQLQueryDynamic query = new SQLQueryDynamic("SELECT * FROM tbl");
        query.addConditionLikesSmart((String[]) null, "abc");
        query.addConditionLikesSmart(new String[]{"a"}, "   ");
        assertEquals("SELECT * FROM tbl", query.getSQL());
    }
}
