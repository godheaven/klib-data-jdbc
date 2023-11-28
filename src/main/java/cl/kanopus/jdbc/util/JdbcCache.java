package cl.kanopus.jdbc.util;

import cl.kanopus.common.util.CryptographyUtils;
import cl.kanopus.common.util.Utils;
import cl.kanopus.jdbc.entity.Mapping;
import cl.kanopus.jdbc.entity.annotation.Column;
import cl.kanopus.jdbc.entity.annotation.Table;
import cl.kanopus.jdbc.entity.mapper.AbstractRowMapper;
import cl.kanopus.jdbc.util.parser.EnumParser;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import cl.kanopus.jdbc.entity.annotation.ColumnGroup;
import cl.kanopus.jdbc.entity.annotation.JoinTable;
import cl.kanopus.jdbc.entity.annotation.View;
import cl.kanopus.jdbc.util.parser.ByteaJsonListParser;
import cl.kanopus.jdbc.util.parser.ByteaJsonParser;
import cl.kanopus.jdbc.util.parser.JsonListParser;
import cl.kanopus.jdbc.util.parser.JsonParser;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.BadPaddingException;

/**
 *
 * @author Pablo Diaz Saavedra
 * @email pabloandres.diazsaavedra@gmail.com
 *
 * This utility class is responsible for creating the "RowMapper" dynamically
 * according to the specified class and stores its definition in the
 * application's CACHE, to increase the response speed in each conversion
 * process from ResultSet to Objects.
 */
@SuppressWarnings("all")
public class JdbcCache {

    private static final Charset DEFAULT_CHARSET = StandardCharsets.ISO_8859_1;

    private static final Map<String, Map<String, String>> translationMapCache = new HashMap<>();
    private static final Map<String, AbstractRowMapper> rowMapperCache = new HashMap<>();
    private static final Map<String, String> sqlBaseCache = new HashMap<>();

    private JdbcCache() {
        throw new IllegalStateException("Utility class");
    }

    public static String geyKeyCache(String canonicalName, boolean loadAll) {
        return canonicalName + (loadAll ? "_ALL" : "_LAZY");
    }

    public static String sqlBase(Class<? extends Mapping> clazz) {
        return sqlBase(clazz, false);
    }

    public static String sqlBase(Class<? extends Mapping> clazz, boolean loadAll) {
        String key = geyKeyCache(clazz.getCanonicalName(), loadAll);
        String sql = sqlBaseCache.get(key);
        if (sql == null) {
            View view = (View) clazz.getDeclaredAnnotation(View.class);
            if (view != null) {
                checkAnnotationNotSupported(clazz);
                sql = view.value();
                sqlBaseCache.put(key, sql);
            } else {
                SQLCreator creator = new SQLCreator();
                sqlBase(null, null, clazz, loadAll, creator);
                sql = creator.generate();
                sqlBaseCache.put(key, sql);
            }

        }
        return sql;
    }

