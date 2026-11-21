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
package cl.kanopus.jdbc.impl.engine;

import org.postgresql.util.PGobject;

public class PostgresEngine implements CustomEngine {

    protected PostgresEngine() {
    }

    // Singleton Instance
    private static class SingletonHolder {

        public static final PostgresEngine INSTANCE = new PostgresEngine();
    }

    public static PostgresEngine getInstance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    public StringBuilder createSqlPagination(String sql, int limit, int offset) {
        StringBuilder sqlPagination = new StringBuilder();
        sqlPagination.append(sql);
        sqlPagination.append(" limit ").append(limit);
        if (offset > 0) {
            sqlPagination.append(" offset ").append(offset);
        }
        return sqlPagination;
    }

    @Override
    public String createSqlNextval(String sequence) {
        return "SELECT nextval('" + sequence + "')";
    }

    @Override
    public String prepareSQL2Engine(String sql) {
        return sql;
    }

    @Override
    public String castToDate(String column) {
        return column + "::date";
    }

    /**
     * JSON values are bound as {@code PGobject} of type json, so they can be stored into json/jsonb columns. The PostgreSQL driver is only required when this method is invoked.
     */
    @Override
    public Object toJsonParameter(String json) {
        return PgJson.create(json);
    }

    /** Isolates the reference to the PostgreSQL driver class. */
    private static final class PgJson {

        private PgJson() {
        }

        static Object create(String json) {
            try {
                PGobject jsonObject = new PGobject();
                jsonObject.setType("json");
                jsonObject.setValue(json);
                return jsonObject;
            } catch (java.sql.SQLException ex) {
                throw new IllegalStateException("Unable to create PostgreSQL json parameter", ex);
            }
        }
    }
}
