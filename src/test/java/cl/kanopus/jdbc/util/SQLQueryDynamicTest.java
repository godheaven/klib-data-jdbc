package cl.kanopus.jdbc.util;

import cl.kanopus.common.data.enums.SortOrder;
import cl.kanopus.jdbc.util.extension.DataType;
import cl.kanopus.jdbc.util.extension.GroupCondition;
import cl.kanopus.jdbc.util.extension.OrderBy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 *
 * @author Pablo Diaz Saavedra
 * @email pabloandres.diazsaavedra@gmail.com
 *
 */
@ExtendWith(SpringExtension.class)
public class SQLQueryDynamicTest {

    public enum EnumValue {
        ALFA,
        BETA,
        GAMA
    };

    @Test
    public void validateSQLBase() {
        final String SQL = "SELECT * FROM tbl_user";
        SQLQueryDynamic sqlQuery = new SQLQueryDynamic(SQL);
        Assertions.assertEquals(SQL, sqlQuery.getSQL());
    }

    @Test
    public void validateSQLBaseWithOnlyParams() {
        final String SQL = "SELECT * FROM tbl_user";
        SQLQueryDynamic sqlQuery = new SQLQueryDynamic(SQL);
        sqlQuery.setEnableUppercaseAutomatically(true);
        sqlQuery.addCustomParam("test", "value1");
        Assertions.assertEquals(SQL, sqlQuery.getSQL());
        Assertions.assertTrue(sqlQuery.getParams().size() == 1);
        Assertions.assertEquals("VALUE1", sqlQuery.getParams().get("test"));
    }

    @Test
    public void validateConditionWhere() {
        final String parameterName = "userid";
        final int parameterValue = 123;

        SQLQueryDynamic sqlQuery = new SQLQueryDynamic("SELECT * FROM tbl_user");
        sqlQuery.addCondition(parameterName, parameterValue, SQLQueryDynamic.Condition.EQUAL);

        Assertions.assertTrue(!sqlQuery.getParams().isEmpty(), "userid parameter had to be added.");

        for (String key : sqlQuery.getParams().keySet()) {
            Object value = sqlQuery.getParams().get(key);
            Assertions.assertTrue((value instanceof Integer), "parameter value must be instance of integer.");
            Assertions.assertTrue(((Integer) value == parameterValue), "parameter value must be (" + parameterValue + ").");
        }

    }

    @Test
    public void validateConditionInWithIntegers() {
        final String parameterName = "field";
        final Integer[] parameterValues = {1, 2, 3};

        SQLQueryDynamic sqlQuery = new SQLQueryDynamic("SELECT * FROM table");
        sqlQuery.addConditionIn(parameterName, parameterValues);

        Assertions.assertTrue(!sqlQuery.getParams().isEmpty(), "userid parameter had to be added.");
        Assertions.assertEquals("SELECT * FROM table WHERE field IN (:field_0,:field_1,:field_2)", sqlQuery.getSQL());

        for (int i = 0; i < parameterValues.length; i++) {
            String key = "field_" + i;
            Assertions.assertTrue(parameterValues[i].equals(sqlQuery.getParams().get(key)));
        }

    }

    @Test
    public void validateConditionInWithEnum() {
        final String parameterName = "field";
        final EnumValue parameterValues[] = {EnumValue.ALFA, EnumValue.BETA, EnumValue.GAMA};

        SQLQueryDynamic sqlQuery = new SQLQueryDynamic("SELECT * FROM table");
        sqlQuery.addConditionIn(parameterName, parameterValues);

        Assertions.assertTrue(!sqlQuery.getParams().isEmpty(), "userid parameter had to be added.");
        Assertions.assertEquals("SELECT * FROM table WHERE field IN (:field_0,:field_1,:field_2)", sqlQuery.getSQL());

        for (int i = 0; i < parameterValues.length; i++) {
            String key = "field_" + i;
            Assertions.assertEquals(parameterValues[i].name(), sqlQuery.getParams().get(key));
        }
    }

