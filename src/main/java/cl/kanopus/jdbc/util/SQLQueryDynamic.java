package cl.kanopus.jdbc.util;

import cl.kanopus.common.enums.EnumIdentifiable;
import cl.kanopus.common.util.Utils;
import cl.kanopus.jdbc.entity.Mapping;
import cl.kanopus.common.data.enums.SortOrder;
import cl.kanopus.jdbc.util.extension.DataType;
import cl.kanopus.jdbc.util.extension.GroupCondition;
import cl.kanopus.jdbc.util.extension.OrderBy;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class allows you to generate Dynamic SQL which can be used in search
 * engines. Unlike other classes to generate Dynamic SQL, this class prevents us
 * from having to put conditions when they apply filters, and that automatically
 * determines how to apply filters.
 *
 * For example: If we add a filter with the method addCondition(String column,
 * Object value, String condition) Example 1) if the object attribute value is a
 * string, the filter should be added only if the String is non-null and not
 * equal to "". Example 2) if the object attribute value is a Integer or Long or
 * Float, the filter should be added only if the values is non-null and not
 * equal to 0.
 *
 * @author Pablo Diaz Saavedra
 * @email pabloandres.diazsaavedra@gmail.com
 */
@SuppressWarnings("all")
public class SQLQueryDynamic {

    public enum Condition {

        EQUAL("="), LESS_THAN("<"), LESS_OR_EQUAL("<="), GREATER_THAN(">"), GREATER_OR_EQUAL(">="), NOT_EQUAL("<>");

        private final String name;

