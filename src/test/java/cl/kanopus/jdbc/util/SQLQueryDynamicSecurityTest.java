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

import cl.kanopus.common.data.enums.SortOrder;
import cl.kanopus.jdbc.example.entity.TestData;
import cl.kanopus.jdbc.example.entity.TestDataEmpty;
import cl.kanopus.jdbc.exception.DataException;
import cl.kanopus.jdbc.util.SQLQueryDynamic.Condition;
import cl.kanopus.jdbc.util.SQLQueryDynamic.IS;
import cl.kanopus.jdbc.util.SQLQueryDynamic.MatchMode;
import cl.kanopus.jdbc.util.extension.DataType;
import cl.kanopus.jdbc.util.extension.GroupCondition;
import cl.kanopus.jdbc.util.extension.OrderBy;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Regression tests for SQL injection through identifiers, JSON conditions and DELETE without WHERE.
 */
class SQLQueryDynamicSecurityTest {

    private static final String[] MALICIOUS = {"id; DROP TABLE users", "1=1 OR id", "name--", "a b", "(SELECT 1)", "x' OR '1'='1", "t1.name)", "a.b.c.d", "", "LOWER(name)"};

    @ParameterizedTest
    @ValueSource(strings = {"id; DROP TABLE users", "1=1 OR id", "name--", "a b", "(SELECT 1)", "x' OR '1'='1", "t1.name)", "a.b.c.d", "", "LOWER(name)", "name/**/"})
    void rejectsMaliciousColumnInConditions(String column) {
        SQLQueryDynamic query = new SQLQueryDynamic("SELECT * FROM tbl");
        assertThrows(IllegalArgumentException.class, () -> query.addCondition(column, 1, Condition.EQUAL));
        assertThrows(IllegalArgumentException.class, () -> query.addConditionLike(column, "v"));
        assertThrows(IllegalArgumentException.class, () -> query.addConditionIn(column, new Object[]{1}));
        assertThrows(IllegalArgumentException.class, () -> query.addConditionBetween(column, 1, 2));
        assertThrows(IllegalArgumentException.class, () -> query.addConditionIs(column, IS.IS_NULL));
        assertThrows(IllegalArgumentException.class, () -> query.addConditionJson(column, "->> 'a' = ${value}", 1));
        assertThrows(IllegalArgumentException.class, () -> query.addConditionLikesSmart(new String[]{column}, "abc"));
        assertThrows(IllegalArgumentException.class, () -> query.addConditionOr(new String[]{column}, new Object[]{1}, new MatchMode[]{MatchMode.EQUAL}));
        assertFalse(query.hasConditions());
        assertEquals("SELECT * FROM tbl", query.getSQL());
    }

    @Test
    void rejectsMaliciousColumnEvenWhenConditionIsNotApplied() {
        SQLQueryDynamic query = new SQLQueryDynamic("SELECT * FROM tbl");
        assertThrows(IllegalArgumentException.class, () -> query.addCondition("a;b", 0, Condition.EQUAL));
        assertThrows(IllegalArgumentException.class, () -> query.addCondition(null, 1, Condition.EQUAL));
    }

    @Test
    void rejectsMaliciousColumnInOrderBy() {
        SQLQueryDynamic query = new SQLQueryDynamic("SELECT * FROM tbl");
        for (String column : MALICIOUS) {
            assertThrows(IllegalArgumentException.class, () -> query.setOrderBy(column));
            assertThrows(IllegalArgumentException.class, () -> query.setOrderBy(column, SortOrder.DESCENDING));
            assertThrows(IllegalArgumentException.class, () -> query.setOrderBy(new String[]{"ok", column}, new SortOrder[]{SortOrder.ASCENDING, SortOrder.ASCENDING}));
            assertThrows(IllegalArgumentException.class, () -> query.setOrderBy(List.of(column), SortOrder.ASCENDING));

            OrderBy orderBy = new OrderBy();
            orderBy.setColumn(column);
            orderBy.setSort(SortOrder.ASCENDING);
            assertThrows(IllegalArgumentException.class, () -> query.setOrderBy(List.of(orderBy)));
        }
        // a rejected order by must not leave a partial ORDER BY behind
        assertFalse(query.isSorted());
        assertEquals("SELECT * FROM tbl", query.getSQL());
    }