    @Test
    public void validateConditionInWithString() {
        final String parameterName = "field";
        final String parameterValues[] = {"a", "b", "c"};

        SQLQueryDynamic sqlQuery = new SQLQueryDynamic("SELECT * FROM table");
        sqlQuery.addConditionIn(parameterName, parameterValues);

        Assertions.assertTrue(!sqlQuery.getParams().isEmpty(), "userid parameter had to be added.");
        Assertions.assertEquals("SELECT * FROM table WHERE UPPER(field) IN (:field_0,:field_1,:field_2)", sqlQuery.getSQL());

        for (int i = 0; i < parameterValues.length; i++) {
            String key = "field_" + i;
            Assertions.assertEquals(parameterValues[i].toUpperCase(), sqlQuery.getParams().get(key));
        }

    }

    @Test
    public void validateConditionEqualsDate() {
        final String parameterName = "field";
        final Date date = new Date();

        SQLQueryDynamic sqlQuery = new SQLQueryDynamic("SELECT * FROM table");
        sqlQuery.addCondition(parameterName, date, SQLQueryDynamic.Condition.EQUAL);
        Assertions.assertTrue(!sqlQuery.getParams().isEmpty());
        Assertions.assertEquals("SELECT * FROM table WHERE field BETWEEN TO_TIMESTAMP(:field_0_start, 'YYYY-MM-DD') AND TO_TIMESTAMP(:field_0_end, 'YYYY-MM-DD HH24:MI:SS')", sqlQuery.getSQL());
    }

    @Test
    public void validateConditionLocaltime() {
        final String parameterName = "field";
        final LocalDateTime date = LocalDateTime.now();

        SQLQueryDynamic sqlQuery = new SQLQueryDynamic("SELECT * FROM table");
        sqlQuery.addCondition(parameterName, date, SQLQueryDynamic.Condition.LESS_THAN);
        Assertions.assertTrue(!sqlQuery.getParams().isEmpty());
        Assertions.assertEquals("SELECT * FROM table WHERE field<TO_TIMESTAMP(:field_0, 'YYYY-MM-DD HH24:MI:SS')", sqlQuery.getSQL());
    }

    @Test
    public void validateConditionLike() {
        SQLQueryDynamic sqlQuery1 = new SQLQueryDynamic("SELECT * FROM table");
        sqlQuery1.addConditionLike("column1", "value1");

        Assertions.assertTrue(!sqlQuery1.getParams().isEmpty(), "parameters should be added.");
        Assertions.assertEquals("SELECT * FROM table WHERE UPPER(column1) LIKE '%'||:column1_0||'%'", sqlQuery1.getSQL());

    }

    @Test
    public void validateConditionBetween() {
        SQLQueryDynamic sqlQuery1 = new SQLQueryDynamic("SELECT * FROM table");
        sqlQuery1.addCondition("c1", "v1", SQLQueryDynamic.Condition.EQUAL);
        sqlQuery1.addConditionBetween("c2", "v2a", "v2b");

        Assertions.assertTrue(!sqlQuery1.getParams().isEmpty(), "parameters should be added.");
        Assertions.assertEquals("SELECT * FROM table WHERE UPPER(c1)=:c1_0 AND c2 BETWEEN :c2_1_start AND :c2_1_end", sqlQuery1.getSQL());
    }

