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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Pablo Diaz Saavedra
 */
public class SQLServerEngine implements CustomEngine {

    final Pattern pattern = Pattern.compile("([\\\\.a-zA-Z0-9_-]+)::date", Pattern.MULTILINE);

    private static final Pattern COUNT = Pattern.compile("(?i)count\\(\\s*\\*\\s*\\)");
    private static final Pattern TO_TIMESTAMP_DATETIME = Pattern.compile("(?i)TO_TIMESTAMP\\(\\s*(:[\\w.]+)\\s*,\\s*'YYYY-MM-DD HH24:MI:SS'\\s*\\)");
    private static final Pattern TO_TIMESTAMP_DATE = Pattern.compile("(?i)TO_TIMESTAMP\\(\\s*(:[\\w.]+)\\s*,\\s*'YYYY-MM-DD'\\s*\\)");
    private static final Pattern DELETE_WITH_ALIAS = Pattern.compile("(?i)^\\s*DELETE\\s+FROM\\s+([\\w.$\\[\\]]+)\\s+(?!WHERE\\b)([\\w$]+)(\\s+WHERE\\b|\\s*$)");

    protected SQLServerEngine() {
    }

    // Singleton Instance
    private static class SingletonHolder {

        public static final SQLServerEngine INSTANCE = new SQLServerEngine();
    }

    public static SQLServerEngine getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * SQL Server requires an ORDER BY clause to use OFFSET/FETCH; when the query has no top level ORDER BY, {@code ORDER BY (SELECT NULL)} is added.
     */
    @Override
    public StringBuilder createSqlPagination(String sql, int limit, int offset) {
        StringBuilder sqlPagination = new StringBuilder();
        sqlPagination.append(sql);
        if (SqlUtils.indexOfTopLevel(sql, "ORDER BY") < 0) {
            sqlPagination.append(" ORDER BY (SELECT NULL)");
        }
        sqlPagination.append(" offset ").append(offset);
        sqlPagination.append(" rows fetch next ").append(limit);
        sqlPagination.append(" rows only ");
        return sqlPagination;
    }

    @Override
    public String createSqlNextval(String sequence) {
        return "SELECT NEXT VALUE FOR " + sequence;
    }

    @Override
    public String toTimestamp(String parameterName, String pattern) {
        return "CONVERT(DATETIME2, :" + parameterName + ", " + (DATE_PATTERN.equals(pattern) ? "23" : "120") + ")";
    }

    @Override
    public String concat(String... parts) {
        return String.join("+", parts);
    }

    @Override
    public String prepareSQL2Engine(String sql) {
        if (sql == null) {
            return null;
        }
        // TO_TIMESTAMP uses a literal as second argument, so it is translated before skipping literals
        String newSql = TO_TIMESTAMP_DATETIME.matcher(sql).replaceAll("CONVERT(DATETIME2, $1, 120)");
        newSql = TO_TIMESTAMP_DATE.matcher(newSql).replaceAll("CONVERT(DATETIME2, $1, 23)");

        newSql = SqlUtils.transformOutsideLiterals(newSql, code -> {
            // Usar replace() en lugar de replaceAll() (no evalúa regex)
            String fragment = code.replace("||", "+");
            // Reemplazo de count(*) por count_big(*) sin importar mayúsculas/minúsculas
            fragment = COUNT.matcher(fragment).replaceAll("count_big(*)");
            Matcher matcher = pattern.matcher(fragment);
            return matcher.replaceAll("CAST($1 as date)");
        });

        // SQL Server does not accept "DELETE FROM table alias", it requires "DELETE alias FROM table alias"
        Matcher delete = DELETE_WITH_ALIAS.matcher(newSql);
        if (delete.find()) {
            newSql = delete.replaceFirst("DELETE $2 FROM $1 $2$3");
        }
        return newSql;
    }
}
