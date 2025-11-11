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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Pablo Diaz Saavedra
 *
 */
public class SQLServerEngine implements CustomEngine {

    final Pattern pattern = Pattern.compile("([\\\\.a-zA-Z0-9_-]+)::date", Pattern.MULTILINE);

    protected SQLServerEngine() {
    }

    //Singleton Instance
    private static class SingletonHolder {

        public static final SQLServerEngine INSTANCE = new SQLServerEngine();
    }

    public static SQLServerEngine getInstance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    public StringBuilder createSqlPagination(String sql, int limit, int offset) {
        StringBuilder sqlPagination = new StringBuilder();
        sqlPagination.append(sql);
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
    public String prepareSQL2Engine(String sql) {
        // Usar replace() en lugar de replaceAll() (no evalúa regex)
        String newSql = sql.replace("||", "+");

        // Reemplazo de count(*) por count_big(*) sin importar mayúsculas/minúsculas
        newSql = newSql.replaceAll("(?i)count\\(\\s*\\*\\s*\\)", "count_big(*)");
        Matcher matcher = pattern.matcher(newSql);
        return matcher.replaceAll("CAST($1 as date)");
    }
}
