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
package cl.kanopus.jdbc.impl;

import cl.kanopus.common.data.Paginator;
import cl.kanopus.common.data.enums.SortOrder;
import cl.kanopus.common.enums.EnumIdentifiable;
import cl.kanopus.common.util.GsonUtils;
import cl.kanopus.common.util.Utils;
import cl.kanopus.common.util.crypto.CryptographyUtils;
import cl.kanopus.jdbc.DAOInterface;
import cl.kanopus.jdbc.entity.Mapping;
import cl.kanopus.jdbc.entity.annotation.Column;
import cl.kanopus.jdbc.entity.annotation.ColumnGroup;
import cl.kanopus.jdbc.entity.annotation.JoinTable;
import cl.kanopus.jdbc.entity.annotation.Table;
import cl.kanopus.jdbc.entity.mapper.AbstractRowMapper;
import cl.kanopus.jdbc.exception.DataException;
import cl.kanopus.jdbc.impl.engine.*;
import cl.kanopus.jdbc.util.JdbcCache;
import cl.kanopus.jdbc.util.QueryIterator;
import cl.kanopus.jdbc.util.SQLQueryDynamic;
import cl.kanopus.jdbc.util.parser.*;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;

/**
 * This abstract class defines methods for data access that are common, generally, all kinds of data access DAO must implement this class.Thus it is given safely access the Connection database. The JdbcTemplate property is kept private and gives access to the database through the methods implemented
 * in this AbstractDAO.
 *
 * @param <T>
 * @param <ID>
 * @author Pablo Diaz Saavedra
 */
@SuppressWarnings("all")
public abstract class AbstractDAO<T extends Mapping, ID> implements DAOInterface<T, ID> {

    protected static final Logger log = LoggerFactory.getLogger(AbstractDAO.class);
    private static final String UNASSIGNED = "[unassigned]";

    enum Operation {
        UPDATE,
        PERSIST
    }

    private final Class<T> genericTypeClass;

    protected AbstractDAO() {
        Type type = getClass().getGenericSuperclass();
        ParameterizedType paramType = (ParameterizedType) type;
        genericTypeClass = (Class<T>) paramType.getActualTypeArguments()[0];
    }

    protected abstract NamedParameterJdbcTemplate getJdbcTemplate();

    protected abstract Engine getEngine();

    private String createSqlPagination2Engine(SQLQueryDynamic sqlQuery) {
        String sql = getCustom().prepareSQL2Engine(sqlQuery.getSQL());
        if (sqlQuery.isLimited()) {
            return getCustom().createSqlPagination(sql, sqlQuery.getLimit(), sqlQuery.getOffset()).toString();
        } else {
            return sql;
        }
    }

    private String createSqlPagination(String sql, int limit, int offset) {
        return getCustom().createSqlPagination(prepare(sql), limit, offset).toString();
    }

    /**
     * Translates the SQL to the configured engine (see {@link CustomEngine#prepareSQL2Engine(String)}).
     */
    private String prepare(String sql) {
        return getCustom().prepareSQL2Engine(sql);
    }

    private static void logSql(String sql, Map<String, ?> params) {
        if (log.isDebugEnabled()) {
            // parameter values may contain sensitive data: only their names are logged
            log.debug("sql: {} params: {}", sql, params == null ? "[]" : params.keySet());
        }
    }

    @Override
    public int deleteById(ID id) throws DataException {
        return deleteById(getGenericTypeClass(), id);
    }

    protected int deleteById(Class clazz, ID key) throws DataException {
        return deleteById(clazz, isArray(key) ? ((Object[]) key) : new Object[]{key});
    }

