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

import cl.kanopus.jdbc.util.SqlUtils;
import java.util.regex.Pattern;

public class OracleEngine implements CustomEngine {

    private static final Pattern CAST_DATE = Pattern.compile("([\\\\.a-zA-Z0-9_-]+)::date", Pattern.CASE_INSENSITIVE);

    protected OracleEngine() {
    }

    // Singleton Instance
    private static class SingletonHolder {

        public static final OracleEngine INSTANCE = new OracleEngine();
    }

    public static OracleEngine getInstance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    public StringBuilder createSqlPagination(String sql, int limit, int offset) {
        StringBuilder sqlPagination = new StringBuilder();
        sqlPagination.append("SELECT * FROM (");
        sqlPagination.append("SELECT consulta.*, rownum rn FROM(");
        sqlPagination.append(sql);
        sqlPagination.append(") consulta ");
        sqlPagination.append(") WHERE rn<= ").append(limit + offset);
        if (offset > 0) {
            sqlPagination.append(" AND rn > ").append(offset);
        }

        return sqlPagination;
    }

    @Override
    public String createSqlNextval(String sequence) {
        return "SELECT " + sequence + ".nextval FROM dual";
    }

    /**
     * In Oracle {@code CAST(timestamp AS DATE)} keeps the time part, {@code TRUNC} is used instead.
     */
    @Override
    public String castToDate(String column) {
        return "TRUNC(" + column + ")";
    }

    @Override
    public String prepareSQL2Engine(String sql) {
        return SqlUtils.transformOutsideLiterals(sql, code -> CAST_DATE.matcher(code).replaceAll("TRUNC($1)"));
    }
}
