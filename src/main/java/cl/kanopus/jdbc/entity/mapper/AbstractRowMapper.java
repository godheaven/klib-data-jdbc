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
package cl.kanopus.jdbc.entity.mapper;

import org.springframework.jdbc.core.RowMapper;

/**
 *
 * AbstractRowMapper class which implements RowMapper interface.
 *
 * RowMapper is used to convert the ResultSet into domain specific object. All
 * class of types RowMapper must be implemented this abstract class and override
 * the mapRow() method.
 *
 * @param <T>
 */
public abstract class AbstractRowMapper<T> implements RowMapper<T> {

}
