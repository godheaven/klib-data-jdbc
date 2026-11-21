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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cl.kanopus.jdbc.entity.enums.JoinOperator;
import cl.kanopus.jdbc.example.entity.TestData;
import cl.kanopus.jdbc.example.entity.TestDataEmpty;
import cl.kanopus.jdbc.example.entity.TestDataHistory;
import cl.kanopus.jdbc.example.entity.TestDataHistoryInherited;
import cl.kanopus.jdbc.example.entity.enums.Color;
import cl.kanopus.jdbc.example.entity.enums.Level;
import cl.kanopus.jdbc.example.entity.enums.Status;
import cl.kanopus.jdbc.util.parser.EnumParser;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the thread safety and isolation of {@link JdbcCache} and its type conversions.
 */
class JdbcCacheConcurrencyTest {

    private static final String BASE_SQL_EMPTY = "SELECT t1.pk_test_data_empty FROM tmp_test_data_empty t1";

    @Test
    void joinsDoNotLeakIntoTheCache() {
        SQLQueryDynamic first = new SQLQueryDynamic(TestDataEmpty.class);
        first.addJoinTable(JoinOperator.INNER_JOIN, "jx", TestDataHistory.class, "testDataId");
        first.addCondition("jx.info", "a", SQLQueryDynamic.Condition.EQUAL);
        assertTrue(first.getSQL().contains("INNER JOIN tmp_test_data_history jx"));

        // a later query of the same class must not see the alias or the properties of the join
        SQLQueryDynamic second = new SQLQueryDynamic(TestDataEmpty.class);
        assertEquals(BASE_SQL_EMPTY, second.getSQL());
        assertFalse(JdbcCache.translationMap(TestDataEmpty.class).containsKey("jx.info"));
        assertFalse(JdbcCache.sqlBase(TestDataEmpty.class).getAliasMap().containsKey("tmp_test_data_history"));

        // the history table is not aliased in the second query: the raw name is kept
        second.addCondition("tmp_test_data_history.info", "b", SQLQueryDynamic.Condition.EQUAL);
        assertTrue(second.getSQL().endsWith("WHERE UPPER(tmp_test_data_history.info)=:tmp_test_data_history.info_0"), second.getSQL());
    }

    @Test
    void cachedMapsAreImmutable() {
        Map<String, String> translation = JdbcCache.translationMap(TestData.class);
        assertThrows(UnsupportedOperationException.class, () -> translation.put("x", "y"));
        Map<String, String> alias = JdbcCache.sqlBase(TestData.class).getAliasMap();
        assertThrows(UnsupportedOperationException.class, () -> alias.put("x", "y"));
        assertThrows(UnsupportedOperationException.class, () -> JdbcCache.mappedFields(TestData.class).clear());
    }

