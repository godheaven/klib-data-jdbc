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
package cl.kanopus.jdbc.util.parser;

import cl.kanopus.common.enums.EnumIdentifiable;

public abstract class EnumParser {

    private EnumParser() {
        throw new IllegalStateException("Utility class");
    }

    public static <T extends Enum<T> & EnumIdentifiable<S>, S> T parse(Class<T> type, S id) {
        for (T t : type.getEnumConstants()) {
            if (t.getId().equals(id)) {
                return t;
            }
        }
        return null;
    }

    /**
     * Resolves an enum constant from a raw database value, regardless of the Java type used by the driver. Numbers are compared numerically and everything else by its text representation, so a numeric-looking text id (e.g. {@code "10"}) still matches an enum whose id is a String.
     *
     * @param type
     *            enum class implementing {@link EnumIdentifiable}
     * @param raw
     *            value read from the database
     * @return the matching constant or {@code null}
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T extends Enum<T>> T parseValue(Class<T> type, Object raw) {
        if (raw == null || type == null || !type.isEnum()) {
            return null;
        }
        for (T t : type.getEnumConstants()) {
            Object id = (t instanceof EnumIdentifiable identifiable) ? identifiable.getId() : t.name();
            if (matches(id, raw)) {
                return t;
            }
        }
        return null;
    }

    private static boolean matches(Object id, Object raw) {
        if (id == null) {
            return false;
        }
        if (id.equals(raw)) {
            return true;
        }
        if (id instanceof Number n1 && raw instanceof Number n2) {
            return new java.math.BigDecimal(n1.toString()).compareTo(new java.math.BigDecimal(n2.toString())) == 0;
        }
        String rawText = String.valueOf(raw).trim();
        if (id instanceof Number) {
            try {
                return new java.math.BigDecimal(id.toString()).compareTo(new java.math.BigDecimal(rawText)) == 0;
            } catch (NumberFormatException ex) {
                return false;
            }
        }
        return String.valueOf(id).equals(rawText) || String.valueOf(id).equals(String.valueOf(raw));
    }
}
