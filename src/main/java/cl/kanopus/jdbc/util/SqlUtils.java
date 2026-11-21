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
package cl.kanopus.jdbc.util;

import java.util.Locale;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * Small helpers to inspect and rewrite SQL text without touching string literals or quoted identifiers.
 *
 * @author Pablo Diaz Saavedra
 */
public final class SqlUtils {

    /**
     * Allowed shape for a column identifier received from callers: {@code column}, {@code table.column} or {@code schema.table.column}.
     */
    public static final Pattern IDENTIFIER = Pattern.compile("^[A-Za-z_][A-Za-z0-9_$]*(\\.[A-Za-z_][A-Za-z0-9_$]*){0,2}$");

    private SqlUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Checks that the given text is a plain SQL identifier (optionally qualified).
     *
     * @param identifier
     *            the identifier to validate
     * @return the same identifier
     * @throws IllegalArgumentException
     *             when the identifier is null or contains characters that are not allowed
     */
    public static String requireIdentifier(String identifier) {
        if (identifier == null || !IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException("Invalid SQL identifier: '" + identifier + "'. Only [A-Za-z0-9_$] names, optionally qualified with '.', are allowed");
        }
        return identifier;
    }

    /**
     * Applies {@code transformer} to every fragment of {@code sql} that is outside single-quoted string literals and double-quoted identifiers. Literal content is kept untouched.
     *
     * @param sql
     *            SQL text
     * @param transformer
     *            transformation applied to code fragments
     * @return rewritten SQL
     */
    public static String transformOutsideLiterals(String sql, UnaryOperator<String> transformer) {
        if (sql == null || sql.isEmpty()) {
            return sql;
        }
        StringBuilder result = new StringBuilder(sql.length());
        StringBuilder code = new StringBuilder();
        int i = 0;
        int length = sql.length();
        while (i < length) {
            char c = sql.charAt(i);
            if (c == '\'' || c == '"') {
                result.append(transformer.apply(code.toString()));
                code.setLength(0);
                int end = endOfQuoted(sql, i, c);
                result.append(sql, i, end);
                i = end;
            } else {
                code.append(c);
                i++;
            }
        }
        result.append(transformer.apply(code.toString()));
        return result.toString();
    }

    /**
     * Finds the first occurrence of {@code keyword} (case-insensitive, whole word) at parenthesis depth zero and outside literals.
     *
     * @param sql
     *            SQL text
     * @param keyword
     *            keyword to look for, words separated by a single space (e.g. {@code "ORDER BY"})
     * @return index of the keyword or {@code -1}
     */
    public static int indexOfTopLevel(String sql, String keyword) {
        return indexOfTopLevel(sql, keyword, 0);
    }

    /**
     * Same as {@link #indexOfTopLevel(String, String)} starting at {@code fromIndex}. Depth is computed from the beginning of the text.
     *
     * @param sql
     *            SQL text
     * @param keyword
     *            keyword to look for
     * @param fromIndex
     *            first index to consider
     * @return index of the keyword or {@code -1}
     */
    public static int indexOfTopLevel(String sql, String keyword, int fromIndex) {
        if (sql == null || keyword == null || keyword.isEmpty()) {
            return -1;
        }
        String[] words = keyword.trim().toUpperCase(Locale.ROOT).split("\\s+");
        int depth = 0;
        int i = 0;
        int length = sql.length();
        while (i < length) {
            char c = sql.charAt(i);
            if (c == '\'' || c == '"') {
                i = endOfQuoted(sql, i, c);
                continue;
            }
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth = Math.max(0, depth - 1);
            } else if (depth == 0 && i >= fromIndex && isWordStart(sql, i)) {
                int end = matchWords(sql, i, words);
                if (end > 0) {
                    return i;
                }
            }
            i++;
        }
        return -1;
    }

    private static boolean isWordStart(String sql, int i) {
        return i == 0 || !isWordChar(sql.charAt(i - 1));
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$' || c == '.';
    }

    private static int matchWords(String sql, int start, String[] words) {
        int pos = start;
        for (int w = 0; w < words.length; w++) {
            if (w > 0) {
                int ws = pos;
                while (pos < sql.length() && Character.isWhitespace(sql.charAt(pos))) {
                    pos++;
                }
                if (pos == ws) {
                    return -1;
                }
            }
            String word = words[w];
            if (!sql.regionMatches(true, pos, word, 0, word.length())) {
                return -1;
            }
            pos += word.length();
        }
        if (pos < sql.length() && isWordChar(sql.charAt(pos))) {
            return -1;
        }
        return pos;
    }

    private static int endOfQuoted(String sql, int start, char quote) {
        int i = start + 1;
        while (i < sql.length()) {
            if (sql.charAt(i) == quote) {
                if (i + 1 < sql.length() && sql.charAt(i + 1) == quote) {
                    i += 2; // escaped quote
                    continue;
                }
                return i + 1;
            }
            i++;
        }
        return sql.length();
    }
}