    @Test
    void rejectsMaliciousColumnInGroupConditions() {
        SQLQueryDynamic query = new SQLQueryDynamic("SELECT * FROM tbl");
        GroupCondition group = new GroupCondition();
        group.addCondition(new GroupCondition.Condition("c1 = 1 OR 1", 1, DataType.NUMERIC, MatchMode.EQUAL));
        assertThrows(IllegalArgumentException.class, () -> query.addGroupCondition(group));

        GroupCondition.Condition orCondition = new GroupCondition.Condition("c1", 1, DataType.NUMERIC, MatchMode.EQUAL);
        orCondition.setConditionsOR(List.of(new GroupCondition.Condition("c2) OR (1=1", 2, DataType.NUMERIC, MatchMode.EQUAL)));
        GroupCondition group2 = new GroupCondition();
        group2.addCondition(orCondition);
        SQLQueryDynamic query2 = new SQLQueryDynamic("SELECT * FROM tbl");
        assertThrows(IllegalArgumentException.class, () -> query2.addGroupCondition(group2));
    }

    @Test
    void acceptsMappedPropertiesAndPlainIdentifiers() {
        SQLQueryDynamic query = new SQLQueryDynamic(TestData.class);
        query.addCondition("group.text", "abc", Condition.EQUAL);
        query.addCondition("td_system_id", 1, Condition.EQUAL);
        query.addCondition("t1.td_status", "OK", Condition.EQUAL);
        query.addCondition("public.tmp_test_data.td_color_id", 2, Condition.EQUAL);
        query.setOrderBy("loginId", SortOrder.DESCENDING);
        String sql = query.getSQL();
        assertTrue(sql.endsWith(
                " WHERE UPPER(t1.td_text)=:t1.td_text_0 AND td_system_id=:td_system_id_1 AND UPPER(t1.td_status)=:t1.td_status_2 AND public.tmp_test_data.td_color_id=:public.tmp_test_data.td_color_id_3 ORDER BY t1.td_login_id DESC"),
                sql);
    }

    @Test
    void parseSortOrderOnlyAcceptsAscOrDesc() {
        assertEquals(SortOrder.ASCENDING, SQLQueryDynamic.parseSortOrder(null));
        assertEquals(SortOrder.ASCENDING, SQLQueryDynamic.parseSortOrder(" "));
        assertEquals(SortOrder.ASCENDING, SQLQueryDynamic.parseSortOrder("asc"));
        assertEquals(SortOrder.ASCENDING, SQLQueryDynamic.parseSortOrder("ASCENDING"));
        assertEquals(SortOrder.DESCENDING, SQLQueryDynamic.parseSortOrder("Desc"));
        assertEquals(SortOrder.DESCENDING, SQLQueryDynamic.parseSortOrder("descending"));
        assertThrows(IllegalArgumentException.class, () -> SQLQueryDynamic.parseSortOrder("DESC; DROP TABLE x"));
        assertThrows(IllegalArgumentException.class, () -> SQLQueryDynamic.parseSortOrder("NULLS FIRST"));
    }

    @Test
    void jsonConditionBindsValueAsParameter() {
        SQLQueryDynamic query = new SQLQueryDynamic("SELECT * FROM tbl");
        String attack = "x' OR '1'='1";
        query.addConditionJson("data", "->> 'name' = '${value}'", attack);

        assertEquals("SELECT * FROM tbl WHERE (data ->> 'name' = :data_0)", query.getSQL());
        assertEquals(attack, query.getParams().get("data_0"));
        assertFalse(query.getSQL().contains(attack));
    }

    @Test
    void jsonConditionWithUnquotedPlaceholderAndJsonLiteral() {
        SQLQueryDynamic query = new SQLQueryDynamic("SELECT * FROM tbl");
        query.addConditionJson("data", "->> 'id' = ${value}", 5);
        query.addConditionJson("doc", "@> '{\"id\": ${value}}'::jsonb", 7);

        assertEquals("SELECT * FROM tbl WHERE (data ->> 'id' = :data_0) AND (doc @> :doc_1::jsonb)", query.getSQL());
        assertEquals(5, query.getParams().get("data_0"));
        assertEquals("{\"id\": 7}", query.getParams().get("doc_1"));
    }