        Condition(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum MatchMode {
        EQUAL,
        LESS_THAN,
        LESS_OR_EQUAL,
        GREATER_THAN,
        GREATER_OR_EQUAL,
        NOT_EQUAL,
        TEXT_CONTAINS,
        TEXT_STARTS_WITH,
        TEXT_ENDS_WITH,
        IN,
        BETWEEN
    }

    public enum IS {

        IS_NULL(" IS NULL"), IS_NOT_NULL(" IS NOT NULL");

        private final String condition;

        IS(String condition) {
            this.condition = condition;
        }

        @Override
        public String toString() {
            return condition;
        }

    }

    public enum Operator {
        OR, AND;

        public String toSentence() {
            return " ".concat(name()).concat(" ");
        }
    }

    private int levelConditions = 0;
    private final String sqlSelect;
    protected final StringBuilder sqlWhere = new StringBuilder();
    protected final HashMap<String, Object> sqlParams = new HashMap<>();
    private Integer limit;
    private int offset = 0;
    private long totalResultCount = 0;
    private String[] sqlOrderBy;
    private boolean[] sqlOrderByDesc;

    protected int index = 0;
    private boolean enableOrderByWithLower = false;
    private boolean enableUppercaseAutomatically = true;
    private boolean enablePrefixParam = false;
    private boolean clauseWhereAutomatically = true;
    private final Class<? extends Mapping> clazz;
    private boolean loadAll;
    private String prefixParam;

    private Map<String, String> propertiesTranslationMap;

    public SQLQueryDynamic(String sqlSelect) {
        this.clazz = null;
        this.sqlSelect = sqlSelect;
        this.prefixParam = Utils.generateRandomText(5);
    }

    public SQLQueryDynamic(String sqlSelect, Class<? extends Mapping> clazz) {
        this.clazz = clazz;
        this.sqlSelect = sqlSelect;
        this.prefixParam = Utils.generateRandomText(5);
    }

    public SQLQueryDynamic(Class<? extends Mapping> clazz) {
        this.clazz = clazz;
        this.sqlSelect = JdbcCache.sqlBase(clazz);
        this.propertiesTranslationMap = JdbcCache.translationMap(clazz);
        this.prefixParam = Utils.generateRandomText(5);
    }

    public SQLQueryDynamic(Class<? extends Mapping> clazz, boolean loadAll) {
        this.clazz = clazz;
        this.loadAll = loadAll;
        this.sqlSelect = JdbcCache.sqlBase(clazz, loadAll);
        this.propertiesTranslationMap = JdbcCache.translationMap(clazz);
        this.prefixParam = Utils.generateRandomText(5);
    }

    public boolean isLimited() {
        return limit != null;
    }

    public boolean isSorted() {
        return sqlOrderBy != null && sqlOrderBy.length > 0;
    }

    public boolean isLoadAll() {
        return loadAll;
    }

    public Class<? extends Mapping> getClazz() {
        return clazz;
    }

    public void setEnableOrderByWithLower(boolean enableOrderByWithLower) {
        this.enableOrderByWithLower = enableOrderByWithLower;
    }

    public void setEnableUppercaseAutomatically(boolean enableUppercaseAutomatically) {
        this.enableUppercaseAutomatically = enableUppercaseAutomatically;
    }

    public void setEnablePrefixParam(boolean enablePrefixParam) {
        this.enablePrefixParam = enablePrefixParam;
    }

    public void setClauseWhereAutomatically(boolean clauseWhereAutomatically) {
        this.clauseWhereAutomatically = clauseWhereAutomatically;
    }

    public HashMap<String, Object> getParams() {
        return sqlParams;
    }

    public void setPrefixParam(String prefixParam) {
        this.prefixParam = prefixParam;
    }

    protected String generateParameterName(String input) {
        return enablePrefixParam ? prefixParam.concat("_").concat(input) : input;
    }

    public Integer getLimit() {
        return limit;
    }

    public int getOffset() {
        return offset;
    }

    protected boolean hasToIncludeOperator() {
        return (index > 0 && !sqlWhere.toString().endsWith("("));
    }

    public long getTotalResultCount() {
        return totalResultCount;
    }

    public void setTotalResultCount(long totalResultCount) {
        this.totalResultCount = totalResultCount;
    }

    public final void addCustomParam(String name, Object value) {
        
        if (value instanceof String) {
            sqlParams.put(generateParameterName(name), ((String) value).toUpperCase());
        } else if (value instanceof EnumIdentifiable) {
            sqlParams.put(generateParameterName(name), ((EnumIdentifiable<?>) value).getId());
        } else if (value instanceof Enum) {
            sqlParams.put(generateParameterName(name), ((Enum<?>) value).name());
        } else if (value instanceof Boolean) {
            sqlParams.put(generateParameterName(name), ((Boolean) value));
        } else {
            sqlParams.put(generateParameterName(name), value);
        }
    }

    public void addCustomCondition(String customCondition) {
        sqlWhere.append(hasToIncludeOperator() ? " AND " : "");
        sqlWhere.append(customCondition);
        index++;
    }

    public void addConditionIs(String column, IS is) {
        addConditionIs(Operator.AND, column, is);
    }

    protected void addConditionIs(Operator operator, String column, IS is) {
        sqlWhere.append(hasToIncludeOperator() ? operator.toSentence() : "");
        sqlWhere.append(column);
        sqlWhere.append(is);
        index++;
    }

    public void addCondition(String column, Object value, Condition condition) {
        this.addCondition(column, value, condition, false);
    }

    public void addCondition(String column, Object value, Condition condition, boolean forceCondition) {
        addCondition(Operator.AND, column, value, condition, forceCondition);
    }

    protected void addCondition(Operator operator, String column, Object value, Condition condition, boolean forceCondition) {
        boolean apply = (forceCondition) ? true : checkToApply(value);
        if (apply) {
            String parameterName = generateParameterName(column + "_" + index);
            sqlWhere.append(hasToIncludeOperator() ? operator.toSentence() : "");

            if (value instanceof Date && condition == Condition.EQUAL) {
                String parameterNameStart = generateParameterName(parameterName + "_start");
                String parameterNameEnd = generateParameterName(parameterName + "_end");
                String dateStr = Utils.getDateFormat((Date) value, "yyyy-MM-dd");

                sqlWhere.append(column);
                sqlWhere.append(" BETWEEN TO_TIMESTAMP(:").append(parameterNameStart).append(", 'YYYY-MM-DD')"); //Postgresql
                sqlWhere.append(" AND TO_TIMESTAMP(:").append(parameterNameEnd).append(", 'YYYY-MM-DD HH24:MI:SS')"); //Postgresql
                sqlParams.put(parameterNameStart, dateStr);
                sqlParams.put(parameterNameEnd, dateStr + " 23:59:59");
            } else if (value instanceof Date || value instanceof LocalDate) {
                sqlWhere.append(column).append("::date"); //Postgresql
                sqlWhere.append(condition);
                sqlWhere.append(":").append(parameterName);
                sqlParams.put(parameterName, value);
            } else if (value instanceof LocalDateTime) {
                String datetimeStr = Utils.getDateTimeFormat((LocalDateTime) value, "yyyy-MM-dd HH:mm:ss");

                sqlWhere.append(column);
                sqlWhere.append(condition);
                sqlWhere.append("TO_TIMESTAMP(:").append(parameterName).append(", 'YYYY-MM-DD HH24:MI:SS')"); //Postgresql
                sqlParams.put(parameterName, datetimeStr);
            } else {
                sqlWhere.append((value instanceof String && enableUppercaseAutomatically) ? "UPPER(" + column + ")" : column);
                sqlWhere.append(condition);
                sqlWhere.append(":").append(parameterName);
                if (value instanceof String) {
                    sqlParams.put(parameterName, ((String) value).toUpperCase());
                } else if (value instanceof EnumIdentifiable) {
                    sqlParams.put(parameterName, ((EnumIdentifiable<?>) value).getId());
                } else if (value instanceof Enum) {
                    sqlParams.put(parameterName, ((Enum<?>) value).name());
                } else if (value instanceof Boolean) {
                    sqlParams.put(parameterName, ((Boolean) value));
                } else {
                    sqlParams.put(parameterName, value);
                }
            }
            index++;
        }
    }

    public void addConditionLike(String column, String value) {
        addConditionLike(column, value, true, true);
    }

    public void addConditionLike(String column, String value, boolean percentAtStart, boolean percentAtEnd) {
        addConditionLike(Operator.AND, column, value, percentAtStart, percentAtEnd);
    }

    protected void addConditionLike(Operator operator, String column, String value, boolean percentAtStart, boolean percentAtEnd) {
        if (value != null && value.trim().compareTo("") != 0) {
            String parameterName = generateParameterName(column + "_" + index);
            sqlWhere.append(hasToIncludeOperator() ? operator.toSentence() : "");
            String pInicio = (percentAtStart) ? "'%'||" : "";
            String pFinal = (percentAtEnd) ? "||'%'" : "";

            sqlWhere.append(enableUppercaseAutomatically ? "UPPER(" + column + ")" : column);
            sqlWhere.append(" LIKE ").append(pInicio).append(":").append(parameterName).append(pFinal);

            sqlParams.put(parameterName, value.trim().toUpperCase());
            index++;
        }
    }

    public void addConditionLikesSmart(String column, String value) {
        addConditionLikesSmart(new String[]{column}, value, " ");
    }

    public void addConditionLikesSmart(String[] columns, String value) {
        addConditionLikesSmart(columns, value, " ");
    }

    public void addConditionLikesSmart(String[] columns, String value, String splitRegex) {
        if (value == null) {
            return;
        }
        String[] splitValues = value.split(splitRegex);
        List<String> listValues = new ArrayList<>();
        for (String sv : splitValues) {
            if (!sv.trim().isEmpty()) {
                listValues.add(sv);
            }
        }

        if (listValues.isEmpty()) {
            return;
        }

        String[] values = listValues.toArray(new String[0]);

        boolean hasClauseWhere = index > 0;
        boolean percentAtStart = true;
        boolean percentAtEnd = true;
        int countItems = 0;
        StringBuilder internalSQL = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            internalSQL.append(i == 0 ? "(" : " AND (");
            for (int j = 0; j < columns.length; j++) {
                countItems++;
                String parameterName = generateParameterName(columns[j] + "_" + index);
                String pInicio = (percentAtStart) ? "'%'||" : "";
                String pFinal = (percentAtEnd) ? "||'%'" : "";

                internalSQL.append((j == 0) ? "" : " OR ");
                internalSQL.append(enableUppercaseAutomatically ? "UPPER(" + columns[j] + ")" : columns[j]);
                internalSQL.append(" LIKE ").append(pInicio).append(":").append(parameterName).append(pFinal);
                sqlParams.put(parameterName, values[i].trim().toUpperCase());
                index++;
            }
            internalSQL.append(")");
        }

        if (countItems > 0) {
            sqlWhere.append((clauseWhereAutomatically && !hasClauseWhere) ? "" : " AND ");
            sqlWhere.append("(").append(internalSQL.toString()).append(")");
        }
    }