    protected int deleteById(Class clazz, Object... keys) throws DataException {
        Table table = getTableName(clazz);
        if (isNullOrUnassigned(table.keys())) {
            throw new DataException("It is necessary to specify the primary keys for the entity: " + clazz.getName());
        }
        if (table.keys().length != keys.length) {
            throw new DataException("It is necessary to specify the same amount keys to remove the entity: " + clazz.getName());
        }

        StringBuilder sql = new StringBuilder();
        sql.append("DELETE FROM ").append(table.name()).append(" WHERE ");
        HashMap<String, Object> params = new HashMap<>();
        for (int i = 0; i < keys.length; i++) {
            params.put(table.keys()[i], keys[i]);
            sql.append(i == 0 ? "" : " AND ");
            sql.append(table.keys()[i]).append("=:").append(table.keys()[i]);
        }

        return update(sql.toString(), params);
    }

    /**
     * Deletes the records matching the conditions of the query.
     *
     * @param query
     *            query with at least one applied condition
     * @return affected rows
     * @throws DataException
     *             when the query has no applied condition (it would delete the whole table)
     */
    protected int delete(SQLQueryDynamic query) throws DataException {
        return update(prepare(query.getSQLDelete()), query.getParams());
    }

    private JdbcTemplate getClassicJdbcTemplate() {
        JdbcOperations operations = getJdbcTemplate().getJdbcOperations();
        if (operations instanceof JdbcTemplate jdbcTemplate) {
            return jdbcTemplate;
        }
        throw new DataException("Stored procedures require a NamedParameterJdbcTemplate backed by a JdbcTemplate, found: " + (operations == null ? "null" : operations.getClass().getName()));
    }

    protected <T> List<T> executeProcedure(String name, SqlParameter[] parameters, Map<String, Object> params, Class<T> returnType) {
        SimpleJdbcCall sjc = new SimpleJdbcCall(getClassicJdbcTemplate()).withoutProcedureColumnMetaDataAccess().withProcedureName(name).declareParameters(parameters);

        SqlParameterSource in = new MapSqlParameterSource().addValues(params);
        Map<String, Object> out = sjc.execute(in);
        if (out == null || out.isEmpty()) {
            return new ArrayList<>();
        }
        for (Object value : out.values()) {
            if (value instanceof List<?> tempList && !tempList.isEmpty() && returnType.isInstance(tempList.get(0))) {
                return (List<T>) tempList;
            }
        }
        return new ArrayList<>();
    }

    protected void executeProcedure(String name, SqlParameter[] parameters, Map<String, Object> params) {
        SimpleJdbcCall sjc = new SimpleJdbcCall(getClassicJdbcTemplate()).withoutProcedureColumnMetaDataAccess().withProcedureName(name).declareParameters(parameters);

        SqlParameterSource in = new MapSqlParameterSource().addValues(params);
        sjc.execute(in);
    }

    protected T executeProcedure(String name, SqlParameter[] parameters, MapSqlParameterSource params, Class<T> returnType) {
        SimpleJdbcCall sjc = new SimpleJdbcCall(getClassicJdbcTemplate());
        sjc.withProcedureName(name);
        sjc.withoutProcedureColumnMetaDataAccess();
        sjc.declareParameters(parameters);
        return sjc.executeFunction(returnType, params);
    }

    protected void executeProcedure(String name, SqlParameter[] parameters, MapSqlParameterSource params) {
        SimpleJdbcCall sjc = new SimpleJdbcCall(getClassicJdbcTemplate());
        sjc.withProcedureName(name);
        sjc.withoutProcedureColumnMetaDataAccess();
        sjc.declareParameters(parameters);
        sjc.execute(params);
    }

    protected Map<String, Object> executeProcedureOut(String name, SqlParameter[] parameters, MapSqlParameterSource params) {
        SimpleJdbcCall sjc = new SimpleJdbcCall(getClassicJdbcTemplate());
        sjc.withProcedureName(name);
        sjc.withoutProcedureColumnMetaDataAccess();
        sjc.declareParameters(parameters);
        Map<String, Object> out = sjc.execute(params);
        return out;
    }

    @Override
    public List<T> findAll() throws DataException {
        return findAll(getGenericTypeClass());
    }

    public <I extends Mapping> List<I> findAll(Class<I> aClass) throws DataException {
        return findAll(aClass, false);
    }

