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

import cl.kanopus.common.util.Utils;
import cl.kanopus.common.util.crypto.CryptographyUtils;
import cl.kanopus.jdbc.entity.Mapping;
import cl.kanopus.jdbc.entity.annotation.*;
import cl.kanopus.jdbc.entity.mapper.AbstractRowMapper;
import cl.kanopus.jdbc.util.parser.*;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.BadPaddingException;

/**
 * @author Pablo Diaz Saavedra
 *         <p>
 *         This utility class is responsible for creating the "RowMapper" dynamically according to the specified class and stores its definition in the application's CACHE, to increase the response speed in each conversion process from ResultSet to Objects.
 *         <p>
 *         All caches are thread safe and the cached maps are immutable: callers that need to modify them must work on a copy.
 */
@SuppressWarnings("all")
public class JdbcCache {

    /**
     * System property used to choose the charset used to convert bytea/binary columns to text (JSON parsers and {@link StringWriter} fields). Defaults to UTF-8. Use {@code -Dkanopus.jdbc.charset=ISO-8859-1} to keep the behavior of versions prior to 4.20.0.
     */
    public static final String CHARSET_PROPERTY = "kanopus.jdbc.charset";

    private static final Charset DEFAULT_CHARSET = resolveCharset();

    private static final Map<String, Map<String, String>> translationMapCache = new ConcurrentHashMap<>();
    private static final Map<String, AbstractRowMapper> rowMapperCache = new ConcurrentHashMap<>();
    private static final Map<String, SqlBase> sqlBaseCache = new ConcurrentHashMap<>();
    private static final Map<Class<?>, List<Field>> fieldsCache = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Constructor<?>> constructorCache = new ConcurrentHashMap<>();

    private JdbcCache() {
        throw new IllegalStateException("Utility class");
    }

    private static Charset resolveCharset() {
        String name = System.getProperty(CHARSET_PROPERTY);
        try {
            return Utils.isNullOrEmpty(name) ? StandardCharsets.UTF_8 : Charset.forName(name);
        } catch (RuntimeException ex) {
            return StandardCharsets.UTF_8;
        }
    }

    /**
     * @return charset used to convert binary columns to text and vice versa
     */
    public static Charset charset() {
        return DEFAULT_CHARSET;
    }

    public static String geyKeyCache(String canonicalName, boolean loadAll) {
        return canonicalName + (loadAll ? "_ALL" : "_LAZY");
    }

    public static SqlBase sqlBase(Class<? extends Mapping> clazz) {
        return sqlBase(clazz, false);
    }

    public static SqlBase sqlBase(Class<? extends Mapping> clazz, boolean loadAll) {
        String key = geyKeyCache(clazz.getName(), loadAll);
        return sqlBaseCache.computeIfAbsent(key, k -> createSqlBase(clazz, loadAll));
    }

    private static SqlBase createSqlBase(Class<? extends Mapping> clazz, boolean loadAll) {
        View view = (View) clazz.getDeclaredAnnotation(View.class);
        if (view != null) {
            checkAnnotationNotSupported(clazz);
            return new SqlBase(null, view.value(), new HashMap<>());
        } else {
            SQLCreator creator = new SQLCreator();
            prepareCreator(null, null, clazz, loadAll, creator);
            return new SqlBase(creator.getTable(), creator.generate(), creator.getAliasMap());
        }
    }

    public static class SqlBase {

        private final String sql;
        private final Map<String, String> aliasMap;

        public SqlBase(String tableBase, String sql, Map<String, String> aliasMap) {
            this.sql = sql;
            this.aliasMap = Collections.unmodifiableMap(new HashMap<>(aliasMap));
        }

        public String getSql() {
            return sql;
        }

        /**
         * @return an immutable view of the table/alias map
         */
        public Map<String, String> getAliasMap() {
            return aliasMap;
        }
    }

    /**
     * Returns the fields of {@code clazz} that can hold mapping annotations: its declared fields plus the fields declared in its superclasses that are not themselves annotated with {@link Table} or {@link View} (a superclass that is an entity by itself is not merged into its children). Static and
     * synthetic fields are ignored. The returned fields are already accessible.
     *
     * @param clazz
     *            class to inspect
     * @return immutable list of fields
     */
    public static List<Field> mappedFields(Class<?> clazz) {
        return fieldsCache.computeIfAbsent(clazz, JdbcCache::collectFields);
    }