    public void addConditionOr(String[] columns, Object[] values, MatchMode[] matchModes) {

        if (values != null) {
            if (columns.length != values.length) {
                throw new IllegalArgumentException(
                        "QueryDynamic: The number of columns should be equal to the number of values?");
            }

            boolean apply = checkToApply(values);
            if (apply) {

                // BUSCAMOS LAS POSICIONES DE LOS PARENTESIS IZQ
                int indexLeftParenthesis = calculeLeftParenthesisIndex(values);

                // BUSCAMOS LAS POSICIONES DE LOS PARENTESIS DER
                int indexRightParenthesis = calculeRightParenthesisIndex(values);

                sqlWhere.append(hasToIncludeOperator() ? Operator.AND.toSentence() : "");
                for (int i = 0; i < columns.length; ++i) {
                    String leftParenthesis = (i == indexLeftParenthesis) ? "(" : "";
                    String rightParenthesis = (i == indexRightParenthesis) ? ")" : "";

                    sqlWhere.append(leftParenthesis);

                    switch (matchModes[i]) {
                        case TEXT_CONTAINS:
                            addConditionLike(Operator.OR, columns[i], String.valueOf(values[i]), true, true);
                            break;
                        case TEXT_ENDS_WITH:
                            addConditionLike(Operator.OR, columns[i], String.valueOf(values[i]), true, false);
                            break;
                        case TEXT_STARTS_WITH:
                            addConditionLike(Operator.OR, columns[i], String.valueOf(values[i]), false, true);
                            break;
                        case IN:
                            if (!(values[i] instanceof Object[])) {
                                addConditionIn(Operator.OR, columns[i], new Object[]{values[i]});
                            } else {
                                addConditionIn(Operator.OR, columns[i], (Object[]) values[i]);
                            }
                            break;
                        case BETWEEN:
                            if (!(values[i] instanceof Object[])) {
                                throw new IllegalArgumentException("QueryDynamic: MatchMode.BETWEEN must be Array with 2 values");
                            }
                            Object[] bValues = (Object[]) values[i];
                            addConditionBetween(Operator.OR, columns[i], bValues[0], bValues[1]);
                            break;
                        case EQUAL:
                        case NOT_EQUAL:
                        case GREATER_OR_EQUAL:
                        case GREATER_THAN:
                        case LESS_THAN:
                        case LESS_OR_EQUAL:
                            addCondition(Operator.OR, columns[i], values[i], convertMatchMode2Condition(matchModes[i]), false);
                            break;
                    }
                    sqlWhere.append(rightParenthesis);

                }
            }
        }

    }