    public <I extends Mapping> List<I> findAll(Class<I> aClass, boolean loadAll) throws DataException {
        SQLQueryDynamic sqlQuery = new SQLQueryDynamic(aClass, loadAll);
        Table table = getTableName(aClass, false);
        if (table != null && !UNASSIGNED.equals(table.defaultOrderBy()) && !Utils.isNullOrEmpty(table.defaultOrderBy())) {
            applyDefaultOrderBy(sqlQuery, table.defaultOrderBy());
        }
        return AbstractDAO.this.find(sqlQuery, aClass);
    }

    /**
     * Applies a {@code defaultOrderBy} annotation value such as {@code "name"}, {@code "name DESC"} or {@code "a ASC, b DESC"}.
     */
    private static void applyDefaultOrderBy(SQLQueryDynamic sqlQuery, String defaultOrderBy) {
        String[] parts = defaultOrderBy.split(",");
        String[] columns = new String[parts.length];
        SortOrder[] sorts = new SortOrder[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String[] tokens = parts[i].trim().split("\\s+");
            if (tokens.length > 2) {
                throw new IllegalArgumentException("Invalid defaultOrderBy: '" + defaultOrderBy + "'");
            }
            columns[i] = tokens[0];
            sorts[i] = SQLQueryDynamic.parseSortOrder(tokens.length == 2 ? tokens[1] : null);
        }
        sqlQuery.setOrderBy(columns, sorts);
    }

    @Override
    public List<T> findTop(int limit, SortOrder sortOrder) throws DataException {
        Table table = getTableName(getGenericTypeClass());
        JdbcCache.SqlBase base = JdbcCache.sqlBase(getGenericTypeClass());
        StringBuilder sql = new StringBuilder();
        sql.append(base.getSql());
        if (!isNullOrUnassigned(table.keys()) && table.keys().length > 0) {
            String alias = base.getAliasMap().get(table.name());
            String direction = sortOrder == SortOrder.DESCENDING ? " DESC" : " ASC";
            sql.append(" ORDER BY ");
            for (int i = 0; i < table.keys().length; i++) {
                sql.append(i == 0 ? "" : ", ");
                sql.append(alias != null ? alias + "." : "").append(table.keys()[i]).append(direction);
            }
        }

        String customSql = createSqlPagination(sql.toString(), limit, 0);
        return AbstractDAO.this.find(customSql);
    }

    public Iterator findQueryIterator(SQLQueryDynamic sqlQuery) {
        int limit = Utils.defaultValue(sqlQuery.getLimit(), 2500);
        if (!sqlQuery.isSorted()) {
            throw new DataException("It is necessary to specify a sort with identifier to be able to iterate over the records.");
        }
        return new QueryIterator<Map<String, Object>>(limit) {

            @Override
            public List getData(int limit, int offset) {
                sqlQuery.setLimit(limit);
                sqlQuery.setOffset(offset);

                List records = null;
                if (sqlQuery.getClazz() != null) {
                    records = getJdbcTemplate().query(createSqlPagination2Engine(sqlQuery), sqlQuery.getParams(), rowMapper(sqlQuery.getClazz(), sqlQuery.isLoadAll()));
                } else {
                    records = getJdbcTemplate().queryForList(createSqlPagination2Engine(sqlQuery), sqlQuery.getParams());
                }

                sqlQuery.setTotalResultCount(sqlQuery.getTotalResultCount() + records.size());
                return records;
            }
        };
    }

    protected List<T> find(String sql) throws DataException {
        List list;
        try {
            logSql(sql, null);
            list = getJdbcTemplate().query(prepare(sql), rowMapper(getGenericTypeClass()));
        } catch (EmptyResultDataAccessException e) {
            list = new ArrayList();
        }
        return list;
    }

    protected List<T> find(String sql, HashMap<String, ?> params) throws DataException {
        List list;
        try {
            logSql(sql, params);
            list = getJdbcTemplate().query(prepare(sql), params, rowMapper(getGenericTypeClass()));
        } catch (EmptyResultDataAccessException e) {
            list = new ArrayList();
        }
        return list;
    }