    public static AbstractRowMapper rowMapper(Class clazz, boolean loadAll) {
        String key = geyKeyCache(clazz.getCanonicalName(), loadAll);
        AbstractRowMapper rowMapper = rowMapperCache.get(key);
        if (rowMapper == null) {
            rowMapper = new AbstractRowMapper() {
                @Override
                public Object mapRow(ResultSet rs, int i) throws SQLException {
                    Object object;
                    try {
                        object = clazz.newInstance();
                        for (Field field : clazz.getDeclaredFields()) {
                            Column column = field.getAnnotation(Column.class);
                            if (column != null) {
                                // this is for private scope
                                field.setAccessible(true);
                                if (column.parser() != null && column.parser() == EnumParser.class) {
                                    String enumId = rs.getString(column.name());
                                    field.set(object, EnumParser.parse(column.parserResult(), isNumeric(enumId) ? Integer.valueOf(enumId) : enumId));
                                } else if (column.parser() != null && column.parser() == JsonListParser.class) {
                                    String json = rs.getString(column.name());
                                    field.set(object, JsonListParser.parse(column.parserResult(), json));
                                } else if (column.parser() != null && column.parser() == JsonParser.class) {
                                    String json = rs.getString(column.name());
                                    field.set(object, JsonParser.parse(column.parserResult(), json));
                                } else if (column.parser() != null && column.parser() == ByteaJsonListParser.class) {
                                    byte[] bytes = rs.getBytes(column.name());
                                    String json = new String((byte[]) bytes, DEFAULT_CHARSET); //TODO: parametrizar el encoding
                                    field.set(object, ByteaJsonListParser.parse(column.parserResult(), json));
                                } else if (column.parser() != null && column.parser() == ByteaJsonParser.class) {
                                    byte[] bytes = rs.getBytes(column.name());
                                    String json = new String((byte[]) bytes, DEFAULT_CHARSET); //TODO: parametrizar el encoding
                                    field.set(object, ByteaJsonParser.parse(column.parserResult(), json));
                                } else {
                                    Object columnObject = rs.getObject(!Utils.isNullOrEmpty(column.alias()) ? column.alias() : column.name());
                                    if (columnObject instanceof BigDecimal && field.getType() == Long.class) {
                                        field.set(object, (((BigDecimal) columnObject).longValue()));
                                    } else if (columnObject instanceof Integer && field.getType() == Long.class) {
                                        field.set(object, (((Integer) columnObject).longValue()));
                                    } else if (columnObject instanceof byte[] && field.getType() == StringWriter.class) {
                                        StringWriter str = new StringWriter();
                                        str.write(new String((byte[]) columnObject, DEFAULT_CHARSET));
                                        field.set(object, str); //TODO: parametrizar el encoding
                                    } else if (columnObject instanceof java.sql.Date && field.getType() == LocalDate.class) {
                                        field.set(object, ((java.sql.Date) columnObject).toLocalDate());
                                    } else if (columnObject instanceof java.sql.Timestamp && field.getType() == LocalDateTime.class) {
                                        field.set(object, ((java.sql.Timestamp) columnObject).toLocalDateTime());
                                    } else if (columnObject instanceof java.sql.Timestamp && field.getType() == LocalDate.class) {
                                        field.set(object, ((java.sql.Timestamp) columnObject).toLocalDateTime().toLocalDate());
                                    } else {
                                        field.set(object, column.encrypted() ? CryptographyUtils.decrypt((String) columnObject) : columnObject);
                                    }

                                }
                            } else {
                                ColumnGroup columnMapping = field.getAnnotation(ColumnGroup.class);
                                if (columnMapping != null) {
                                    try {
                                        // this is for private scope
                                        field.setAccessible(true);
                                        field.set(object, rowMapper(columnMapping.result(), loadAll).mapRow(rs, i));

                                    } catch (Exception ex) {
                                        //Internal mapping is not feasible or SQL incomplete
                                        field.setAccessible(true);
                                        field.set(object, null);
                                        if (!columnMapping.nullable()) {
                                            throw new SQLException(columnMapping.result().getName() + " cannot be null", ex.getMessage());
                                        }
                                        if (ex.getCause().getCause() != null && ex.getCause().getCause().getClass() == BadPaddingException.class) {
                                            throw new SQLException(columnMapping.result().getName() + " cannot be decrypted using security key", ex.getMessage());
                                        }
                                    }
                                } else {
                                    JoinTable joinTable = field.getAnnotation(JoinTable.class);
                                    if (joinTable != null) {
                                        try {
                                            // this is for private scope
                                            field.setAccessible(true);
                                            field.set(object, rowMapper(joinTable.table(), loadAll).mapRow(rs, i));
                                        } catch (Exception ex) {
                                            //Internal mapping is not feasible or SQL incomplete
                                            field.setAccessible(true);
                                            field.set(object, null);

                                            //TODO: se debe implementar el lanzar excepcion solo en ciertas condiciones -->    throw ex;
                                            if (ex.getCause().getCause() != null && ex.getCause().getCause().getClass() == BadPaddingException.class) {
                                                throw new SQLException(columnMapping.result().getName() + " cannot be decrypted using security key", ex.getMessage());
                                            }

                                        }
                                    }

                                }
                            }

                        }
                    } catch (Exception ex) {
                        throw new SQLException("Error performing automatic rowmapper of " + clazz + ": " + ex.getMessage(), ex);
                    }
                    return object;
                }
            };
            rowMapperCache.put(key, rowMapper);
        }
        return rowMapper;
    }

    public static Map<String, String> translationMap(Class<? extends Mapping> clazz) {
        Map<String, String> translationMap = translationMapCache.get(clazz.getCanonicalName());
        if (translationMap == null || translationMap.isEmpty()) {
            translationMap = new HashMap<>();
            translationMapExtract(clazz, translationMap, "");
            translationMapCache.put(clazz.getCanonicalName(), translationMap);
        }
        return translationMap;

    }

    private static void translationMapExtract(Class clazz, Map<String, String> translationMap, String prefix) {

        for (Field field : clazz.getDeclaredFields()) {
            Column column = field.getAnnotation(Column.class);
            if (column != null) {
                translationMap.put(prefix + field.getName(), column.name());
            } else {
                ColumnGroup columnMapping = field.getAnnotation(ColumnGroup.class);
                if (columnMapping != null) {
                    translationMapExtract(columnMapping.result(), translationMap, prefix + field.getName() + ".");
                } else {
                    JoinTable joinTable = field.getAnnotation(JoinTable.class);
                    if (joinTable != null) {
                        translationMapExtract(joinTable.table(), translationMap, prefix + field.getName() + ".");
                    }
                }
            }
        }

    }

    private static boolean isNumeric(String str) {
        return (!Utils.isNullOrEmpty(str)) ? str.matches("[0-9]+") : false;
    }

    private static void sqlBase(Table parent, JoinTable joined, Class currentClazz, boolean loadAll, SQLCreator creator) {

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
            tableName = parent.name();
            table = parent;
        }

        for (Field field : currentClazz.getDeclaredFields()) {

            Column column = field.getAnnotation(Column.class);
            if (column != null) {
                creator.addColumn(tableName, column.name(), column.alias());
            } else {
                JoinTable joinTable = field.getAnnotation(JoinTable.class);
                if (joinTable != null && (!joinTable.lazy() || loadAll)) {
                    sqlBase(table, joinTable, joinTable.table(), loadAll, creator);
                } else {
                    ColumnGroup columnGroup = field.getAnnotation(ColumnGroup.class);
                    if (columnGroup != null) {
                        sqlBase(table, joinTable, columnGroup.result(), loadAll, creator);
                    }
                }

            }

        }

    }

    private static void checkAnnotationNotSupported(Class currentClazz) {
        for (Field field : currentClazz.getDeclaredFields()) {
            JoinTable joinTable = field.getAnnotation(JoinTable.class);
            if (joinTable != null) {
                throw new RuntimeException("JoinTable not suṕported using View");
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
                throw new RuntimeException("Error JoinTable without keys defined");
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
