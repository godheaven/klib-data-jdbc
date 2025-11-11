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
package cl.kanopus.jdbc.example;

import cl.kanopus.jdbc.entity.Mapping;
import cl.kanopus.jdbc.impl.AbstractDAO;
import cl.kanopus.jdbc.impl.engine.Engine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * This abstract class defines methods for data access that are common,
 * generally, all kinds of data access DAO must implement this class.Thus it is
 * given safely access the Connection database.The JdbcTemplate property is kept
 * private and gives access to the database through the methods implemented in
 * this AbstractDAO.
 */
public abstract class AbstractBaseDAO<T extends Mapping, I> extends AbstractDAO<T, I> {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    protected NamedParameterJdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    @Override
    protected Engine getEngine() {
        return Engine.POSTGRES;
    }

}