    protected <I extends Mapping> List<I> find(String sql, HashMap<String, ?> params, Class<I> clazz) throws DataException {
        List list;
        try {
            logSql(sql, params);
            list = getJdbcTemplate().query(prepare(sql), params, rowMapper(clazz));
        } catch (EmptyResultDataAccessException e) {
            list = new ArrayList();
        }
        return list;
    }

    protected <I extends Mapping> List<I> find(String sql, HashMap<String, ?> params, Class<I> clazz, int limit, int offset) throws DataException {
        List list;
        try {
            logSql(sql, params);
            String sqlPagination = createSqlPagination(sql, limit, offset);
            list = getJdbcTemplate().query(sqlPagination, params, rowMapper(clazz));
        } catch (EmptyResultDataAccessException e) {
            list = new ArrayList();
        }
        return list;
    }

    protected List<T> find(SQLQueryDynamic sqlQuery) throws DataException {
        return (List<T>) find(sqlQuery, sqlQuery.getClazz());
    }

    protected <I extends Mapping> List<I> find(SQLQueryDynamic sqlQuery, Class<I> clazz) throws DataException {
        String sql = createSqlPagination2Engine(sqlQuery);
        logSql(sql, sqlQuery.getParams());
        List records = getJdbcTemplate().query(sql, sqlQuery.getParams(), rowMapper(clazz, sqlQuery.isLoadAll()));
        if (sqlQuery.isLimited()) {
            long count = getJdbcTemplate().queryForObject(getCustom().prepareSQL2Engine(sqlQuery.getSQLCount()), sqlQuery.getParams(), Long.class);
            sqlQuery.setTotalResultCount(count);
        } else {
            sqlQuery.setTotalResultCount(records.size());
        }
        return records;
    }

    protected Paginator<T> findPaginator(SQLQueryDynamic sqlQuery) throws DataException {
        return (Paginator<T>) findPaginator(sqlQuery, sqlQuery.getClazz());
    }

    protected <I extends Mapping> Paginator<I> findPaginator(SQLQueryDynamic sqlQuery, Class<I> clazz) throws DataException {
        List records = find(sqlQuery, clazz != null ? clazz : (Class<I>) sqlQuery.getClazz());
        Paginator paginator = new Paginator();
        paginator.setRecords(records);
        paginator.setTotalRecords(sqlQuery.getTotalResultCount());
        return paginator;
    }

    protected Paginator<Map<String, Object>> findMaps(SQLQueryDynamic sqlQuery) throws DataException {
        Paginator paginator = new Paginator();
        try {
            List records = getJdbcTemplate().queryForList(createSqlPagination2Engine(sqlQuery), sqlQuery.getParams());
            if (sqlQuery.isLimited()) {
                long count = getJdbcTemplate().queryForObject(getCustom().prepareSQL2Engine(sqlQuery.getSQLCount()), sqlQuery.getParams(), Long.class);
                sqlQuery.setTotalResultCount(count);
            } else {
                sqlQuery.setTotalResultCount(records.size());
            }

            paginator.setRecords(records);
            paginator.setTotalRecords(sqlQuery.getTotalResultCount());
        } catch (EmptyResultDataAccessException e) {
            paginator.setRecords(new ArrayList<>());
            paginator.setTotalRecords(0);
        }
        return paginator;
    }

    protected List<Map<String, Object>> findMaps(String sql, Map<String, ?> params) throws DataException {
        List list;
        try {
            list = getJdbcTemplate().queryForList(prepare(sql), params);
        } catch (EmptyResultDataAccessException e) {
            list = new ArrayList<>();
        }
        return list;
    }

    protected List<Map<String, Object>> findMaps(String sql, Map<String, ?> params, int limit, int offset) throws DataException {
        List list;
        try {
            String sqlPagination = createSqlPagination(sql, limit, offset);
            list = getJdbcTemplate().queryForList(sqlPagination, params);
        } catch (EmptyResultDataAccessException e) {
            list = new ArrayList<>();
        }
        return list;
    }