    @Test
    void jsonConditionWithSeveralValuesUsesOr() {
        SQLQueryDynamic query = new SQLQueryDynamic("SELECT * FROM tbl");
        query.addCondition("c0", 1, Condition.EQUAL);
        query.addConditionJson("data", "->> 'id' = '${value}'", new Object[]{null, "a", "b"});

        assertEquals("SELECT * FROM tbl WHERE c0=:c0_0 AND (data ->> 'id' = :data_1 OR data ->> 'id' = :data_2)", query.getSQL());
        assertEquals("a", query.getParams().get("data_1"));
        assertEquals("b", query.getParams().get("data_2"));
    }

    @Test
    void deleteWithoutConditionsIsRejected() {
        SQLQueryDynamic query = new SQLQueryDynamic(TestData.class);
        assertThrows(DataException.class, query::getSQLDelete);

        // conditions that are not applied (zero, empty, null) do not count
        query.addCondition("id", 0L, Condition.EQUAL);
        query.addCondition("loginId", "  ", Condition.EQUAL);
        query.addConditionLike("loginId", null);
        query.addCustomParam("p", 1);
        assertThrows(DataException.class, query::getSQLDelete);
    }

    @Test
    void deleteWithConditions() {
        SQLQueryDynamic query = new SQLQueryDynamic(TestData.class);
        query.addCondition("id", 10L, Condition.EQUAL);
        assertEquals("DELETE FROM tmp_test_data t1 WHERE t1.pk_test_data=:t1.pk_test_data_0", query.getSQLDelete());
    }

    @Test
    void deleteWithWhereInBaseSql() {
        SQLQueryDynamic query = new SQLQueryDynamic("SELECT * FROM tbl WHERE a=:a");
        query.addCustomParam("a", 1);
        assertEquals("DELETE FROM tbl WHERE a=:a", query.getSQLDelete());

        query.addCondition("b", 2, Condition.EQUAL);
        assertEquals("DELETE FROM tbl WHERE a=:a AND (b=:b_0)", query.getSQLDelete());
        assertEquals("SELECT * FROM tbl WHERE a=:a AND (b=:b_0)", query.getSQL());
    }

    @Test
    void deleteWithJoinsIsRejected() {
        SQLQueryDynamic query = new SQLQueryDynamic("SELECT * FROM a INNER JOIN b ON a.id=b.id");
        query.addCondition("a.id", 1, Condition.EQUAL);
        assertThrows(DataException.class, query::getSQLDelete);

        SQLQueryDynamic query2 = new SQLQueryDynamic(TestDataEmpty.class);
        query2.addJoinTable(cl.kanopus.jdbc.entity.enums.JoinOperator.INNER_JOIN, cl.kanopus.jdbc.example.entity.TestDataHistory.class, "testDataId");
        query2.addCondition("id", 1L, Condition.EQUAL);
        assertThrows(DataException.class, query2::getSQLDelete);
    }

    @Test
    void deleteWithoutFromIsRejected() {
        SQLQueryDynamic query = new SQLQueryDynamic("VALUES 1");
        query.addCustomCondition("1=1");
        assertThrows(DataException.class, query::getSQLDelete);
    }

    @Test
    void invalidJoinAliasIsRejected() {
        SQLQueryDynamic query = new SQLQueryDynamic(TestDataEmpty.class);
        assertThrows(IllegalArgumentException.class,
                () -> query.addJoinTable(cl.kanopus.jdbc.entity.enums.JoinOperator.INNER_JOIN, "x ON 1=1 --", cl.kanopus.jdbc.example.entity.TestDataHistory.class, "testDataId"));
        assertThrows(IllegalStateException.class, () -> new SQLQueryDynamic("SELECT 1").addJoinTable(cl.kanopus.jdbc.entity.enums.JoinOperator.INNER_JOIN, TestData.class, "id"));
    }
}
