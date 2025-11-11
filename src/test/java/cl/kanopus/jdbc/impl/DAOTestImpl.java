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

import cl.kanopus.jdbc.entity.Mapping;
import cl.kanopus.jdbc.example.entity.TestData;
import cl.kanopus.jdbc.impl.engine.Engine;
import cl.kanopus.jdbc.util.SQLQueryDynamic;
import java.util.HashMap;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class DAOTestImpl extends AbstractDAO<TestData, Long> implements DAOTest {

    @Autowired
    @Qualifier("jdbcTemplateTest")
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    protected NamedParameterJdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    @Override
    protected Engine getEngine() {
        return Engine.POSTGRES;
    }

    @Override
    public Integer queryForInteger(String sql, HashMap<String, ?> params) {
        return super.queryForInteger(sql, params);
    }

    @Override
    public Long queryForLong(String sql, HashMap<String, ?> params) {
        return super.queryForLong(sql, params);
    }

    @Override
    public String queryForString(String sql, HashMap<String, ?> params) {
        return super.queryForString(sql, params);
    }

    @Override
    public List<TestData> find(String sql, HashMap<String, ?> params) {
        return super.find(sql, params);
    }

    @Override
    public <I extends Mapping> List<I> find(String sql, HashMap<String, ?> params, Class<I> clazz) {
        return super.find(sql, params, clazz);
    }

    @Override
    public <I extends Mapping> List<I> find(String sql, HashMap<String, ?> params, Class<I> clazz, int limit, int offset) {
        return super.find(sql, params, clazz, limit, offset);
    }

    @Override
    public List<String> findStrings(String sql, HashMap<String, ?> params) {
        return super.findStrings(sql, params);
    }

    @Override
    public List<Long> findLongs(String sql, HashMap<String, ?> params) {
        return super.findLongs(sql, params);
    }

    @Override
    public List<TestData> find(SQLQueryDynamic sqlQuery) {
        return super.find(sqlQuery);
    }

    @Override
    public List findAll(Class clazz) {
        return super.findAll(clazz);
    }

}