    protected List<String> findStrings(String sql, Map<String, ?> params) throws DataException {
        List list;
        try {
            list = getJdbcTemplate().queryForList(prepare(sql), params, String.class);
        } catch (EmptyResultDataAccessException e) {
            list = new ArrayList<>();
        }
        return list;
    }

    protected List<String> findStrings(String sql, Map<String, ?> params, int limit, int offset) throws DataException {
        List list;
        try {
            String sqlPagination = createSqlPagination(sql, limit, offset);
            list = getJdbcTemplate().queryForList(sqlPagination, params, String.class);
        } catch (EmptyResultDataAccessException e) {
            list = new ArrayList<>();
        }
        return list;
    }

    protected List<Long> findLongs(String sql, Map<String, ?> params) throws DataException {
        List list;
        try {
            list = getJdbcTemplate().queryForList(prepare(sql), params, Long.class);
        } catch (EmptyResultDataAccessException e) {
            list = new ArrayList<>();
        }
        return list;
    }

    @Override
    public long generateID() throws DataException {
        Table table = getTableName(getGenericTypeClass());
        String sequence = prepareSequence(table);
        if (sequence == null) {
            throw new DataException("It is necessary to specify the sequence related to the entity: " + table.name());
        }
        String customSql = getCustom().createSqlNextval(sequence);
        return queryForLong(customSql);
    }

    protected long generateAnyID(Class<? extends Mapping> clazz) throws DataException {
        Table table = getTableName(clazz);
        String sequence = prepareSequence(table);
        if (sequence == null) {
            throw new DataException("It is necessary to specify the sequence related to the entity: " + table.name());
        }
        String customSql = getCustom().createSqlNextval(sequence);
        return queryForLong(customSql);
    }

    private String prepareSequence(Table table) {
        if (!isNullOrUnassigned(table.sequence())) {
            return table.sequence();
        }
        return (!isNullOrUnassigned(table.keys()) && table.keys().length == 1) ? String.format("%s_%s_seq", table.name(), table.keys()[0]) : null;
    }

    private static boolean isNullOrUnassigned(Object property) {
        if (property instanceof String[] values) {
            return values.length == 0 || (values.length == 1 && UNASSIGNED.equals(values[0]));
        }
        return (property == null || UNASSIGNED.equals(property));
    }

    protected boolean exists(SQLQueryDynamic query) throws DataException {
        Long count = queryForLong(query.getSQLCount(), query.getParams());
        return (count != null && count > 0);
    }

    @Override
    public boolean existsById(ID id) throws DataException {
        Object[] keys = isArray(id) ? (Object[]) id : new Object[]{id};
        Class<T> clazz = getGenericTypeClass();
        Table table = getTableName(clazz);
        checkKeys(clazz, table, keys);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT count(*) FROM ").append(table.name()).append(" WHERE ");
        HashMap<String, Object> params = new HashMap<>();
        appendKeyConditions(sql, params, table, null, keys);
        Long count = queryForLong(sql.toString(), params);
        return count != null && count > 0;
    }

    private void checkKeys(Class<?> clazz, Table table, Object[] keys) {
        if (isNullOrUnassigned(table.keys())) {
            throw new DataException("It is necessary to specify the primary keys for the entity: " + clazz.getName());
        }
        if (table.keys().length != keys.length) {
            throw new DataException("It is necessary to specify the same keys to identify the entity: " + clazz.getName());
        }
    }

    private static void appendKeyConditions(StringBuilder sql, Map<String, Object> params, Table table, String alias, Object[] keys) {
        for (int i = 0; i < keys.length; i++) {
            String key = table.keys()[i];
            params.put(key, keys[i] instanceof EnumIdentifiable<?> e ? e.getId() : keys[i]);
            sql.append(i == 0 ? "" : " AND ");
            sql.append(alias != null ? alias + "." : "").append(key).append("=:").append(key);
        }
    }