    public void addConditionIn(String column, List<String> values) {
        boolean apply = checkToApply(values);
        if (apply) {
            addConditionIn(Operator.AND, column, values.toArray());
        }
    }

    public void addConditionIn(String column, Object[] values) {
        addConditionIn(Operator.AND, column, values);
    }

    protected void addConditionIn(Operator operator, String column, Object[] values) {
        boolean apply = checkToApply(values);
        if (apply) {
            sqlWhere.append(hasToIncludeOperator() ? operator.toSentence() : "");
            sqlWhere.append((values[0] instanceof String && enableUppercaseAutomatically) ? "UPPER(" + column + ")" : column);
            sqlWhere.append(" IN (");
            for (int i = 0; i < values.length; i++) {
                String parameterName = generateParameterName(column + "_" + index);
                sqlWhere.append(i == 0 ? ":" : ",:").append(parameterName);
                if (values[i] instanceof String) {
                    sqlParams.put(parameterName, ((String) values[i]).toUpperCase());
                } else if (values[i] instanceof Enum) {
                    if (values[i] instanceof EnumIdentifiable) {
                        sqlParams.put(parameterName, ((EnumIdentifiable) values[i]).getId());
                    } else {
                        sqlParams.put(parameterName, ((Enum) values[i]).name());
                    }

                } else {
                    sqlParams.put(parameterName, values[i]);
                }
                index++;
            }
            sqlWhere.append(")");
        }
    }

    public void addConditionBetween(String column, Object value1, Object value2) {
        addConditionBetween(Operator.AND, column, value1, value2);
    }

