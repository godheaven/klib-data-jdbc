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

import cl.kanopus.common.data.enums.SortOrder;
import cl.kanopus.common.enums.EnumIdentifiable;
import cl.kanopus.common.util.Utils;
import cl.kanopus.jdbc.entity.Mapping;
import cl.kanopus.jdbc.entity.annotation.Table;
import cl.kanopus.jdbc.entity.enums.JoinOperator;
import cl.kanopus.jdbc.exception.DataException;
import cl.kanopus.jdbc.impl.engine.CustomEngine;
import cl.kanopus.jdbc.impl.engine.Engine;
import cl.kanopus.jdbc.impl.engine.PostgresEngine;
import cl.kanopus.jdbc.util.extension.DataType;
import cl.kanopus.jdbc.util.extension.GroupCondition;
import cl.kanopus.jdbc.util.extension.OrderBy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This class allows you to generate Dynamic SQL which can be used in search engines. Unlike other classes to generate Dynamic SQL, this class prevents us from having to put conditions when they apply filters, and that automatically determines how to apply filters.
 *
 * <p>
 * For example: If we add a filter with the method addCondition(String column, Object value, String condition) Example 1) if the object attribute value is a string, the filter should be added only if the String is non-null and not equal to "". Example 2) if the object attribute value is a Integer or
 * Long or Float, the filter should be added only if the values is non-null and not equal to 0.
 *
 * <p>
 * Security: column names received by the condition and order methods must be either a property of the mapped class (e.g. {@code "group.text"}) or a plain SQL identifier matching {@link SqlUtils#IDENTIFIER}; anything else is rejected with {@link IllegalArgumentException}. Values are always bound as
 * parameters. Raw SQL can only be added through {@link #addCustomCondition(String)}, which must never receive user input.
 *
 * <p>
 * Instances are not thread safe; create a new instance per query.
 *
 * @author Pablo Diaz Saavedra
 */
@SuppressWarnings("all")
public class SQLQueryDynamic {

    private static final String VALUE_PLACEHOLDER = "${value}";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    protected final StringBuilder sqlJoins = new StringBuilder();
    protected final StringBuilder sqlWhere = new StringBuilder();
    protected final HashMap<String, Object> sqlParams = new HashMap<>();
    private final String sqlSelect;
    private final Class<? extends Mapping> clazz;
    protected int index = 0;
    private int levelConditions = 0;
    private Integer limit;
    private int offset = 0;
    private long totalResultCount = 0;
    private String[] sqlOrderBy;
    private boolean[] sqlOrderByDesc;
    private boolean enableOrderByWithLower = false;
    private boolean enableUppercaseAutomatically = true;
    private boolean enablePrefixParam = false;
    private boolean clauseWhereAutomatically = true;
    private boolean loadAll;
    private String prefixParam;
    private Map<String, String> propertiesTranslationMap;
    private Map<String, String> aliasMap = new HashMap<>();
    private CustomEngine dialect = PostgresEngine.getInstance();
    private Boolean selectHasWhere;

    private int indexJoins = 0;

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
        this(clazz, false);
    }

    public SQLQueryDynamic(Class<? extends Mapping> clazz, boolean loadAll) {
        this.clazz = clazz;
        this.loadAll = loadAll;
        JdbcCache.SqlBase base = JdbcCache.sqlBase(clazz, loadAll);
        this.sqlSelect = base.getSql();
        // defensive copies: joins added to this query must not leak into the shared cache
        this.aliasMap = new HashMap<>(base.getAliasMap());
        this.propertiesTranslationMap = new HashMap<>(JdbcCache.translationMap(clazz));
        this.prefixParam = Utils.generateRandomText(5);
    }

    /**
     * Selects the SQL dialect used to render engine specific fragments (date casts, timestamp parsing and string concatenation). By default the PostgreSQL syntax is generated, which {@code AbstractDAO} translates for the other engines through {@link CustomEngine#prepareSQL2Engine(String)}.
     *
     * @param engine
     *            the target engine
     * @throws IllegalStateException
     *             when conditions were already added
     */
    public void setEngine(Engine engine) {
        setEngine(engine == null ? PostgresEngine.getInstance() : engine.getCustomEngine());
    }

    /**
     * @param customEngine
     *            the dialect implementation
     * @see #setEngine(Engine)
     */
    public void setEngine(CustomEngine customEngine) {
        if (index > 0) {
            throw new IllegalStateException("The engine must be defined before adding conditions");
        }
        this.dialect = customEngine == null ? PostgresEngine.getInstance() : customEngine;
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

    /**
     * @return {@code true} when at least one condition was applied to the WHERE clause
     */
    public boolean hasConditions() {
        return index > 0 && sqlWhere.length() > 0;
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

    public void setLimit(Integer limit) {
        if (limit != null && limit > 0) {
            this.limit = limit;
        }
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        if (offset != null && offset >= 0) {
            this.offset = offset;
        }
    }

    protected boolean hasToIncludeOperator() {
        return (index > 0 && sqlWhere.length() > 0 && sqlWhere.charAt(sqlWhere.length() - 1) != '(');
    }

    public long getTotalResultCount() {
        return totalResultCount;
    }

    public void setTotalResultCount(long totalResultCount) {
        this.totalResultCount = totalResultCount;
    }

    public final void addJoinTable(JoinOperator joinOperator, Class<? extends Mapping> clazz, String foreignKey) {
        addJoinTable(joinOperator, null, clazz, foreignKey);
    }

    public final void addJoinTable(JoinOperator joinOperator, String aliasJoin, Class<? extends Mapping> clazzJoin, String foreignKey) {
        if (this.clazz == null) {
            throw new IllegalStateException("addJoinTable requires a SQLQueryDynamic created from a mapped class");
        }
        if (aliasJoin != null && !aliasJoin.isEmpty() && (aliasJoin.contains(".") || !SqlUtils.IDENTIFIER.matcher(aliasJoin).matches())) {
            throw new IllegalArgumentException("Invalid join alias: '" + aliasJoin + "'");
        }

        // Primary Key
        Table table = this.clazz.getDeclaredAnnotation(Table.class);
        if (table == null) {
            throw new IllegalStateException("Error class " + this.clazz.getName() + " without @Table annotation");
        }
        String alias = Utils.defaultValue(aliasMap.get(table.name()), "t1");
        if (table.keys().length == 0) {
            throw new IllegalStateException("Error Table " + table.name() + " without primary key defined");
        }
        String columnPrimaryKey = alias + "." + table.keys()[0];

        // Foreing Key
        Table tableJoin = clazzJoin.getDeclaredAnnotation(Table.class);
        if (tableJoin == null) {
            throw new IllegalStateException("Error class " + clazzJoin.getName() + " without @Table annotation");
        }
        aliasJoin = generateJoinAlias(aliasJoin, tableJoin.name());
        if (propertiesTranslationMap == null) {
            propertiesTranslationMap = new HashMap<>();
        }
        propertiesTranslationMap.putAll(prepareMapWithAlias(aliasJoin, JdbcCache.translationMap(clazzJoin)));

        String columnForeignKey = getRealName(aliasJoin + "." + foreignKey);

        sqlJoins.append(" ").append(joinOperator.toString().replace("_", " ")).append(" ").append(tableJoin.name());
        sqlJoins.append(" ").append(aliasJoin);
        sqlJoins.append(" ON ").append(columnPrimaryKey).append("=").append(columnForeignKey);
    }

    private Map<String, String> prepareMapWithAlias(String prefix, Map<String, String> properties) {
        return properties.entrySet().stream().collect(Collectors.toMap(entry -> prefix + "." + entry.getKey(), // Nuevo key con prefijo
                Map.Entry::getValue));
    }

    public final void addCustomParam(String name, Object value) {
        sqlParams.put(generateParameterName(name), toParameterValue(value, true));
    }

    /**
     * Adds a raw SQL condition. <b>Never</b> build {@code customCondition} with user input: use named parameters and {@link #addCustomParam(String, Object)} instead.
     *
     * @param customCondition
     *            raw SQL condition
     * @param apply
     *            whether the condition must be added
     */
    public void addCustomCondition(String customCondition, boolean apply) {
        if (apply) {
            addCustomCondition(customCondition);
        }
    }

    /**
     * Adds a raw SQL condition. <b>Never</b> build {@code customCondition} with user input: use named parameters and {@link #addCustomParam(String, Object)} instead.
     *
     * @param customCondition
     *            raw SQL condition
     */
    public void addCustomCondition(String customCondition) {
        sqlWhere.append(hasToIncludeOperator() ? " AND " : "");
        sqlWhere.append(customCondition);
        index++;
    }

    public void addConditionIs(String column, IS is) {
        addConditionIs(Operator.AND, column, is);
    }

    protected void addConditionIs(Operator operator, String column, IS is) {
        String columnName = getRealName(column);
        sqlWhere.append(hasToIncludeOperator() ? operator.toSentence() : "");
        sqlWhere.append(columnName);
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
        String columnName = getRealName(column);
        if (condition == null) {
            throw new IllegalArgumentException("QueryDynamic: condition is required");
        }

        if (apply) {
            String parameterName = generateParameterName(columnName + "_" + index);
            sqlWhere.append(hasToIncludeOperator() ? operator.toSentence() : "");

            if (value instanceof Date && condition == Condition.EQUAL) {
                String parameterNameStart = generateParameterName(parameterName + "_start");
                String parameterNameEnd = generateParameterName(parameterName + "_end");
                String dateStr = toLocalDate((Date) value).format(DATE_FORMAT);

                sqlWhere.append(columnName);
                sqlWhere.append(" BETWEEN ").append(dialect.toTimestamp(parameterNameStart, CustomEngine.DATE_PATTERN));
                sqlWhere.append(" AND ").append(dialect.toTimestamp(parameterNameEnd, CustomEngine.DATETIME_PATTERN));
                sqlParams.put(parameterNameStart, dateStr);
                sqlParams.put(parameterNameEnd, dateStr + " 23:59:59");
            } else if (value instanceof Date || value instanceof LocalDate) {
                sqlWhere.append(dialect.castToDate(columnName));
                sqlWhere.append(condition);
                sqlWhere.append(":").append(parameterName);
                sqlParams.put(parameterName, value);
            } else if (value instanceof LocalDateTime) {
                String datetimeStr = ((LocalDateTime) value).format(DATETIME_FORMAT);

                sqlWhere.append(columnName);
                sqlWhere.append(condition);
                sqlWhere.append(dialect.toTimestamp(parameterName, CustomEngine.DATETIME_PATTERN));
                sqlParams.put(parameterName, datetimeStr);
            } else {
                sqlWhere.append((value instanceof String && enableUppercaseAutomatically) ? "UPPER(" + columnName + ")" : columnName);
                sqlWhere.append(condition);
                sqlWhere.append(":").append(parameterName);
                sqlParams.put(parameterName, toParameterValue(value, enableUppercaseAutomatically));
            }
            index++;
        }
    }

    private static LocalDate toLocalDate(Date date) {
        if (date instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private static Object toParameterValue(Object value, boolean uppercase) {
        if (value instanceof String text) {
            return uppercase ? text.toUpperCase(Locale.ROOT) : text;
        } else if (value instanceof EnumIdentifiable<?> identifiable) {
            return identifiable.getId();
        } else if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        } else if (value instanceof Instant instant) {
            return Timestamp.from(instant);
        }
        return value;
    }

    public void addConditionLike(String column, String value) {
        addConditionLike(column, value, true, true);
    }

    public void addConditionLike(String column, String value, boolean percentAtStart, boolean percentAtEnd) {
        addConditionLike(Operator.AND, column, value, percentAtStart, percentAtEnd);
    }

    protected void addConditionLike(Operator operator, String column, String value, boolean percentAtStart, boolean percentAtEnd) {
        if (value != null && value.trim().compareTo("") != 0) {

            String columnName = getRealName(column);
            String parameterName = generateParameterName(columnName + "_" + index);
            sqlWhere.append(hasToIncludeOperator() ? operator.toSentence() : "");

            sqlWhere.append(enableUppercaseAutomatically ? "UPPER(" + columnName + ")" : columnName);
            sqlWhere.append(" LIKE ").append(likeExpression(parameterName, percentAtStart, percentAtEnd));

            sqlParams.put(parameterName, value.trim().toUpperCase(Locale.ROOT));
            index++;
        }
    }

    private String likeExpression(String parameterName, boolean percentAtStart, boolean percentAtEnd) {
        List<String> parts = new ArrayList<>(3);
        if (percentAtStart) {
            parts.add("'%'");
        }
        parts.add(":" + parameterName);
        if (percentAtEnd) {
            parts.add("'%'");
        }
        return parts.size() == 1 ? parts.get(0) : dialect.concat(parts.toArray(new String[0]));
    }

    public void addConditionLikesSmart(String column, String value) {
        addConditionLikesSmart(new String[]{column}, value, " ");
    }

    public void addConditionLikesSmart(String[] columns, String value) {
        addConditionLikesSmart(columns, value, " ");
    }

    public void addConditionLikesSmart(String[] columns, String value, String splitRegex) {
        if (value == null || columns == null || columns.length == 0) {
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
        String[] columnNames = new String[columns.length];
        for (int j = 0; j < columns.length; j++) {
            columnNames[j] = getRealName(columns[j]);
        }

        boolean hasClauseWhere = index > 0;
        int countItems = 0;
        StringBuilder internalSQL = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            internalSQL.append(i == 0 ? "(" : " AND (");
            for (int j = 0; j < columnNames.length; j++) {
                countItems++;

                String columnName = columnNames[j];
                String parameterName = generateParameterName(columnName + "_" + index);

                internalSQL.append((j == 0) ? "" : " OR ");
                internalSQL.append(enableUppercaseAutomatically ? "UPPER(" + columnName + ")" : columnName);
                internalSQL.append(" LIKE ").append(likeExpression(parameterName, true, true));
                sqlParams.put(parameterName, values[i].trim().toUpperCase(Locale.ROOT));
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
                throw new IllegalArgumentException("QueryDynamic: The number of columns should be equal to the number of values?");
            }
            if (matchModes == null || matchModes.length != columns.length) {
                throw new IllegalArgumentException("QueryDynamic: The number of columns should be equal to the number of match modes");
            }

            boolean apply = checkToApply(values);
            if (apply) {
                int startLength = sqlWhere.length();
                int startIndex = index;

                // BUSCAMOS LAS POSICIONES DE LOS PARENTESIS IZQ
                int indexLeftParenthesis = calculeLeftParenthesisIndex(values);

                // BUSCAMOS LAS POSICIONES DE LOS PARENTESIS DER
                int indexRightParenthesis = calculeRightParenthesisIndex(values);

                sqlWhere.append(hasToIncludeOperator() ? Operator.AND.toSentence() : "");
                for (int i = 0; i < columns.length; ++i) {
                    String leftParenthesis = (i == indexLeftParenthesis) ? "(" : "";
                    String rightParenthesis = (i == indexRightParenthesis) ? ")" : "";

                    String columnName = getRealName(columns[i]);
                    sqlWhere.append(leftParenthesis);

                    switch (matchModes[i]) {
                        case TEXT_CONTAINS :
                            addConditionLike(Operator.OR, columnName, String.valueOf(values[i]), true, true);
                            break;
                        case TEXT_ENDS_WITH :
                            addConditionLike(Operator.OR, columnName, String.valueOf(values[i]), true, false);
                            break;
                        case TEXT_STARTS_WITH :
                            addConditionLike(Operator.OR, columnName, String.valueOf(values[i]), false, true);
                            break;
                        case IN :
                            if (!(values[i] instanceof Object[])) {
                                addConditionIn(Operator.OR, columnName, new Object[]{values[i]});
                            } else {
                                addConditionIn(Operator.OR, columnName, (Object[]) values[i]);
                            }
                            break;
                        case BETWEEN :
                            if (!(values[i] instanceof Object[])) {
                                throw new IllegalArgumentException("QueryDynamic: MatchMode.BETWEEN must be Array with 2 values");
                            }
                            Object[] bValues = (Object[]) values[i];
                            addConditionBetween(Operator.OR, columnName, bValues[0], bValues[1]);
                            break;
                        case EQUAL :
                        case NOT_EQUAL :
                        case GREATER_OR_EQUAL :
                        case GREATER_THAN :
                        case LESS_THAN :
                        case LESS_OR_EQUAL :
                            addCondition(Operator.OR, columnName, values[i], convertMatchMode2Condition(matchModes[i]), false);
                            break;
                    }
                    sqlWhere.append(rightParenthesis);
                }

                if (index == startIndex) {
                    // nothing was applied (e.g. all values were zero): remove the empty parentheses
                    sqlWhere.setLength(startLength);
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
            String columnName = getRealName(column);
            sqlWhere.append(hasToIncludeOperator() ? operator.toSentence() : "");
            sqlWhere.append((values[0] instanceof String && enableUppercaseAutomatically) ? "UPPER(" + columnName + ")" : columnName);
            sqlWhere.append(" IN (");
            for (int i = 0; i < values.length; i++) {

                String parameterName = generateParameterName(columnName + "_" + index);
                sqlWhere.append(i == 0 ? ":" : ",:").append(parameterName);
                if (values[i] instanceof String) {
                    sqlParams.put(parameterName, ((String) values[i]).toUpperCase(Locale.ROOT));
                } else {
                    sqlParams.put(parameterName, toParameterValue(values[i], false));
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
            String columnName = getRealName(column);
            String parameterNameStart = generateParameterName(columnName + "_" + index + "_start");
            String parameterNameEnd = generateParameterName(columnName + "_" + index + "_end");

            sqlWhere.append(hasToIncludeOperator() ? operator.toSentence() : "");
            sqlWhere.append(columnName);
            sqlWhere.append(" BETWEEN ");
            sqlWhere.append(":").append(parameterNameStart);
            sqlWhere.append(" AND ");
            sqlWhere.append(":").append(parameterNameEnd);

            sqlParams.put(parameterNameStart, toParameterValue(value1, false));
            sqlParams.put(parameterNameEnd, toParameterValue(value2, false));
            index++;
        }
    }

    /**
     * Adds a condition over a JSON column. The {@code expression} is developer provided SQL appended after the column, where the {@code ${value}} placeholder is replaced by a bound parameter (values are never concatenated into the SQL).
     *
     * <ul>
     * <li>{@code ->> 'id' = ${value}}: the placeholder is replaced by a parameter bound to the value.</li>
     * <li>{@code ->> 'id' = '${value}'} or {@code @> '{"id": ${value}}'}: the whole literal containing the placeholder is replaced by a parameter bound to the literal text with the value substituted. Add an explicit cast when the database needs a type (e.g. {@code @> '{"id": ${value}}'::jsonb}).</li>
     * </ul>
     *
     * @param column
     *            column or mapped property
     * @param expression
     *            SQL expression (never user input)
     * @param value
     *            value to bind
     */
    public void addConditionJson(String column, String expression, Object value) {
        boolean apply = checkToApply(value);
        if (apply) {
            addConditionJson(column, expression, new Object[]{value});
        }
    }

    /**
     * Same as {@link #addConditionJson(String, String, Object)} with several values joined with OR.
     *
     * @param column
     *            column or mapped property
     * @param expression
     *            SQL expression (never user input)
     * @param values
     *            values to bind
     */
    public void addConditionJson(String column, String expression, Object[] values) {
        boolean apply = checkToApply(values);
        if (apply) {
            String columnName = getRealName(column);
            // BUSCAMOS LAS POSICIONES DE LOS PARENTESIS IZQ
            int indexIzq = calculeLeftParenthesisIndex(values);

            // BUSCAMOS LAS POSICIONES DE LOS PARENTESIS DER
            int indexDer = calculeRightParenthesisIndex(values);

            for (int i = 0; i < values.length; i++) {
                String leftParenthesis = (i == indexIzq) ? "(" : "";
                String rightParenthesis = (i == indexDer) ? ")" : "";
                if (values[i] != null) {

                    String parameterName = generateParameterName(columnName + "_" + index);
                    sqlWhere.append(hasToIncludeOperator() ? ((i == 0 || i == indexIzq) ? " AND " : " OR ") : "");
                    sqlWhere.append(leftParenthesis);
                    sqlWhere.append(columnName).append(" ");
                    sqlWhere.append(bindJsonExpression(expression, parameterName, values[i]));
                    sqlWhere.append(rightParenthesis);
                    index++;
                }
            }
        }
    }

    private String bindJsonExpression(String expression, String parameterName, Object value) {
        if (expression == null) {
            throw new IllegalArgumentException("QueryDynamic: JSON expression is required");
        }
        Object rawValue = toParameterValue(value, false);
        String textValue = String.valueOf(rawValue);
        StringBuilder result = new StringBuilder();
        int occurrence = 0;
        int i = 0;
        while (i < expression.length()) {
            char c = expression.charAt(i);
            if (c == '\'') {
                int end = i + 1;
                while (end < expression.length()) {
                    if (expression.charAt(end) == '\'') {
                        if (end + 1 < expression.length() && expression.charAt(end + 1) == '\'') {
                            end += 2;
                            continue;
                        }
                        break;
                    }
                    end++;
                }
                String literal = expression.substring(i, Math.min(end + 1, expression.length()));
                if (literal.contains(VALUE_PLACEHOLDER)) {
                    String name = occurrence == 0 ? parameterName : parameterName + "_" + occurrence;
                    occurrence++;
                    String content = literal.length() >= 2 ? literal.substring(1, literal.length() - 1) : "";
                    sqlParams.put(name, content.replace("''", "'").replace(VALUE_PLACEHOLDER, textValue));
                    result.append(":").append(name);
                } else {
                    result.append(literal);
                }
                i = end + 1;
            } else if (expression.startsWith(VALUE_PLACEHOLDER, i)) {
                String name = occurrence == 0 ? parameterName : parameterName + "_" + occurrence;
                occurrence++;
                sqlParams.put(name, rawValue);
                result.append(":").append(name);
                i += VALUE_PLACEHOLDER.length();
            } else {
                result.append(c);
                i++;
            }
        }
        return result.toString();
    }

    /**
     * Parses a textual sort direction.
     *
     * @param direction
     *            {@code ASC}, {@code DESC}, {@code ASCENDING} or {@code DESCENDING} (case-insensitive); {@code null} or empty means ascending
     * @return the sort order
     * @throws IllegalArgumentException
     *             for any other value
     */
    public static SortOrder parseSortOrder(String direction) {
        if (direction == null || direction.trim().isEmpty()) {
            return SortOrder.ASCENDING;
        }
        switch (direction.trim().toUpperCase(Locale.ROOT)) {
            case "ASC" :
            case "ASCENDING" :
                return SortOrder.ASCENDING;
            case "DESC" :
            case "DESCENDING" :
                return SortOrder.DESCENDING;
            default :
                throw new IllegalArgumentException("Invalid sort direction: '" + direction + "'. Allowed values are ASC or DESC");
        }
    }

    public final void setOrderBy(String column) {
        setOrderBy(column, SortOrder.ASCENDING);
    }

    public final void setOrderBy(String column, SortOrder sortOrder) {
        boolean desc = (sortOrder == SortOrder.DESCENDING);

        String columnName = getRealName(column);
        sqlOrderBy = new String[]{columnName};
        sqlOrderByDesc = new boolean[]{desc};
    }

    public final void setOrderBy(String columns[], SortOrder[] sortOrders) {

        if (columns.length != sortOrders.length) {
            throw new IllegalArgumentException("QueryDynamic: The number of columns should be equal to the number of sortOrders");
        }

        String[] orderBy = new String[columns.length];
        boolean[] orderByDesc = new boolean[sortOrders.length];

        for (int i = 0; i < columns.length; i++) {
            orderBy[i] = getRealName(columns[i]);
            orderByDesc[i] = sortOrders[i] == SortOrder.DESCENDING;
        }
        sqlOrderBy = orderBy;
        sqlOrderByDesc = orderByDesc;
    }

    public final void setOrderBy(List<String> columns, SortOrder sort) {
        String[] orderBy = new String[columns.size()];
        boolean[] orderByDesc = new boolean[columns.size()];

        for (int i = 0; i < columns.size(); i++) {
            orderBy[i] = getRealName(columns.get(i));
            orderByDesc[i] = sort == SortOrder.DESCENDING;
        }
        sqlOrderBy = orderBy;
        sqlOrderByDesc = orderByDesc;
    }

    public final void setOrderBy(String column, String defaultColumn, SortOrder sortOrder) {
        setOrderBy(Utils.isNullOrEmpty(column) ? defaultColumn : column, sortOrder);
    }

    public void setOrderBy(List<OrderBy> ordersBy) {
        if (!Utils.isNullOrEmpty(ordersBy)) {

            String[] columns = new String[ordersBy.size()];
            SortOrder[] sorts = new SortOrder[ordersBy.size()];

            for (int i = 0; i < ordersBy.size(); i++) {
                columns[i] = ordersBy.get(i).getColumn();
                sorts[i] = ordersBy.get(i).getSort();
            }

            this.setOrderBy(columns, sorts);
        }
    }

    private boolean selectHasTopLevelWhere() {
        if (selectHasWhere == null) {
            int from = SqlUtils.indexOfTopLevel(sqlSelect, "FROM");
            selectHasWhere = from >= 0 && SqlUtils.indexOfTopLevel(sqlSelect, "WHERE", from) >= 0;
        }
        return selectHasWhere;
    }

    private String whereClause() {
        if (!hasConditions()) {
            return "";
        }
        // the base SQL already has a WHERE clause: the dynamic conditions are appended with AND
        return selectHasTopLevelWhere() ? " AND (" + sqlWhere + ")" : " WHERE " + sqlWhere;
    }

    public String getSQL() {
        StringBuilder sql = new StringBuilder();
        sql.append(sqlSelect);
        if (!sqlJoins.isEmpty()) {
            sql.append(sqlJoins);
        }
        sql.append(whereClause());

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

    /**
     * Generates the SQL used to count the records of this query. A simple {@code SELECT ... FROM} is rewritten as {@code SELECT count(*) FROM ...}; queries using DISTINCT, GROUP BY, HAVING, set operators or an ORDER BY in the base SQL are wrapped: {@code SELECT count(*) FROM (<query>) count_query}.
     *
     * @return SQL count
     */
    public String getSQLCount() {
        StringBuilder sql = new StringBuilder();
        String where = whereClause();
        int indexFrom = SqlUtils.indexOfTopLevel(sqlSelect, "FROM");
        if (indexFrom > 0 && !requiresCountWrapper(indexFrom)) {
            sql.append("SELECT count(*) ").append(sqlSelect.substring(indexFrom));
            if (!sqlJoins.isEmpty()) {
                sql.append(sqlJoins);
            }
            sql.append(where);
        } else {
            sql.append("SELECT count(*) FROM (").append(sqlSelect);
            if (!sqlJoins.isEmpty()) {
                sql.append(sqlJoins);
            }
            sql.append(where);
            sql.append(") count_query");
        }
        return sql.toString();
    }

    private boolean requiresCountWrapper(int indexFrom) {
        String selectPart = sqlSelect.substring(0, indexFrom);
        if (SqlUtils.indexOfTopLevel(selectPart, "DISTINCT") >= 0 || SqlUtils.indexOfTopLevel(selectPart, "TOP") >= 0) {
            return true;
        }
        for (String keyword : new String[]{"GROUP BY", "HAVING", "UNION", "INTERSECT", "EXCEPT", "MINUS", "ORDER BY", "LIMIT", "OFFSET", "FETCH"}) {
            if (SqlUtils.indexOfTopLevel(sqlSelect, keyword, indexFrom) >= 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generates a DELETE statement using the FROM clause of the base SQL and the applied conditions.
     *
     * @return SQL delete
     * @throws DataException
     *             when no condition was applied (a DELETE without WHERE would remove the whole table) or the query contains joins
     */
    public String getSQLDelete() {
        int indexFrom = SqlUtils.indexOfTopLevel(sqlSelect, "FROM");
        if (indexFrom < 0) {
            throw new DataException("Unable to generate DELETE: the base SQL has no FROM clause");
        }
        String fromPart = sqlSelect.substring(indexFrom);
        if (!sqlJoins.isEmpty() || SqlUtils.indexOfTopLevel(fromPart, "JOIN") >= 0) {
            throw new DataException("Unable to generate DELETE: joins are not supported");
        }
        if (!hasConditions() && !selectHasTopLevelWhere()) {
            throw new DataException("Refusing to generate a DELETE without WHERE conditions: no condition was applied and the whole table would be deleted");
        }
        return "DELETE " + fromPart + whereClause();
    }

    protected boolean checkToApply(Object value) {
        if (value == null) {
            return false;
        } else if (value instanceof String s) {
            return (s.trim().compareTo("") != 0);
        } else if (value instanceof BigDecimal bd) {
            return bd.signum() != 0;
        } else if (value instanceof BigInteger bi) {
            return bi.signum() != 0;
        } else if (value instanceof Double d) {
            return d != 0 && !d.isNaN();
        } else if (value instanceof Float f) {
            return f != 0 && !f.isNaN();
        } else if (value instanceof Number n) {
            // Integer, Long, Short, Byte, AtomicInteger, AtomicLong...
            return n.longValue() != 0;
        } else if (value instanceof Date || value instanceof Time || value instanceof Temporal || value instanceof UUID || value instanceof Enum || value instanceof Boolean
                || value instanceof Character) {
            return true;
        } else if (value instanceof Object[]) {
            boolean allNulls = true;
            for (Object v : (Object[]) value) {
                if (v instanceof String) {
                    if (!Utils.isNullOrEmpty((String) v)) {
                        allNulls = false;
                        break;
                    }
                } else if (v != null) {
                    allNulls = false;
                    break;
                }
            }
            return !allNulls;
        } else if (value instanceof Collection<?> collection) {
            return !collection.isEmpty();
        } else if (value instanceof byte[] bytes) {
            return (bytes.length > 0);
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

    public void addGroupCondition(GroupCondition groupCondition) {
        List<GroupCondition> groupConditions = new ArrayList<>();
        groupConditions.add(groupCondition);
        addGroupConditions(groupConditions);
    }

    public void addGroupConditions(List<GroupCondition> groupConditions) {
        if (!Utils.isNullOrEmpty(groupConditions)) {
            int startLength = this.sqlWhere.length();
            this.sqlWhere.append((this.index == 0) ? "(" : " AND (");
            boolean anyApplied = false;
            for (GroupCondition groupCondition : groupConditions) {
                if (groupCondition == null) {
                    continue;
                }
                int groupLength = this.sqlWhere.length();
                int groupIndex = this.index;
                if (anyApplied) {
                    this.sqlWhere.append(" OR ");
                }
                generateConditions(groupCondition.getConditions());
                if (this.index == groupIndex) {
                    // no condition of the group could be applied: remove what was written
                    this.sqlWhere.setLength(groupLength);
                } else {
                    anyApplied = true;
                }
            }
            if (anyApplied) {
                this.sqlWhere.append(")");
            } else {
                this.sqlWhere.setLength(startLength);
            }
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
            case TEXT_CONTAINS :
                addConditionLike(Operator.AND, condition.getColumn(), String.valueOf(condition.getValue()), true, true);
                break;
            case TEXT_ENDS_WITH :
                addConditionLike(Operator.AND, condition.getColumn(), String.valueOf(condition.getValue()), true, false);
                break;
            case TEXT_STARTS_WITH :
                addConditionLike(Operator.AND, condition.getColumn(), String.valueOf(condition.getValue()), false, true);
                break;
            case IN :
                if (condition.getValue() instanceof Collection<?> collection) {
                    addConditionIn(Operator.AND, condition.getColumn(), collection.toArray());
                } else if (!(condition.getValue() instanceof Object[])) {
                    addConditionIn(Operator.AND, condition.getColumn(), new Object[]{condition.getValue()});
                } else {
                    addConditionIn(Operator.AND, condition.getColumn(), (Object[]) condition.getValue());
                }
                break;
            case BETWEEN :
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
            case EQUAL :
            case NOT_EQUAL :
            case GREATER_OR_EQUAL :
            case GREATER_THAN :
            case LESS_THAN :
            case LESS_OR_EQUAL :
                addCondition(Operator.AND, condition.getColumn(), condition.getValue(), convertMatchMode2Condition(condition.getMatchMode()), false);
                break;
        }
    }

    private void checkDataTypeAndMatchMode(GroupCondition.Condition condition) {
        if (condition.getDataType() == null || condition.getMatchMode() == null) {
            throw new IllegalArgumentException("QueryDynamic: dataType and matchMode are required for column " + condition.getColumn());
        }
        boolean contains = Arrays.stream(condition.getDataType().getMatchModes()).anyMatch(condition.getMatchMode()::equals);
        if (!contains) {
            throw new IllegalArgumentException("QueryDynamic: MatchMode." + condition.getMatchMode() + " is not supported with " + condition.getDataType());
        }

        if (condition.getDataType() == DataType.DATE && condition.getValue() != null
                && !(condition.getValue() instanceof Date || condition.getValue() instanceof LocalDate || condition.getValue() instanceof LocalDateTime || condition.getValue() instanceof List)) {
            throw new IllegalArgumentException("QueryDynamic: DataType.DATE cannot be applied to " + condition.getValue());
        }
    }

    /**
     * Resolves a column: a property of the mapped class (or of a joined class, prefixed by its alias) is translated to the qualified column name; anything else must be a plain SQL identifier.
     *
     * @param column
     *            property or column name
     * @return the SQL column reference
     * @throws IllegalArgumentException
     *             when the column is not a mapped property nor a valid identifier
     */
    private String getRealName(String column) {
        String mapped = (propertiesTranslationMap != null && column != null) ? propertiesTranslationMap.get(column) : null;
        String fullColumnName = (mapped != null) ? mapped : SqlUtils.requireIdentifier(column);

        int dot = fullColumnName.lastIndexOf('.');
        if (dot > 0) {
            String tableName = fullColumnName.substring(0, dot);
            String alias = aliasMap.get(tableName);
            if (alias != null) {
                // The table name has an alias, so we replace it in the column name
                return alias + "." + fullColumnName.substring(dot + 1);
            }
        }
        // There is not a alias generated automatically for the table, so we return the column name
        return fullColumnName;
    }

    private String generateJoinAlias(String alias, String name) {
        if (Utils.isNullOrEmpty(alias)) {
            indexJoins++;
            alias = "jt" + indexJoins;
        }
        aliasMap.put(name, alias);
        return alias;
    }

    public enum Condition {
        EQUAL("="),
        LESS_THAN("<"),
        LESS_OR_EQUAL("<="),
        GREATER_THAN(">"),
        GREATER_OR_EQUAL(">="),
        NOT_EQUAL("<>");

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
        IS_NULL(" IS NULL"),
        IS_NOT_NULL(" IS NOT NULL");

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
        OR,
        AND;

        public String toSentence() {
            return " ".concat(name()).concat(" ");
        }
    }
}