    @Test
    public void validateConditionLikeSmart() {
        SQLQueryDynamic sqlQuery1 = new SQLQueryDynamic("SELECT * FROM table");
        sqlQuery1.addConditionLikesSmart(new String[]{"usr_first_name", "usr_last_name"}, "AA:BB", ":");
        Assertions.assertTrue(!sqlQuery1.getParams().isEmpty(), "parameters should be added.");
        Assertions.assertEquals(
                "SELECT * FROM table WHERE ((UPPER(usr_first_name) LIKE '%'||:usr_first_name_0||'%' OR UPPER(usr_last_name) LIKE '%'||:usr_last_name_1||'%') AND (UPPER(usr_first_name) LIKE '%'||:usr_first_name_2||'%' OR UPPER(usr_last_name) LIKE '%'||:usr_last_name_3||'%'))",
                sqlQuery1.getSQL());

        SQLQueryDynamic sqlQuery2 = new SQLQueryDynamic("SELECT * FROM table");
        sqlQuery2.addConditionLikesSmart(new String[]{"usr_first_name", "usr_last_name"}, "AA", ":");
        Assertions.assertTrue(!sqlQuery2.getParams().isEmpty(), "parameters should be added.");
        Assertions.assertEquals("SELECT * FROM table WHERE ((UPPER(usr_first_name) LIKE '%'||:usr_first_name_0||'%' OR UPPER(usr_last_name) LIKE '%'||:usr_last_name_1||'%'))", sqlQuery2.getSQL());

        SQLQueryDynamic sqlQuery3 = new SQLQueryDynamic("SELECT * FROM table");
        sqlQuery3.addConditionLikesSmart(new String[]{"usr_first_name", "usr_last_name"}, "AA:BB:CC", ":");
        Assertions.assertTrue(!sqlQuery3.getParams().isEmpty(), "parameters should be added.");
        Assertions.assertTrue("AA".equalsIgnoreCase((String) sqlQuery3.getParams().get("usr_first_name_0")));
        Assertions.assertTrue("AA".equalsIgnoreCase((String) sqlQuery3.getParams().get("usr_last_name_1")));
        Assertions.assertTrue("BB".equalsIgnoreCase((String) sqlQuery3.getParams().get("usr_first_name_2")));
        Assertions.assertTrue("BB".equalsIgnoreCase((String) sqlQuery3.getParams().get("usr_last_name_3")));
        Assertions.assertTrue("CC".equalsIgnoreCase((String) sqlQuery3.getParams().get("usr_first_name_4")));
        Assertions.assertTrue("CC".equalsIgnoreCase((String) sqlQuery3.getParams().get("usr_last_name_5")));
        Assertions.assertEquals(
                "SELECT * FROM table WHERE ((UPPER(usr_first_name) LIKE '%'||:usr_first_name_0||'%' OR UPPER(usr_last_name) LIKE '%'||:usr_last_name_1||'%') AND (UPPER(usr_first_name) LIKE '%'||:usr_first_name_2||'%' OR UPPER(usr_last_name) LIKE '%'||:usr_last_name_3||'%') AND (UPPER(usr_first_name) LIKE '%'||:usr_first_name_4||'%' OR UPPER(usr_last_name) LIKE '%'||:usr_last_name_5||'%'))",
                sqlQuery3.getSQL());

        SQLQueryDynamic sqlQuery4 = new SQLQueryDynamic("SELECT * FROM table");
        sqlQuery4.addConditionLikesSmart(new String[]{"usr_first_name", "usr_last_name"}, ",  venegas", ",");
        Assertions.assertTrue(!sqlQuery4.getParams().isEmpty(), "parameters should be added.");
        Assertions.assertEquals("SELECT * FROM table WHERE ((UPPER(usr_first_name) LIKE '%'||:usr_first_name_0||'%' OR UPPER(usr_last_name) LIKE '%'||:usr_last_name_1||'%'))", sqlQuery4.getSQL());
        Assertions.assertTrue("venegas".equalsIgnoreCase((String) sqlQuery4.getParams().get("usr_first_name_0")));
        Assertions.assertTrue("venegas".equalsIgnoreCase((String) sqlQuery4.getParams().get("usr_last_name_1")));

    }