    protected void addConditionBetween(Operator operator, String column, Object value1, Object value2) {
        if (value1 != null && value2 != null) {
            String parameterNameStart = generateParameterName(column + "_" + index + "_start");
            String parameterNameEnd = generateParameterName(column + "_" + index + "_end");

            sqlWhere.append(hasToIncludeOperator() ? operator.toSentence() : "");
            sqlWhere.append(column);
            sqlWhere.append(" BETWEEN ");
            sqlWhere.append(":").append(parameterNameStart);
            sqlWhere.append(" AND ");
            sqlWhere.append(":").append(parameterNameEnd);

            sqlParams.put(parameterNameStart, value1);
            sqlParams.put(parameterNameEnd, value2);
            index++;
        }
    }

    public void addConditionJsonOr(String column, String expression, Object[] values) {
        boolean apply = checkToApply(values);
        if (apply) {

            // BUSCAMOS LAS POSICIONES DE LOS PARENTESIS IZQ
            int indexIzq = calculeLeftParenthesisIndex(values);

            // BUSCAMOS LAS POSICIONES DE LOS PARENTESIS DER
            int indexDer = calculeRightParenthesisIndex(values);

            for (int i = 0; i < values.length; i++) {
                String leftParenthesis = (i == indexIzq) ? "(" : "";
                String rightParenthesis = (i == indexDer) ? ")" : "";
                if (values[i] != null) {
                    String parameterName = generateParameterName(column + "_" + index);
                    sqlWhere.append(hasToIncludeOperator() ? ((i == 0) ? " AND " : " OR ") : "");
                    sqlWhere.append(leftParenthesis);
                    sqlWhere.append(column).append(" ");
                    sqlWhere.append(expression.replace("${value}", values[i] + "")); //@TODO: debe ser escapado o preparar la sentencia
                    //sqlWhere.append(expression.replace("${value}", ":" + parameterName));
                    sqlWhere.append(rightParenthesis);
                    sqlParams.put(parameterName,
                            (values[i] instanceof String) ? (((String) values[i]).trim().toUpperCase()) : values[i]);
                    index++;
                }
            }
        }

    }

    public final void setOrderBy(String column) {
        setOrderBy(column, SortOrder.ASCENDING);
    }

    public final void setOrderBy(String column, SortOrder sortOrder) {
        boolean desc = (sortOrder == SortOrder.DESCENDING);
        sqlOrderBy = (propertiesTranslationMap != null && propertiesTranslationMap.containsKey(column))
                ? new String[]{propertiesTranslationMap.get(column)} : new String[]{column};
        sqlOrderByDesc = new boolean[]{desc};
    }

    public final void setOrderBy(String columns[], SortOrder[] sortOrders) {

        if (columns.length != sortOrders.length) {
            throw new IllegalArgumentException(
                    "QueryDynamic: The number of columns should be equal to the number of sortOrders");
        }

        sqlOrderBy = new String[columns.length];
        sqlOrderByDesc = new boolean[sortOrders.length];

        for (int i = 0; i < columns.length; i++) {
            sqlOrderBy[i] = (propertiesTranslationMap != null && propertiesTranslationMap.containsKey(columns[i])) ? propertiesTranslationMap.get(columns[i]) : columns[i];
            sqlOrderByDesc[i] = sortOrders[i] == SortOrder.DESCENDING;
        }
    }

    public final void setOrderBy(List<String> columns, SortOrder sort) {
        sqlOrderBy = new String[columns.size()];
        sqlOrderByDesc = new boolean[columns.size()];

        for (int i = 0; i < columns.size(); i++) {
            sqlOrderBy[i] = (propertiesTranslationMap != null && propertiesTranslationMap.containsKey(columns.get(i))) ? propertiesTranslationMap.get(columns.get(i)) : columns.get(i);
            sqlOrderByDesc[i] = sort == SortOrder.DESCENDING;
        }
    }

    public final void setOrderBy(String column, String defaultColumn, SortOrder sortOrder) {
        setOrderBy(Utils.isNullOrEmpty(column) ? defaultColumn : column, sortOrder);
    }

    public void setLimit(Integer limit) {
        if (limit != null && limit > 0) {
            this.limit = limit;
        }
    }

    public void setOffset(Integer offset) {
        if (offset != null && offset >= 0) {
            this.offset = offset;
        }
    }

