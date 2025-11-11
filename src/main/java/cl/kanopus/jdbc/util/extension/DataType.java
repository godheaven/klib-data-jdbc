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
package cl.kanopus.jdbc.util.extension;

import cl.kanopus.common.enums.EnumIdentifiable;
import cl.kanopus.jdbc.util.SQLQueryDynamic.MatchMode;

public enum DataType implements EnumIdentifiable<String> {
    ALPHANUMERIC(
            MatchMode.EQUAL,
            MatchMode.NOT_EQUAL,
            MatchMode.TEXT_CONTAINS,
            MatchMode.TEXT_STARTS_WITH,
            MatchMode.TEXT_ENDS_WITH,
            MatchMode.IN
    ),
    NUMERIC(
            MatchMode.EQUAL,
            MatchMode.LESS_THAN,
            MatchMode.LESS_OR_EQUAL,
            MatchMode.GREATER_THAN,
            MatchMode.GREATER_OR_EQUAL,
            MatchMode.NOT_EQUAL,
            MatchMode.IN,
            MatchMode.BETWEEN
    ),
    DATE(
            MatchMode.EQUAL,
            MatchMode.LESS_THAN,
            MatchMode.LESS_OR_EQUAL,
            MatchMode.GREATER_THAN,
            MatchMode.GREATER_OR_EQUAL,
            MatchMode.NOT_EQUAL,
            MatchMode.BETWEEN
    );

    private final MatchMode[] matchModes;

    DataType(MatchMode... matchModes) {
        this.matchModes = matchModes;
    }

    @Override
    public String getId() {
        return this.name();
    }

    public MatchMode[] getMatchModes() {
        return matchModes;
    }

}
