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

import cl.kanopus.jdbc.DAOInterface;
import cl.kanopus.jdbc.entity.Mapping;
import cl.kanopus.jdbc.example.entity.TestData;
import cl.kanopus.jdbc.util.SQLQueryDynamic;
import java.util.HashMap;
import java.util.List;

public interface DAOTest extends DAOInterface<TestData, Long> {

    Integer queryForInteger(String sql, HashMap<String, ?> params);

    Long queryForLong(String sql, HashMap<String, ?> params);

    String queryForString(String sql, HashMap<String, ?> params);

    List<TestData> find(String sql, HashMap<String, ?> params);

    <I extends Mapping> List<I> find(String sql, HashMap<String, ?> params, Class<I> clazz);

    <I extends Mapping> List<I> find(String sql, HashMap<String, ?> params, Class<I> clazz, int limit, int offset);

    List<String> findStrings(String sql, HashMap<String, ?> params);

    List<Long> findLongs(String sql, HashMap<String, ?> params);

    List<?> find(SQLQueryDynamic sqlQuery);

    List findAll(Class clazz);

}