    public String getSQL() {
        StringBuilder sql = new StringBuilder();
        sql.append(sqlSelect);
        if (index > 0) {
            sql.append(" WHERE ").append(sqlWhere);
        }
        // Aqui se genera el order by
        if (sqlOrderBy != null && sqlOrderBy.length > 0) {
            sql.append(" ORDER BY ");
            for (int i = 0; i < sqlOrderBy.length; i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                sql.append(enableOrderByWithLower ? "LOWER(" : "");
                sql.append(sqlOrderBy[i]);
                sql.append(enableOrderByWithLower ? ") " : " ");
                sql.append(sqlOrderByDesc[i] ? "DESC" : "ASC");
            }
        }
        return sql.toString();
    }

    public String getSQLCount() {
        StringBuilder sql = new StringBuilder();
        if (sqlSelect != null) {
            int indexFrom = sqlSelect.toUpperCase().indexOf(" FROM ");
            sql.append("SELECT count(*)").append(sqlSelect.substring(indexFrom));
        }
        if (!sqlParams.isEmpty() || index > 0) {
            sql.append(" WHERE ").append(sqlWhere);
        }
        return sql.toString();
    }

    protected boolean checkToApply(Object value) {
        if (value instanceof Integer) {
            Integer i = (Integer) value;
            return (i != 0);
        } else if (value instanceof Long) {
            Long l = (Long) value;
            return (l != 0);
        } else if (value instanceof String) {
            String s = (String) value;
            return (s.trim().compareTo("") != 0);
        } else if (value instanceof Date) {
            Date d = (Date) value;
            return (d != null);
        } else if (value instanceof Time) {
            Time t = (Time) value;
            return (t != null);
        } else if (value instanceof LocalDate) {
            LocalDate d = (LocalDate) value;
            return (d != null);
        } else if (value instanceof LocalDateTime) {
            LocalDateTime d = (LocalDateTime) value;
            return (d != null);
        } else if (value instanceof Enum) {
            return (value != null);
        } else if (value instanceof Boolean) {
            return (value != null);
        } else if (value instanceof Object[]) {
            boolean allNulls = true;
            for (Object v : (Object[]) value) {
                if (v != null) {
                    allNulls = false;
                    break;
                }
            }
            return !allNulls;
        } else if (value instanceof List) {
            return !((List) value).isEmpty();
        } else {
            return false;
        }
    }

    protected int calculeLeftParenthesisIndex(Object[] values) {
        int idx = -1;
        for (int i = 0; i < values.length; i++) {
            if (values[i] != null && idx == -1) {
                idx = i;
            }
        }
        return idx;
    }

    protected int calculeRightParenthesisIndex(Object[] values) {
        int idx = -1;
        for (int i = values.length - 1; i >= 0; i--) {
            if (values[i] != null && idx == -1) {
                idx = i;
            }
        }
        return idx;
    }

    protected Condition convertMatchMode2Condition(MatchMode match) {
        Condition condition;
        try {
            condition = Condition.valueOf(match.name());
        } catch (Exception ex) {
            condition = null;
        }
        return condition;
    }

    public void setOrderBy(List<OrderBy> customOrderBy, List<OrderBy> defaultOrderBy) {
        List<OrderBy> ordersBy = !Utils.isNullOrEmpty(customOrderBy) ? customOrderBy : defaultOrderBy;
        String[] columns = new String[ordersBy.size()];
        SortOrder[] sorts = new SortOrder[ordersBy.size()];

        for (int i = 0; i < ordersBy.size(); i++) {
            columns[i] = ordersBy.get(i).getColumn();
            sorts[i] = ordersBy.get(i).getSort();
        }

        this.setOrderBy(columns, sorts);
    }

    public void addGroupCondition(GroupCondition groupCondition) {
        List<GroupCondition> groupConditions = new ArrayList<>();
        groupConditions.add(groupCondition);
        addGroupConditions(groupConditions);
    }

    public void addGroupConditions(List<GroupCondition> groupConditions) {
        if (!Utils.isNullOrEmpty(groupConditions)) {
            this.sqlWhere.append((this.index == 0) ? "(" : " AND (");
            for (int i = 0; i < groupConditions.size(); i++) {
                if (i > 0) {
                    this.sqlWhere.append(" OR ");
                }
                generateConditions(groupConditions.get(i).getConditions());
            }
            this.sqlWhere.append(")");

            //removing empty parentheses when condition groups cannot be applied 
            Utils.replaceAll(sqlWhere, " AND (())", "");
        }

    }

