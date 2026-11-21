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
import cl.kanopus.jdbc.entity.Mapping;
import cl.kanopus.jdbc.example.AbstractBaseDAO;
import cl.kanopus.jdbc.example.entity.TestData;
import cl.kanopus.jdbc.util.SQLQueryDynamic;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

/**
 * Exposes protected methods of {@link AbstractDAO} for the regression tests.
 */
@Repository
public class RegressionDAO extends AbstractBaseDAO<TestData, Long> {

    public int deleteQuery(SQLQueryDynamic query) {
        return delete(query);
    }

    public boolean existsQuery(SQLQueryDynamic query) {
        return exists(query);
    }

    public <I extends Mapping> Paginator<I> paginator(SQLQueryDynamic query, Class<I> clazz) {
        return findPaginator(query, clazz);
    }

    public Paginator<Map<String, Object>> maps(SQLQueryDynamic query) {
        return findMaps(query);
    }

    public List<Map<String, Object>> maps(String sql, Map<String, ?> params, int limit, int offset) {
        return findMaps(sql, params, limit, offset);
    }

    public List<String> strings(String sql, Map<String, ?> params, int limit, int offset) {
        return findStrings(sql, params, limit, offset);
    }

    public List<TestData> query(SQLQueryDynamic query) {
        return find(query);
    }

    public TestData one(SQLQueryDynamic query) {
        return queryForObject(query);
    }

    public int execute(String sql, HashMap<String, ?> params) {
        return update(sql, params);
    }

    public Integer function(String name, SqlParameter[] parameters, MapSqlParameterSource params) {
        return executeFunction(name, parameters, params);
    }

    public Map<String, Object> procedureOut(String name, SqlParameter[] parameters, MapSqlParameterSource params) {
        return executeProcedureOut(name, parameters, params);
    }

    public List<Integer> procedureList(String name, SqlParameter[] parameters, Map<String, Object> params) {
        return executeProcedure(name, parameters, params, Integer.class);
    }

    private Integer executeFunction(String name, SqlParameter[] parameters, MapSqlParameterSource params) {
        Map<String, Object> out = executeProcedureOut(name, parameters, params);
        Object value = out.values().iterator().next();
        return value == null ? null : ((Number) value).intValue();
    }
}