    private static List<Field> collectFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class && current != Mapping.class) {
            if (current != clazz && (current.isAnnotationPresent(Table.class) || current.isAnnotationPresent(View.class))) {
                break;
            }
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                if (field.isAnnotationPresent(Column.class) || field.isAnnotationPresent(ColumnGroup.class) || field.isAnnotationPresent(JoinTable.class)) {
                    field.setAccessible(true);
                    fields.add(field);
                }
            }
            current = current.getSuperclass();
        }
        return Collections.unmodifiableList(fields);
    }

    private static Object newInstance(Class<?> clazz) throws ReflectiveOperationException {
        Constructor<?> constructor = constructorCache.computeIfAbsent(clazz, c -> {
            try {
                Constructor<?> ctor = c.getDeclaredConstructor();
                ctor.setAccessible(true);
                return ctor;
            } catch (NoSuchMethodException ex) {
                throw new IllegalStateException("Class " + c.getName() + " requires a no-arguments constructor", ex);
            }
        });
        return constructor.newInstance();
    }

    public static AbstractRowMapper rowMapper(Class clazz, boolean loadAll) {
        String key = geyKeyCache(clazz.getName(), loadAll);
        return rowMapperCache.computeIfAbsent(key, k -> createRowMapper(clazz, loadAll));
    }

    private static AbstractRowMapper createRowMapper(Class clazz, boolean loadAll) {
        return new AbstractRowMapper() {
            @Override
            public Object mapRow(ResultSet rs, int i) throws SQLException {
                Object object;
                try {
                    object = newInstance(clazz);
                    for (Field field : mappedFields(clazz)) {
                        Column column = field.getAnnotation(Column.class);
                        if (column != null) {
                            mapColumn(rs, object, field, column);
                        } else {
                            ColumnGroup columnMapping = field.getAnnotation(ColumnGroup.class);
                            if (columnMapping != null) {
                                try {
                                    field.set(object, rowMapper(columnMapping.result(), loadAll).mapRow(rs, i));
                                } catch (Exception ex) {
                                    // Internal mapping is not feasible or SQL incomplete
                                    field.set(object, null);
                                    if (!columnMapping.nullable()) {
                                        throw new SQLException(columnMapping.result().getName() + " cannot be null", ex.getMessage(), ex);
                                    }
                                    if (hasCause(ex, BadPaddingException.class)) {
                                        throw new SQLException(columnMapping.result().getName() + " cannot be decrypted using security key", ex.getMessage(), ex);
                                    }
                                }
                            } else {
                                JoinTable joinTable = field.getAnnotation(JoinTable.class);
                                if (joinTable != null) {
                                    try {
                                        field.set(object, rowMapper(joinTable.table(), loadAll).mapRow(rs, i));
                                    } catch (Exception ex) {
                                        // Internal mapping is not feasible or SQL incomplete (e.g. lazy join)
                                        field.set(object, null);
                                        if (hasCause(ex, BadPaddingException.class)) {
                                            throw new SQLException(joinTable.table().getName() + " cannot be decrypted using security key", ex.getMessage(), ex);
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (SQLException ex) {
                    throw new SQLException("Error performing automatic rowmapper of " + clazz + ": " + ex.getMessage(), ex.getSQLState(), ex);
                } catch (Exception ex) {
                    throw new SQLException("Error performing automatic rowmapper of " + clazz + ": " + ex.getMessage(), ex);
                }
                return object;
            }
        };
    }

    private static void mapColumn(ResultSet rs, Object object, Field field, Column column) throws Exception {
        String label = !Utils.isNullOrEmpty(column.alias()) ? column.alias() : column.name();
        Class parser = column.parser();
        if (parser == EnumParser.class) {
            field.set(object, EnumParser.parseValue(column.parserResult(), rs.getObject(label)));
        } else if (parser == JsonListParser.class) {
            field.set(object, JsonListParser.parse(column.parserResult(), rs.getString(label)));
        } else if (parser == JsonParser.class) {
            field.set(object, JsonParser.parse(column.parserResult(), rs.getString(label)));
        } else if (parser == ByteaJsonListParser.class) {
            field.set(object, ByteaJsonListParser.parse(column.parserResult(), bytesToString(rs.getBytes(label))));
        } else if (parser == ByteaJsonParser.class) {
            field.set(object, ByteaJsonParser.parse(column.parserResult(), bytesToString(rs.getBytes(label))));
        } else {
            Object columnObject = rs.getObject(label);
            if (column.encrypted() && columnObject instanceof String text) {
                columnObject = CryptographyUtils.decrypt(text);
            }
            Object value = convert(columnObject, field.getType());
            if (value != null || !field.getType().isPrimitive()) {
                field.set(object, value);
            }
        }
    }

    private static String bytesToString(byte[] bytes) {
        return bytes == null ? null : new String(bytes, DEFAULT_CHARSET);
    }

    private static boolean hasCause(Throwable ex, Class<? extends Throwable> type) {
        Throwable current = ex;
        int guard = 0;
        while (current != null && guard++ < 20) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    /**
     * Converts a value returned by the JDBC driver to the type of the target field. Unknown combinations are returned unchanged.
     *
     * @param value
     *            value obtained with {@code ResultSet.getObject}
     * @param target
     *            type of the field
     * @return converted value
     */
    static Object convert(Object value, Class<?> target) {
        if (value == null || target.isInstance(value)) {
            return value;
        }
        if (value instanceof Number number) {
            Object converted = convertNumber(number, target);
            if (converted != null) {
                return converted;
            }
        }
        if (value instanceof byte[] bytes && target == StringWriter.class) {
            StringWriter str = new StringWriter();
            str.write(new String(bytes, DEFAULT_CHARSET));
            return str;
        }
        if (value instanceof java.sql.Timestamp ts) {
            if (target == LocalDateTime.class) {
                return ts.toLocalDateTime();
            } else if (target == LocalDate.class) {
                return ts.toLocalDateTime().toLocalDate();
            } else if (target == Instant.class) {
                return ts.toInstant();
            } else if (target == OffsetDateTime.class) {
                return ts.toInstant().atZone(ZoneId.systemDefault()).toOffsetDateTime();
            } else if (target == ZonedDateTime.class) {
                return ts.toInstant().atZone(ZoneId.systemDefault());
            }
        }
        if (value instanceof java.sql.Date date) {
            if (target == LocalDate.class) {
                return date.toLocalDate();
            } else if (target == LocalDateTime.class) {
                return date.toLocalDate().atStartOfDay();
            }
        }
        if (value instanceof OffsetDateTime odt) {
            if (target == LocalDateTime.class) {
                return odt.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
            } else if (target == Instant.class) {
                return odt.toInstant();
            } else if (target == java.util.Date.class) {
                return java.util.Date.from(odt.toInstant());
            } else if (target == LocalDate.class) {
                return odt.atZoneSameInstant(ZoneId.systemDefault()).toLocalDate();
            } else if (target == ZonedDateTime.class) {
                return odt.toZonedDateTime();
            }
        }
        if (value instanceof LocalDateTime ldt) {
            if (target == java.util.Date.class) {
                return java.sql.Timestamp.valueOf(ldt);
            } else if (target == LocalDate.class) {
                return ldt.toLocalDate();
            }
        }
        if (value instanceof LocalDate ld && target == java.util.Date.class) {
            return java.sql.Date.valueOf(ld);
        }
        if (target == UUID.class && value instanceof String text) {
            return UUID.fromString(text);
        }
        if (target == String.class && (value instanceof UUID || value instanceof Character)) {
            return value.toString();
        }
        return value;
    }

    private static Object convertNumber(Number number, Class<?> target) {
        if (target == Long.class || target == long.class) {
            return number.longValue();
        } else if (target == Integer.class || target == int.class) {
            return number.intValue();
        } else if (target == Short.class || target == short.class) {
            return number.shortValue();
        } else if (target == Byte.class || target == byte.class) {
            return number.byteValue();
        } else if (target == Double.class || target == double.class) {
            return number.doubleValue();
        } else if (target == Float.class || target == float.class) {
            return number.floatValue();
        } else if (target == BigDecimal.class) {
            return (number instanceof BigInteger bi) ? new BigDecimal(bi) : new BigDecimal(number.toString());
        } else if (target == BigInteger.class) {
            return (number instanceof BigDecimal bd) ? bd.toBigInteger() : BigInteger.valueOf(number.longValue());
        } else if (target == Boolean.class || target == boolean.class) {
            return number.intValue() != 0;
        }
        return null;
    }

    /**
     * @param clazz
     *            entity class
     * @return an immutable map property path -> qualified column name
     */
    public static Map<String, String> translationMap(Class<? extends Mapping> clazz) {
        return translationMapCache.computeIfAbsent(clazz.getName(), k -> {
            Table table = (Table) clazz.getDeclaredAnnotation(Table.class);
            Map<String, String> translationMap = new HashMap<>();
            translationMapExtract(clazz, translationMap, "", table);
            return Collections.unmodifiableMap(translationMap);
        });
    }

    private static void translationMapExtract(Class clazz, Map<String, String> translationMap, String prefix, Table parent) {

        Table table = (Table) clazz.getDeclaredAnnotation(Table.class);
        String tableName;
        if (table != null) {
            tableName = table.name();
        } else if (parent != null) {
            tableName = parent.name();
        } else {
            tableName = null;
        }

        for (Field field : mappedFields(clazz)) {
            Column column = field.getAnnotation(Column.class);
            if (column != null) {
                translationMap.putIfAbsent(prefix + field.getName(), tableName != null ? tableName + "." + column.name() : column.name());
            } else {
                ColumnGroup columnMapping = field.getAnnotation(ColumnGroup.class);
                if (columnMapping != null) {
                    translationMapExtract(columnMapping.result(), translationMap, prefix + field.getName() + ".", (table != null) ? table : parent);
                } else {
                    JoinTable joinTable = field.getAnnotation(JoinTable.class);
                    if (joinTable != null) {
                        translationMapExtract(joinTable.table(), translationMap, prefix + field.getName() + ".", (table != null) ? table : parent);
                    }
                }
            }
        }
    }

    private static void prepareCreator(Table parent, JoinTable joined, Class currentClazz, boolean loadAll, SQLCreator creator) {

        String tableName = null;
        Table table = (Table) currentClazz.getDeclaredAnnotation(Table.class);
        if (table != null) {
            tableName = table.name();
            if (creator.isEmpty()) {
                creator.setTable(table.name());
            } else if (joined != null) {
                creator.addJoinTable(parent, joined);
            }
        } else {
            if (parent == null) {
                throw new IllegalStateException("There is no annotation @Table or @View into the class: " + currentClazz.getName());
            }
            tableName = parent.name();
            table = parent;
        }

        for (Field field : mappedFields(currentClazz)) {

            Column column = field.getAnnotation(Column.class);
            if (column != null) {
                creator.addColumn(tableName, column.name(), column.alias());
            } else {
                JoinTable joinTable = field.getAnnotation(JoinTable.class);
                if (joinTable != null && (!joinTable.lazy() || loadAll)) {
                    prepareCreator(table, joinTable, joinTable.table(), loadAll, creator);
                } else {
                    ColumnGroup columnGroup = field.getAnnotation(ColumnGroup.class);
                    if (columnGroup != null) {
                        prepareCreator(table, joinTable, columnGroup.result(), loadAll, creator);
                    }
                }
            }
        }
    }

    private static void checkAnnotationNotSupported(Class currentClazz) {
        for (Field field : mappedFields(currentClazz)) {
            JoinTable joinTable = field.getAnnotation(JoinTable.class);
            if (joinTable != null) {
                throw new UnsupportedOperationException("JoinTable not supported using View");
            }
        }
    }

    static class SQLCreator {

        private int index = 0;
        private String table;
        private final List<String> columns = new ArrayList<>();

        private final StringBuilder sqlJoins = new StringBuilder();
        private final Map<String, String> aliasMap = new HashMap<>();

        public boolean isEmpty() {
            return table == null || table.isEmpty();
        }

        public void setTable(String table) {
            this.table = table;
            generateAlias(table);
        }

        public void addJoinTable(Table parent, JoinTable joinTable) {
            Table tableJoin = joinTable.table().getDeclaredAnnotation(Table.class);
            if (tableJoin.keys() == null || tableJoin.keys().length != 1) {
                throw new IllegalStateException("Error JoinTable without keys defined");
            }
            String alias = generateAlias(tableJoin.name());
            String aliasParent = getAlias(parent.name());

            sqlJoins.append(" ").append(joinTable.operator().toString().replace("_", " ")).append(" ").append(tableJoin.name());
            sqlJoins.append(" ").append(alias);
            sqlJoins.append(" ON ").append(aliasParent).append(".").append(joinTable.foreignKey()).append("=").append(alias).append(".").append(tableJoin.keys()[0]);
        }

        public String generate() {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT ");
            for (int i = 0; i < columns.size(); i++) {
                sql.append(i == 0 ? "" : ", ");
                sql.append(columns.get(i));
            }

            sql.append(" FROM ").append(table);
            sql.append(" ").append(aliasMap.get(table));
            if (sqlJoins.length() > 0) {
                sql.append(sqlJoins.toString());
            }

            return sql.toString();
        }

        public String getTable() {
            return table;
        }

        public Map<String, String> getAliasMap() {
            return aliasMap;
        }

        private String generateAlias(String name) {
            index++;
            String alias = "t" + index;
            aliasMap.put(name, alias);
            return alias;
        }

        private String getAlias(String name) {
            return aliasMap.get(name);
        }

        private void addColumn(String tableName, String column, String alias) {
            columns.add(getAlias(tableName).concat(".").concat(column).concat(!Utils.isNullOrEmpty(alias) ? " AS ".concat(alias) : ""));
        }
    }
}