    private void generateConditions(List<GroupCondition.Condition> conditions) {
        this.levelConditions++;
        if (!Utils.isNullOrEmpty(conditions)) {
            if (levelConditions > 1) {
                this.sqlWhere.append(" AND ");
            }
            this.sqlWhere.append("(");
            for (int i = 0; i < conditions.size(); i++) {
                GroupCondition.Condition condition = conditions.get(i);
                if (!Utils.isNullOrEmpty(condition.getConditionsOR())) {
                    List<GroupCondition.Condition> conditionsOr = new ArrayList<>();
                    conditionsOr.add(condition);
                    conditionsOr.addAll(condition.getConditionsOR());
                    addConditionOr(conditionsOr);
                } else {
                    addConditionAnd(condition);
                }
            }
            this.sqlWhere.append(")");
        }
        this.levelConditions--;
    }

    private void addConditionOr(List<GroupCondition.Condition> conditionsOr) {

        String[] columns = new String[conditionsOr.size()];
        Object[] values = new Object[conditionsOr.size()];
        MatchMode[] matchModes = new MatchMode[conditionsOr.size()];
        for (int i = 0; i < conditionsOr.size(); i++) {
            columns[i] = conditionsOr.get(i).getColumn();
            values[i] = conditionsOr.get(i).getValue();
            matchModes[i] = conditionsOr.get(i).getMatchMode();
            checkDataTypeAndMatchMode(conditionsOr.get(i));
        }
        addConditionOr(columns, values, matchModes);
    }

    private void addConditionAnd(GroupCondition.Condition condition) {
        checkDataTypeAndMatchMode(condition);

        switch (condition.getMatchMode()) {
            case TEXT_CONTAINS:
                addConditionLike(Operator.AND, condition.getColumn(), String.valueOf(condition.getValue()), true, true);
                break;
            case TEXT_ENDS_WITH:
                addConditionLike(Operator.AND, condition.getColumn(), String.valueOf(condition.getValue()), true, false);
                break;
            case TEXT_STARTS_WITH:
                addConditionLike(Operator.AND, condition.getColumn(), String.valueOf(condition.getValue()), false, true);
                break;
            case IN:
                if (!(condition.getValue() instanceof Object[])) {
                    addConditionIn(Operator.AND, condition.getColumn(), new Object[]{condition.getValue()});
                } else {
                    addConditionIn(Operator.AND, condition.getColumn(), (Object[]) condition.getValue());
                }
                break;
            case BETWEEN:
                if (condition.getValue() instanceof Object[]) {
                    Object[] bValues = (Object[]) condition.getValue();
                    addConditionBetween(Operator.AND, condition.getColumn(), bValues[0], bValues[1]);
                } else if (condition.getValue() instanceof List) {
                    List bValues = (List) condition.getValue();
                    addConditionBetween(Operator.AND, condition.getColumn(), bValues.get(0), bValues.get(1));
                } else {
                    throw new IllegalArgumentException("QueryDynamic: MatchMode.BETWEEN must be Array with 2 values");
                }

                break;
            case EQUAL:
            case NOT_EQUAL:
            case GREATER_OR_EQUAL:
            case GREATER_THAN:
            case LESS_THAN:
            case LESS_OR_EQUAL:
                addCondition(Operator.AND, condition.getColumn(), condition.getValue(), convertMatchMode2Condition(condition.getMatchMode()), false);
                break;
        }

    }

    private void checkDataTypeAndMatchMode(GroupCondition.Condition condition) {
        boolean contains = Arrays.stream(condition.getDataType().getMatchModes()).anyMatch(condition.getMatchMode()::equals);
        if (!contains) {
            throw new IllegalArgumentException("QueryDynamic: MatchMode." + condition.getMatchMode() + " is not supported with " + condition.getDataType());
        }

        if (condition.getDataType() == DataType.DATE && !(condition.getValue() instanceof Date || condition.getValue() instanceof LocalDate || condition.getValue() instanceof List)) {
            throw new IllegalArgumentException("QueryDynamic: DataType.DATE cannot be applied to " + condition.getValue());
        }

    }
}