    @Override
    public Optional<T> findById(ID id) throws DataException {
        return Optional.ofNullable((T) getById(getGenericTypeClass(), id));
    }

    @Override
    public T getById(ID id) throws DataException {
        return (T) getById(getGenericTypeClass(), id);
    }

    @Override
    public T getById(ID key, boolean loadAll) throws DataException {
        return (T) getById(getGenericTypeClass(), loadAll, isArray(key) ? ((Object[]) key) : new Object[]{key});
    }

    protected <T extends Mapping> T getById(Class<T> clazz, ID key) throws DataException {
        return getById(clazz, isArray(key) ? ((Object[]) key) : new Object[]{key});
    }

    protected <T extends Mapping> T getById(Class<T> clazz, Object... keys) throws DataException {
        return getById(clazz, false, keys);
    }

    protected <T extends Mapping> T getById(Class<T> clazz, boolean loadAll, Object... keys) throws DataException {
        Table table = getTableName(clazz);
        checkKeys(clazz, table, keys);

        JdbcCache.SqlBase base = JdbcCache.sqlBase(clazz, true);
        StringBuilder sql = new StringBuilder();
        sql.append(base.getSql()).append(" WHERE ");
        HashMap<String, Object> params = new HashMap<>();
        appendKeyConditions(sql, params, table, base.getAliasMap().get(table.name()), keys);

        return (T) queryForObject(sql.toString(), params, rowMapper(clazz, loadAll));
    }

    private Class<T> getGenericTypeClass() {
        return genericTypeClass;
    }

    @Override
    public T persist(T object) throws DataException {
        persistAny(object);
        return object;
    }

    protected void persistAny(Mapping object) throws DataException {

        Table table = getTableName(object.getClass());
        HashMap<String, Object> params = prepareParams(Operation.PERSIST, object);

        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(table.name());
        sql.append("(");
        boolean firstElement = true;
        for (String key : params.keySet()) {
            sql.append(!firstElement ? "," : "");
            sql.append(key);
            firstElement = false;
        }
        sql.append(") VALUES");
        sql.append("(");
        firstElement = true;
        for (String key : params.keySet()) {
            sql.append(!firstElement ? "," : "");
            sql.append(":").append(key);
            firstElement = false;
        }
        sql.append(")");

        update(sql.toString(), params);
    }

    protected String queryForString(String sql, HashMap<String, ?> params) throws DataException {
        String object;
        try {
            object = getJdbcTemplate().queryForObject(prepare(sql), params, String.class);
        } catch (EmptyResultDataAccessException e) {
            object = null;
        }
        return object;
    }

    protected byte[] queryForBytes(String sql, HashMap<String, ?> params) throws DataException {
        return getJdbcTemplate().queryForObject(prepare(sql), params, byte[].class);
    }

    protected Integer queryForInteger(String sql) throws DataException {
        return queryForInteger(sql, null);
    }

    protected Integer queryForInteger(String sql, HashMap<String, ?> params) throws DataException {
        Integer object;
        try {
            object = getJdbcTemplate().queryForObject(prepare(sql), params, Integer.class);
        } catch (EmptyResultDataAccessException e) {
            object = null;
        }
        return object;
    }

    protected Long queryForLong(String sql) throws DataException {
        return queryForLong(sql, null);
    }

    protected Long queryForLong(String sql, HashMap<String, ?> params) throws DataException {
        Long object;
        try {
            object = getJdbcTemplate().queryForObject(prepare(sql), params, Long.class);
        } catch (EmptyResultDataAccessException e) {
            object = null;
        }
        return object;
    }

    protected <I extends Mapping> Optional<I> queryForObjectOptional(SQLQueryDynamic sqlQuery) throws DataException {
        I result = queryForObject(createSqlPagination2Engine(sqlQuery), sqlQuery.getParams(), (Class<I>) sqlQuery.getClazz());
        return Optional.ofNullable(result);
    }