    @Test
    void concurrentQueriesAreIsolated() throws Exception {
        int threads = 16;
        int iterations = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<List<String>>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                final String alias = "jx" + t;
                futures.add(pool.submit(() -> {
                    start.await();
                    List<String> sqls = new ArrayList<>();
                    for (int i = 0; i < iterations; i++) {
                        SQLQueryDynamic query = new SQLQueryDynamic(TestDataEmpty.class);
                        query.addJoinTable(JoinOperator.LEFT_JOIN, alias, TestDataHistory.class, "testDataId");
                        query.addCondition(alias + ".info", "v", SQLQueryDynamic.Condition.EQUAL);
                        sqls.add(query.getSQL());
                        // cache entries are always the same instance
                        assertSame(JdbcCache.sqlBase(TestData.class, true), JdbcCache.sqlBase(TestData.class, true));
                        assertSame(JdbcCache.rowMapper(TestData.class, false), JdbcCache.rowMapper(TestData.class, false));
                    }
                    return sqls;
                }));
            }
            start.countDown();
            for (int t = 0; t < threads; t++) {
                String expected = BASE_SQL_EMPTY + " LEFT JOIN tmp_test_data_history jx" + t + " ON t1.pk_test_data_empty=jx" + t + ".fk_test_data WHERE UPPER(jx" + t + ".info)=:jx" + t + ".info_0";
                for (String sql : futures.get(t).get(60, TimeUnit.SECONDS)) {
                    assertEquals(expected, sql);
                }
            }
        } finally {
            pool.shutdownNow();
        }
        assertEquals(BASE_SQL_EMPTY, new SQLQueryDynamic(TestDataEmpty.class).getSQL());
        assertEquals(1, JdbcCache.translationMap(TestDataEmpty.class).size());
    }

    @Test
    void superclassFieldsAreMappedUnlessTheSuperclassIsAnEntity() {
        Map<String, String> translation = JdbcCache.translationMap(TestDataHistoryInherited.class);
        assertEquals("tmp_test_data_history.info", translation.get("info"));
        assertEquals("tmp_test_data_history.pk_test_data_history", translation.get("id"));
        assertEquals("SELECT t1.pk_test_data_history, t1.fk_test_data, t1.info FROM tmp_test_data_history t1", JdbcCache.sqlBase(TestDataHistoryInherited.class).getSql());

        // TestDataEmpty extends TestViewData, which is annotated with @View: its fields are not inherited
        assertEquals(1, JdbcCache.mappedFields(TestDataEmpty.class).size());
    }

    @Test
    void convertNumbers() {
        assertEquals(5L, JdbcCache.convert(new BigDecimal("5"), Long.class));
        assertEquals(5L, JdbcCache.convert(5, long.class));
        assertEquals(7, JdbcCache.convert(new BigDecimal("7"), Integer.class));
        assertEquals(7, JdbcCache.convert(7L, int.class));
        assertEquals((short) 2, JdbcCache.convert(2, Short.class));
        assertEquals((byte) 2, JdbcCache.convert(2, byte.class));
        assertEquals(1.5d, JdbcCache.convert(new BigDecimal("1.5"), Double.class));
        assertEquals(1.5f, JdbcCache.convert(1.5d, float.class));
        assertEquals(new BigDecimal("12"), JdbcCache.convert(12, BigDecimal.class));
        assertEquals(new BigDecimal("12"), JdbcCache.convert(BigInteger.valueOf(12), BigDecimal.class));
        assertEquals(BigInteger.valueOf(12), JdbcCache.convert(new BigDecimal("12.0"), BigInteger.class));
        assertEquals(BigInteger.valueOf(12), JdbcCache.convert(12, BigInteger.class));
        assertEquals(Boolean.TRUE, JdbcCache.convert(new BigDecimal("1"), Boolean.class));
        assertEquals(Boolean.FALSE, JdbcCache.convert(0, boolean.class));
        assertEquals(3, JdbcCache.convert(3, Object.class));
        // unknown combinations are returned unchanged
        assertEquals(3, JdbcCache.convert(3, String.class));
        assertNull(JdbcCache.convert(null, Long.class));
    }

    @Test
    void convertDates() {
        LocalDateTime ldt = LocalDateTime.of(2025, 3, 4, 5, 6, 7);
        Timestamp ts = Timestamp.valueOf(ldt);
        assertEquals(ldt, JdbcCache.convert(ts, LocalDateTime.class));
        assertEquals(ldt.toLocalDate(), JdbcCache.convert(ts, LocalDate.class));
        assertEquals(ts.toInstant(), JdbcCache.convert(ts, Instant.class));
        assertEquals(ts.toInstant(), ((OffsetDateTime) JdbcCache.convert(ts, OffsetDateTime.class)).toInstant());
        assertEquals(ts.toInstant(), ((ZonedDateTime) JdbcCache.convert(ts, ZonedDateTime.class)).toInstant());
        assertSame(ts, JdbcCache.convert(ts, Date.class));

        java.sql.Date date = java.sql.Date.valueOf(LocalDate.of(2025, 3, 4));
        assertEquals(LocalDate.of(2025, 3, 4), JdbcCache.convert(date, LocalDate.class));
        assertEquals(LocalDate.of(2025, 3, 4).atStartOfDay(), JdbcCache.convert(date, LocalDateTime.class));

        OffsetDateTime odt = OffsetDateTime.of(ldt, ZoneOffset.UTC);
        assertEquals(odt.toInstant(), JdbcCache.convert(odt, Instant.class));
        assertEquals(odt.toInstant(), ((Date) JdbcCache.convert(odt, Date.class)).toInstant());
        assertEquals(odt.toInstant(), ((ZonedDateTime) JdbcCache.convert(odt, ZonedDateTime.class)).toInstant());
        assertTrue(JdbcCache.convert(odt, LocalDateTime.class) instanceof LocalDateTime);
        assertTrue(JdbcCache.convert(odt, LocalDate.class) instanceof LocalDate);

        assertEquals(ts, JdbcCache.convert(ldt, Date.class));
        assertEquals(ldt.toLocalDate(), JdbcCache.convert(ldt, LocalDate.class));
        assertEquals(date, JdbcCache.convert(LocalDate.of(2025, 3, 4), Date.class));
    }

    @Test
    void convertOtherTypes() {
        UUID uuid = UUID.randomUUID();
        assertEquals(uuid, JdbcCache.convert(uuid.toString(), UUID.class));
        assertEquals(uuid.toString(), JdbcCache.convert(uuid, String.class));
        assertEquals("c", JdbcCache.convert('c', String.class));

        byte[] utf8 = "ñandú €".getBytes(StandardCharsets.UTF_8);
        assertEquals(StandardCharsets.UTF_8, JdbcCache.charset());
        assertEquals("ñandú €", JdbcCache.convert(utf8, StringWriter.class).toString());
    }

    @Test
    void enumParserAcceptsNumericLookingStringIds() {
        assertEquals(Level.LOW, EnumParser.parseValue(Level.class, "10"));
        assertEquals(Level.HIGH, EnumParser.parseValue(Level.class, 20));
        assertEquals(Color.BLACK, EnumParser.parseValue(Color.class, 2));
        assertEquals(Color.BLACK, EnumParser.parseValue(Color.class, 2L));
        assertEquals(Color.BLACK, EnumParser.parseValue(Color.class, new BigDecimal("2")));
        assertEquals(Color.RED, EnumParser.parseValue(Color.class, "1"));
        assertEquals(Status.SUCCESS, EnumParser.parseValue(Status.class, "SUCCESS "));
        assertNull(EnumParser.parseValue(Color.class, "x"));
        assertNull(EnumParser.parseValue(Color.class, null));
        assertNull(EnumParser.parseValue(Status.class, "UNKNOWN"));
        assertNull(EnumParser.parseValue(SimpleEnum.class, null));
        assertEquals(SimpleEnum.B, EnumParser.parseValue(SimpleEnum.class, "B"));
    }

    enum SimpleEnum {
        A,
        B
    }
}