    @Test
    public void validateConditionOr() {
        String[] columns = new String[]{"c1", "c2", "c3", "c4", "c5", "c6", "c7", "c8", "c9", "c10"};
        Object[] values = new Object[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        SQLQueryDynamic.MatchMode[] matchModes = new SQLQueryDynamic.MatchMode[]{
            SQLQueryDynamic.MatchMode.EQUAL,
            SQLQueryDynamic.MatchMode.GREATER_OR_EQUAL,
            SQLQueryDynamic.MatchMode.GREATER_THAN,
            SQLQueryDynamic.MatchMode.IN,
            SQLQueryDynamic.MatchMode.LESS_OR_EQUAL,
            SQLQueryDynamic.MatchMode.LESS_THAN,
            SQLQueryDynamic.MatchMode.NOT_EQUAL,
            SQLQueryDynamic.MatchMode.TEXT_CONTAINS,
            SQLQueryDynamic.MatchMode.TEXT_ENDS_WITH,
            SQLQueryDynamic.MatchMode.TEXT_STARTS_WITH,};

        SQLQueryDynamic sqlQuery1 = new SQLQueryDynamic("SELECT * FROM table");
        sqlQuery1.addConditionOr(columns, values, matchModes);
        Assertions.assertTrue(!sqlQuery1.getParams().isEmpty(), "parameters should be added.");
        Assertions.assertEquals(
                "SELECT * FROM table WHERE (c1=:c1_0 OR c2>=:c2_1 OR c3>:c3_2 OR c4 IN (:c4_3) OR c5<=:c5_4 OR c6<:c6_5 OR c7<>:c7_6 OR UPPER(c8) LIKE '%'||:c8_7||'%' OR UPPER(c9) LIKE '%'||:c9_8 OR UPPER(c10) LIKE :c10_9||'%')",
                sqlQuery1.getSQL());

        SQLQueryDynamic sqlQuery2 = new SQLQueryDynamic("SELECT * FROM table");
        sqlQuery2.addCondition("c0", "v0", SQLQueryDynamic.Condition.EQUAL);
        sqlQuery2.addConditionOr(columns, values, matchModes);
        Assertions.assertTrue(!sqlQuery2.getParams().isEmpty(), "parameters should be added.");
        Assertions.assertEquals(
                "SELECT * FROM table WHERE UPPER(c0)=:c0_0 AND (c1=:c1_1 OR c2>=:c2_2 OR c3>:c3_3 OR c4 IN (:c4_4) OR c5<=:c5_5 OR c6<:c6_6 OR c7<>:c7_7 OR UPPER(c8) LIKE '%'||:c8_8||'%' OR UPPER(c9) LIKE '%'||:c9_9 OR UPPER(c10) LIKE :c10_10||'%')",
                sqlQuery2.getSQL());

        SQLQueryDynamic sqlQuery3 = new SQLQueryDynamic("SELECT * FROM table");
        sqlQuery3.addConditionOr(columns, values, matchModes);
        sqlQuery3.addCondition("c0", "v0", SQLQueryDynamic.Condition.EQUAL);
        Assertions.assertTrue(!sqlQuery3.getParams().isEmpty(), "parameters should be added.");
        Assertions.assertEquals(
                "SELECT * FROM table WHERE (c1=:c1_0 OR c2>=:c2_1 OR c3>:c3_2 OR c4 IN (:c4_3) OR c5<=:c5_4 OR c6<:c6_5 OR c7<>:c7_6 OR UPPER(c8) LIKE '%'||:c8_7||'%' OR UPPER(c9) LIKE '%'||:c9_8 OR UPPER(c10) LIKE :c10_9||'%') AND UPPER(c0)=:c0_10",
                sqlQuery3.getSQL());

    }

    @Test
    public void validateConditionOrWithNulls() {
        String[] columns = new String[]{"c1", "c2", "c3"};

        SQLQueryDynamic.MatchMode[] matchModes = new SQLQueryDynamic.MatchMode[]{
            SQLQueryDynamic.MatchMode.EQUAL,
            SQLQueryDynamic.MatchMode.GREATER_OR_EQUAL,
            SQLQueryDynamic.MatchMode.NOT_EQUAL,};

        SQLQueryDynamic sqlQuery1 = new SQLQueryDynamic("SELECT * FROM table");
        sqlQuery1.addConditionOr(columns, new Object[]{1, null, 3}, matchModes);
        Assertions.assertTrue(!sqlQuery1.getParams().isEmpty(), "parameters should be added.");
        Assertions.assertEquals("SELECT * FROM table WHERE (c1=:c1_0 OR c3<>:c3_1)", sqlQuery1.getSQL());

        SQLQueryDynamic sqlQuery2 = new SQLQueryDynamic("SELECT * FROM table");
        sqlQuery2.addConditionOr(columns, null, matchModes);
        Assertions.assertTrue(sqlQuery2.getParams().isEmpty(), "parameters should be empty.");
        Assertions.assertEquals("SELECT * FROM table", sqlQuery2.getSQL());

        SQLQueryDynamic sqlQuery3 = new SQLQueryDynamic("SELECT * FROM table");
        sqlQuery3.addCondition("c0", "v0", SQLQueryDynamic.Condition.EQUAL);
        sqlQuery3.addConditionOr(columns, new Object[]{null, null, null}, matchModes);

        Assertions.assertTrue(!sqlQuery3.getParams().isEmpty(), "parameters should be added.");
        Assertions.assertEquals("SELECT * FROM table WHERE UPPER(c0)=:c0_0", sqlQuery3.getSQL());

    }

    @Test
    public void validateConditionOrBetween() {
        String[] columns = new String[]{"c1", "c2", "c3"};
        Object[] values = new Object[]{1, 2, new Object[]{50, 100}};
        SQLQueryDynamic.MatchMode[] matchModes = new SQLQueryDynamic.MatchMode[]{
            SQLQueryDynamic.MatchMode.EQUAL,
            SQLQueryDynamic.MatchMode.GREATER_OR_EQUAL,
            SQLQueryDynamic.MatchMode.BETWEEN,};

        SQLQueryDynamic sqlQuery1 = new SQLQueryDynamic("SELECT * FROM table");
        sqlQuery1.addConditionOr(columns, values, matchModes);

        Assertions.assertTrue(!sqlQuery1.getParams().isEmpty(), "parameters should be added.");
        Assertions.assertEquals("SELECT * FROM table WHERE (c1=:c1_0 OR c2>=:c2_1 OR c3 BETWEEN :c3_2_start AND :c3_2_end)", sqlQuery1.getSQL());

    }

    @Test
    public void validateCustomCondition() {
        SQLQueryDynamic sqlQuery = new SQLQueryDynamic("SELECT * FROM tbl_user");
        sqlQuery.addCustomCondition("column IN(SELECT columnX FROM custom_table)");
        Assertions.assertTrue("SELECT * FROM tbl_user WHERE column IN(SELECT columnX FROM custom_table)".equalsIgnoreCase(sqlQuery.getSQL()), "Validation sql");
    }

    @Test
    public void validateUppercaseDisabled() {
        SQLQueryDynamic sqlQuery1 = new SQLQueryDynamic("SELECT * FROM table");
        sqlQuery1.setEnableUppercaseAutomatically(false);
        sqlQuery1.addConditionLike("column1", "value1");

        Assertions.assertTrue(!sqlQuery1.getParams().isEmpty(), "parameters should be added.");
        Assertions.assertEquals("SELECT * FROM table WHERE column1 LIKE '%'||:column1_0||'%'", sqlQuery1.getSQL());

        SQLQueryDynamic sqlQuery2 = new SQLQueryDynamic("SELECT * FROM table");
        sqlQuery2.setEnableUppercaseAutomatically(true);
        sqlQuery2.addConditionLike("column1", "value1");

        Assertions.assertTrue(!sqlQuery2.getParams().isEmpty(), "parameters should be added.");
        Assertions.assertEquals("SELECT * FROM table WHERE UPPER(column1) LIKE '%'||:column1_0||'%'", sqlQuery2.getSQL());

    }

    @Test
    public void validateOrderByColumn() {
        SQLQueryDynamic sqlQuery1 = new SQLQueryDynamic("SELECT * FROM table");
        sqlQuery1.setOrderBy("column1");

        Assertions.assertEquals("SELECT * FROM table ORDER BY column1 ASC", sqlQuery1.getSQL());
    }

    @Test
    public void validateOrderByColumnDescending() {
        SQLQueryDynamic sqlQuery1 = new SQLQueryDynamic("SELECT * FROM table");
        sqlQuery1.setOrderBy("column1", SortOrder.DESCENDING);

        Assertions.assertEquals("SELECT * FROM table ORDER BY column1 DESC", sqlQuery1.getSQL());
    }

    @Test
    public void validateOrderByColumnsAndSorted() {
        SQLQueryDynamic sqlQuery1 = new SQLQueryDynamic("SELECT * FROM table");
        sqlQuery1.setOrderBy(new String[]{"column1", "column2"}, new SortOrder[]{SortOrder.ASCENDING, SortOrder.DESCENDING});

        Assertions.assertEquals("SELECT * FROM table ORDER BY column1 ASC, column2 DESC", sqlQuery1.getSQL());
    }

    @Test
    public void validateOrderByColumnsAndSortedInvalid() {
        SQLQueryDynamic sqlQuery1 = new SQLQueryDynamic("SELECT * FROM table");
        assertThrows(IllegalArgumentException.class, () -> {
            sqlQuery1.setOrderBy(new String[]{"column1", "column2"}, new SortOrder[]{SortOrder.ASCENDING});
        });

    }

    @Test
    public void validateSingleDateConditionWithCastxx() {
        SQLQueryDynamic query = new SQLQueryDynamic("SELECT * FROM TABLE");
        List<GroupCondition> groupConditionList = new ArrayList<>();

        //group1
        GroupCondition group1 = new GroupCondition();
        List<GroupCondition.Condition> conditions1 = new ArrayList<>();
        conditions1.add(new GroupCondition.Condition("c1", new Date(), DataType.DATE, SQLQueryDynamic.MatchMode.EQUAL));
        conditions1.add(new GroupCondition.Condition("c2", "v2", DataType.ALPHANUMERIC, SQLQueryDynamic.MatchMode.EQUAL));
        conditions1.add(new GroupCondition.Condition("c3", 3, DataType.NUMERIC, SQLQueryDynamic.MatchMode.EQUAL));
        group1.setConditions(conditions1);
        groupConditionList.add(group1);

        //group2
        GroupCondition group2 = new GroupCondition();
        List<GroupCondition.Condition> conditions2 = new ArrayList<>();
        conditions2.add(new GroupCondition.Condition("c4", new Date(), DataType.DATE, SQLQueryDynamic.MatchMode.LESS_OR_EQUAL));
        conditions2.add(new GroupCondition.Condition("c5", "v5", DataType.ALPHANUMERIC, SQLQueryDynamic.MatchMode.TEXT_CONTAINS));
        conditions2.add(new GroupCondition.Condition("c6", 6, DataType.NUMERIC, SQLQueryDynamic.MatchMode.NOT_EQUAL));
        group2.setConditions(conditions2);
        groupConditionList.add(group2);

        query.addGroupConditions(groupConditionList);
        Assertions.assertEquals("SELECT * FROM TABLE WHERE ((c1 BETWEEN TO_TIMESTAMP(:c1_0_start, 'YYYY-MM-DD') AND TO_TIMESTAMP(:c1_0_end, 'YYYY-MM-DD HH24:MI:SS') AND UPPER(c2)=:c2_1 AND c3=:c3_2) OR (c4::date<=:c4_3 AND UPPER(c5) LIKE '%'||:c5_4||'%' AND c6<>:c6_5))", query.getSQL());

    }

    @Test
    public void validateSingleDateConditionWithCast() {
        SQLQueryDynamic query = new SQLQueryDynamic("SELECT * FROM TABLE");
        List<GroupCondition> groupConditionList = new ArrayList<>();
        GroupCondition groupCondition = new GroupCondition();
        List<GroupCondition.Condition> conditionList = new ArrayList<>();
        GroupCondition.Condition condition = new GroupCondition.Condition();
        condition.setColumn("c1");
        condition.setValue(new Date());
        condition.setMatchMode(SQLQueryDynamic.MatchMode.EQUAL);
        condition.setDataType(DataType.DATE);

        conditionList.add(condition);
        groupCondition.setConditions(conditionList);
        groupConditionList.add(groupCondition);

        query.addGroupConditions(groupConditionList);
        Assertions.assertEquals("SELECT * FROM TABLE WHERE ((c1 BETWEEN TO_TIMESTAMP(:c1_0_start, 'YYYY-MM-DD') AND TO_TIMESTAMP(:c1_0_end, 'YYYY-MM-DD HH24:MI:SS')))", query.getSQL());
    }

    @Test
    public void validateBetweenDateConditionWithCast() {
        SQLQueryDynamic query = new SQLQueryDynamic("SELECT * FROM TABLE");
        List<GroupCondition> groupConditionList = new ArrayList<>();
        GroupCondition groupCondition = new GroupCondition();
        List<GroupCondition.Condition> conditionList = new ArrayList<>();
        GroupCondition.Condition condition = new GroupCondition.Condition();
        condition.setColumn("c1");
        condition.setValue(Arrays.asList(new Date(), new Date()));
        condition.setMatchMode(SQLQueryDynamic.MatchMode.BETWEEN);
        condition.setDataType(DataType.DATE);

        conditionList.add(condition);
        groupCondition.setConditions(conditionList);
        groupConditionList.add(groupCondition);

        query.addGroupConditions(groupConditionList);
        Assertions.assertEquals("SELECT * FROM TABLE WHERE ((c1 BETWEEN :c1_0_start AND :c1_0_end))", query.getSQL());
    }

    @Test
    public void validateGroupConditionSingleEmpty() {
        SQLQueryDynamic query = new SQLQueryDynamic("SELECT * FROM TABLE");

        GroupCondition.Condition condition = new GroupCondition.Condition();
        condition.setColumn("c1");
        condition.setValue(0);
        condition.setMatchMode(SQLQueryDynamic.MatchMode.EQUAL);
        condition.setDataType(DataType.NUMERIC);

        GroupCondition groupCondition = new GroupCondition();
        groupCondition.addCondition(condition);

        query.addGroupCondition(groupCondition);
        Assertions.assertEquals("SELECT * FROM TABLE", query.getSQL());
    }

    @Test
    public void validateGroupConditionMultipleOneEmpty() {
        SQLQueryDynamic query = new SQLQueryDynamic("SELECT * FROM TABLE");

        GroupCondition.Condition c1 = new GroupCondition.Condition();
        c1.setColumn("c1");
        c1.setValue(0);
        c1.setMatchMode(SQLQueryDynamic.MatchMode.EQUAL);
        c1.setDataType(DataType.NUMERIC);

        GroupCondition.Condition c2 = new GroupCondition.Condition();
        c2.setColumn("c2");
        c2.setValue(1);
        c2.setMatchMode(SQLQueryDynamic.MatchMode.EQUAL);
        c2.setDataType(DataType.NUMERIC);

        GroupCondition groupCondition = new GroupCondition();
        groupCondition.addCondition(c1);
        groupCondition.addCondition(c2);

        query.addGroupCondition(groupCondition);
        Assertions.assertEquals("SELECT * FROM TABLE WHERE ((c2=:c2_0))", query.getSQL());
    }

    @Test
    public void validateGroupConditionMultipleEmpty() {
        SQLQueryDynamic query = new SQLQueryDynamic("SELECT * FROM TABLE");
        query.addCondition("c1", "Text", SQLQueryDynamic.Condition.EQUAL);

        GroupCondition.Condition c1 = new GroupCondition.Condition();
        c1.setColumn("gc1");
        c1.setValue(0);
        c1.setMatchMode(SQLQueryDynamic.MatchMode.EQUAL);
        c1.setDataType(DataType.NUMERIC);

        GroupCondition.Condition c2 = new GroupCondition.Condition();
        c2.setColumn("gc2");
        c2.setValue(0);
        c2.setMatchMode(SQLQueryDynamic.MatchMode.EQUAL);
        c2.setDataType(DataType.NUMERIC);

        GroupCondition groupCondition1 = new GroupCondition();
        groupCondition1.addCondition(c1);
        groupCondition1.addCondition(c2);

        GroupCondition groupCondition2 = new GroupCondition();
        groupCondition2.addCondition(c1);
        groupCondition2.addCondition(c2);

        query.addGroupCondition(groupCondition1);
        query.addGroupCondition(groupCondition2);

        Assertions.assertEquals("SELECT * FROM TABLE WHERE UPPER(c1)=:c1_0", query.getSQL());
    }

    @Test
    public void validateOrderByDefault() {
        SQLQueryDynamic query = new SQLQueryDynamic("SELECT * FROM TABLE");
        List<OrderBy> orderByList = new ArrayList<>();
        OrderBy orderBy = new OrderBy();
        orderBy.setColumn("c1");
        orderBy.setSort(SortOrder.ASCENDING);
        orderByList.add(orderBy);
        query.setOrderBy(new ArrayList<>(), orderByList);

        Assertions.assertEquals("SELECT * FROM TABLE ORDER BY c1 ASC", query.getSQL());
    }

    @Test
    public void validateOrderByCustom() {
        SQLQueryDynamic query = new SQLQueryDynamic("SELECT * FROM TABLE");
        List<OrderBy> orderByList = new ArrayList<>();
        List<OrderBy> customOrderByList = new ArrayList<>();

        OrderBy orderBy = new OrderBy();
        orderBy.setColumn("c1");
        orderBy.setSort(SortOrder.ASCENDING);

        OrderBy custom = new OrderBy();
        custom.setColumn("c2");
        custom.setSort(SortOrder.DESCENDING);

        orderByList.add(orderBy);
        customOrderByList.add(custom);
        query.setOrderBy(customOrderByList, orderByList);

        Assertions.assertEquals("SELECT * FROM TABLE ORDER BY c2 DESC", query.getSQL());
    }

    @Test
    public void validateParamsWithAutomaticPrefix() {
        final String SQL = "SELECT * FROM tbl_user";
        SQLQueryDynamic query = new SQLQueryDynamic(SQL);
        query.setEnablePrefixParam(true);
        query.addCondition("column1", "one", SQLQueryDynamic.Condition.EQUAL);
        Assertions.assertTrue(query.getParams().size() == 1);

        for (String param : query.sqlParams.keySet()) {
            Assertions.assertNotNull(param);
        }

        Assertions.assertNotEquals("SELECT * FROM tbl_user WHERE UPPER(column1)=:column1_0", query.getSQL());
    }
}