    private T queryForObject(String sql, HashMap<String, ?> params, AbstractRowMapper<T> rowMapper) throws DataException {
        T mapping;
        try {
            logSql(sql, params);
            mapping = getJdbcTemplate().queryForObject(prepare(sql), params, rowMapper);
        } catch (EmptyResultDataAccessException e) {
            mapping = null;
        }
        return mapping;
    }

    protected T queryForObject(String sql, HashMap<String, ?> params) throws DataException {
        return queryForObject(sql, params, true);
    }

    protected T queryForObject(String sql, HashMap<String, ?> params, boolean loadAll) throws DataException {
        T object;
        try {
            object = (T) queryForObject(sql, params, rowMapper(getGenericTypeClass(), loadAll));
        } catch (EmptyResultDataAccessException e) {
            object = null;
        }
        return object;
    }

    protected <I extends Mapping> I queryForObject(SQLQueryDynamic sqlQuery) throws DataException {
        return queryForObject(createSqlPagination2Engine(sqlQuery), sqlQuery.getParams(), (Class<I>) sqlQuery.getClazz());
    }

    protected <I extends Mapping> I queryForObject(String sql, HashMap<String, ?> params, Class<I> clazz) throws DataException {
        return queryForObject(sql, params, clazz, true);
    }

    protected <I extends Mapping> I queryForObject(String sql, HashMap<String, ?> params, Class<I> clazz, boolean loadAll) throws DataException {
        Object object;
        try {
            logSql(sql, params);
            object = getJdbcTemplate().queryForObject(prepare(sql), params, rowMapper(clazz, loadAll));
        } catch (EmptyResultDataAccessException e) {
            object = null;
        }
        return (I) object;
    }

    private AbstractRowMapper rowMapper(final Class clazz) {
        return JdbcCache.rowMapper(clazz, false);
    }

    private AbstractRowMapper rowMapper(final Class clazz, boolean loadAll) {
        return JdbcCache.rowMapper(clazz, loadAll);
    }

    protected int update(String sql, HashMap<String, ?> params) throws DataException {
        logSql(sql, params);
        return getJdbcTemplate().update(prepare(sql), params);
    }

    @Override
    public T update(T object) throws DataException {
        return updateAny(object);
    }

    protected <I extends Mapping> I updateAny(I object) throws DataException {
        Table table = getTableName(object.getClass());
        if (isNullOrUnassigned(table.keys())) {
            throw new DataException("It is necessary to specify the primary keys for the entity: " + object.getClass().getName());
        }

        List<String> primaryKeys = Arrays.asList(table.keys());
        HashMap<String, Object> params = prepareParams(Operation.UPDATE, object);
        StringBuilder sql = new StringBuilder();
        sql.append("UPDATE ").append(table.name());
        sql.append(" SET ");
        boolean firstElement = true;
        for (String key : params.keySet()) {
            if (primaryKeys.contains(key)) {
                continue;
            }
            sql.append(!firstElement ? "," : "");
            sql.append(key).append("=:").append(key);
            firstElement = false;
        }
        sql.append(" WHERE ");

        firstElement = true;
        for (String key : primaryKeys) {
            sql.append(!firstElement ? " AND " : "");
            sql.append(key).append("=:").append(key);
            firstElement = false;
        }
        update(sql.toString(), params);
        return object;
    }

    private Table getTableName(Class clazz) throws DataException {
        return getTableName(clazz, true);
    }

    private Table getTableName(Class clazz, boolean required) throws DataException {
        Table table = (Table) clazz.getAnnotation(Table.class);
        if (required && table == null) {
            throw new DataException("There is no annotation @Table into the class: " + clazz.getCanonicalName());
        }
        return table;
    }

    private boolean isPrimaryKey(Table table, String field) {
        boolean isPrimary = false;
        if (table != null) {
            for (String key : table.keys()) {
                if (key.equals(field)) {
                    isPrimary = true;
                    break;
                }
            }
        }
        return isPrimary;
    }

    private HashMap<String, Object> prepareParams(Operation operation, Object object) throws DataException {
        HashMap<String, Object> params = new HashMap<>();
        try {
            if (object != null) {

                Table table = object.getClass().getAnnotation(Table.class);
                for (Field field : JdbcCache.mappedFields(object.getClass())) {
                    Object value = field.get(object);

                    Column column = field.getAnnotation(Column.class);
                    if (column != null && ((operation == Operation.PERSIST && column.insertable()) || (operation == Operation.UPDATE && column.updatable()) || isPrimaryKey(table, column.name()))) {
                        if (column.parser() == EnumParser.class && value instanceof EnumIdentifiable) {
                            params.put(column.name(), ((EnumIdentifiable) value).getId());
                        } else if (column.parser() == EnumParser.class && value instanceof Enum) {
                            params.put(column.name(), ((Enum) value).name());
                        } else if (column.parser() == JsonParser.class || column.parser() == JsonListParser.class) {
                            params.put(column.name(), value == null ? null : getCustom().toJsonParameter(GsonUtils.custom.toJson(value)));
                        } else if (column.parser() == ByteaJsonParser.class || column.parser() == ByteaJsonListParser.class) {
                            byte[] bytes = value == null ? null : GsonUtils.custom.toJson(value).getBytes(JdbcCache.charset());
                            params.put(column.name(), bytes);
                        } else if (value instanceof StringWriter) {
                            byte[] bytes = ((StringWriter) value).toString().getBytes(JdbcCache.charset());
                            params.put(column.name(), bytes);
                        } else if (value instanceof String) {
                            String text = ((String) value).trim();
                            if (column.length() > 0 && text.length() > column.length()) {
                                log.warn("Value of column {}.{} truncated from {} to {} characters", table != null ? table.name() : object.getClass().getSimpleName(), column.name(), text.length(),
                                        column.length());
                                text = text.substring(0, column.length());
                            }
                            params.put(column.name(), column.encrypted() ? CryptographyUtils.encrypt(text) : text);
                        } else {
                            boolean isZeroOrNull = (value == null || (value instanceof Long && ((Long) value) == 0) || (value instanceof Integer && ((Integer) value) == 0));
                            if (!column.serial() || (column.serial() && !isZeroOrNull)) {
                                params.put(column.name(), value);
                            }
                        }
                    } else {
                        ColumnGroup columnMapping = field.getAnnotation(ColumnGroup.class);
                        if (columnMapping != null) {
                            params.putAll(prepareParams(operation, value));
                        } else {
                            JoinTable joinTable = field.getAnnotation(JoinTable.class);
                            if (joinTable != null) {
                                Object tableValue = field.get(object);
                                if (tableValue != null) {
                                    params.put(joinTable.foreignKey(), extractPrimaryKey(tableValue));
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            throw new DataException("Error preparing parameter of the Object", ex);
        }
        return params;
    }

    private Object extractPrimaryKey(Object entity) throws IllegalArgumentException, IllegalAccessException {
        Table table = entity.getClass().getAnnotation(Table.class);
        if (table == null || isNullOrUnassigned(table.keys())) {
            throw new DataException("It is necessary to specify the primary keys for the entity: " + entity.getClass().getName());
        }
        String key = table.keys()[0];
        Object value = null;
        for (Field field : JdbcCache.mappedFields(entity.getClass())) {
            Column column = field.getAnnotation(Column.class);
            if (column != null && column.name().equals(key)) {
                value = field.get(entity);
                if (column.parser() == EnumParser.class && value instanceof EnumIdentifiable) {
                    value = ((EnumIdentifiable) value).getId();
                } else if (column.parser() == EnumParser.class && value instanceof Enum) {
                    value = ((Enum) value).name();
                }

                break;
            }
        }

        return value;
    }

    protected CustomEngine getCustom() {
        Engine engine = getEngine();
        if (null == engine) {
            throw new DataException("Engine not supported");
        }
        return engine.getCustomEngine();
    }

    private boolean isArray(Object obj) {
        return obj != null && obj.getClass().isArray();
    }
}
