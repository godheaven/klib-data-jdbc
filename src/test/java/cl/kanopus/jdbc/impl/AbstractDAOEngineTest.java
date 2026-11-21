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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.kanopus.common.data.enums.SortOrder;
import cl.kanopus.jdbc.example.entity.TestComposite;
import cl.kanopus.jdbc.example.entity.TestData;
import cl.kanopus.jdbc.example.entity.TestType;
import cl.kanopus.jdbc.exception.DataException;
import cl.kanopus.jdbc.impl.engine.Engine;
import cl.kanopus.jdbc.util.SQLQueryDynamic;
import cl.kanopus.jdbc.util.SQLQueryDynamic.Condition;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Verifies, without a database, the SQL sent to the driver for the engines other than PostgreSQL.
 */
class AbstractDAOEngineTest {

    static class EngineDAO<T extends cl.kanopus.jdbc.entity.Mapping, I> extends AbstractDAO<T, I> {

        private final NamedParameterJdbcTemplate template;
        private final Engine engine;

        EngineDAO(NamedParameterJdbcTemplate template, Engine engine) {
            this.template = template;
            this.engine = engine;
        }

        @Override
        protected NamedParameterJdbcTemplate getJdbcTemplate() {
            return template;
        }

        @Override
        protected Engine getEngine() {
            return engine;
        }
    }

    static class DataDAO extends EngineDAO<TestData, Long> {
        DataDAO(NamedParameterJdbcTemplate template, Engine engine) {
            super(template, engine);
        }
    }

    static class CompositeEngineDAO extends EngineDAO<TestComposite, Object> {
        CompositeEngineDAO(NamedParameterJdbcTemplate template, Engine engine) {
            super(template, engine);
        }
    }

    private final NamedParameterJdbcTemplate template = mock(NamedParameterJdbcTemplate.class);

    @Test
    void sqlServerExistsAndDeleteAreTranslated() {
        DataDAO dao = new DataDAO(template, Engine.SQLSERVER);
        when(template.queryForObject(anyString(), anyMap(), eq(Long.class))).thenReturn(1L);
        SQLQueryDynamic query = new SQLQueryDynamic(TestData.class);
        query.addConditionLike("loginId", "abc");
        assertTrue(dao.exists(query));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(template).queryForObject(sql.capture(), anyMap(), eq(Long.class));
        assertEquals("SELECT count_big(*) FROM tmp_test_data t1 WHERE UPPER(t1.td_login_id) LIKE '%'+:t1.td_login_id_0+'%'", sql.getValue());

        when(template.update(anyString(), anyMap())).thenReturn(3);
        assertEquals(3, dao.delete(query));
        verify(template).update(sql.capture(), anyMap());
        assertEquals("DELETE t1 FROM tmp_test_data t1 WHERE UPPER(t1.td_login_id) LIKE '%'+:t1.td_login_id_0+'%'", sql.getValue());
    }

    @Test
    void existsReturnsFalseWhenCountIsNull() {
        DataDAO dao = new DataDAO(template, Engine.POSTGRES);
        SQLQueryDynamic query = new SQLQueryDynamic(TestData.class);
        query.addCondition("id", 1L, Condition.EQUAL);
        assertFalse(dao.exists(query));
    }

    @Test
    @SuppressWarnings("unchecked")
    void sqlServerPaginationOnAllPaths() {
        DataDAO dao = new DataDAO(template, Engine.SQLSERVER);
        when(template.query(anyString(), anyMap(), any(RowMapper.class))).thenReturn(List.of());
        when(template.queryForList(anyString(), anyMap())).thenReturn(List.of());
        when(template.queryForList(anyString(), anyMap(), eq(String.class))).thenReturn(List.of());
        when(template.query(anyString(), any(RowMapper.class))).thenReturn(List.of());

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);

        dao.find("SELECT a FROM t WHERE n LIKE '%'||:p", new HashMap<>(), TestType.class, 10, 20);
        verify(template).query(sql.capture(), anyMap(), any(RowMapper.class));
        assertEquals("SELECT a FROM t WHERE n LIKE '%'+:p ORDER BY (SELECT NULL) offset 20 rows fetch next 10 rows only ", sql.getValue());

        dao.findMaps("SELECT a FROM t ORDER BY a", new HashMap<>(), 5, 0);
        verify(template).queryForList(sql.capture(), anyMap());
        assertEquals("SELECT a FROM t ORDER BY a offset 0 rows fetch next 5 rows only ", sql.getValue());

        dao.findStrings("SELECT a FROM t", new HashMap<>(), 5, 0);
        verify(template).queryForList(sql.capture(), anyMap(), eq(String.class));
        assertTrue(sql.getValue().contains("ORDER BY (SELECT NULL)"));

        dao.findTop(3, SortOrder.DESCENDING);
        verify(template).query(sql.capture(), any(RowMapper.class));
        assertTrue(sql.getValue().endsWith("FROM tmp_test_data t1 ORDER BY t1.pk_test_data DESC offset 0 rows fetch next 3 rows only "), sql.getValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findTopWithCompositeKeys() {
        CompositeEngineDAO dao = new CompositeEngineDAO(template, Engine.POSTGRES);
        when(template.query(anyString(), any(RowMapper.class))).thenReturn(List.of());
        dao.findTop(5, SortOrder.DESCENDING);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(template).query(sql.capture(), any(RowMapper.class));
        assertEquals("SELECT t1.pk_group, t1.pk_code, t1.tc_amount, t1.tc_level AS tc_level_alias FROM tmp_test_composite t1 ORDER BY t1.pk_group DESC, t1.pk_code DESC limit 5", sql.getValue());
    }

    @Test
    void existsByIdWithCompositeKeys() {
        CompositeEngineDAO dao = new CompositeEngineDAO(template, Engine.ORACLE);
        when(template.queryForObject(anyString(), anyMap(), eq(Long.class))).thenReturn(1L);
        assertTrue(dao.existsById(new Object[]{1, "A"}));
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, ?>> params = ArgumentCaptor.forClass(Map.class);
        verify(template).queryForObject(sql.capture(), params.capture(), eq(Long.class));
        assertEquals("SELECT count(*) FROM tmp_test_composite WHERE pk_group=:pk_group AND pk_code=:pk_code", sql.getValue());
        assertEquals(1, params.getValue().get("pk_group"));
        assertEquals("A", params.getValue().get("pk_code"));
    }

    @ParameterizedTest
    @EnumSource(value = Engine.class, names = {"SQLSERVER", "ORACLE", "DB2"})
    @SuppressWarnings("unchecked")
    void jsonIsBoundAsStringForNonPostgresEngines(Engine engine) {
        DataDAO dao = new DataDAO(template, engine);
        TestData.JsonData json = new TestData.JsonData();
        json.setText("x");
        TestData data = new TestData();
        data.setId(1);
        data.setLoginId("a");
        data.setJson(json);
        dao.persist(data);

        ArgumentCaptor<Map<String, ?>> params = ArgumentCaptor.forClass(Map.class);
        verify(template).update(anyString(), params.capture());
        assertInstanceOf(String.class, params.getValue().get("td_data_json"));
        assertTrue(((String) params.getValue().get("td_data_json")).contains("\"text\":\"x\""));
        assertTrue(params.getValue().containsKey("td_list_json"));
        assertEquals(null, params.getValue().get("td_list_json"));
    }

    @Test
    void proceduresRequireAJdbcTemplate() {
        NamedParameterJdbcTemplate custom = new NamedParameterJdbcTemplate(mock(JdbcOperations.class));
        RegressionLikeDAO dao = new RegressionLikeDAO(custom);
        assertThrows(DataException.class, () -> dao.call());
    }

    @Test
    void engineIsRequired() {
        DataDAO dao = new DataDAO(template, null);
        assertThrows(DataException.class, dao::generateID);
    }

    @Test
    void generateIdUsesTheSequenceOfTheEngine() {
        DataDAO dao = new DataDAO(template, Engine.SQLSERVER);
        when(template.queryForObject(anyString(), org.mockito.ArgumentMatchers.<Map<String, ?>>any(), eq(Long.class))).thenReturn(10L);
        assertEquals(10L, dao.generateID());
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(template).queryForObject(sql.capture(), org.mockito.ArgumentMatchers.<Map<String, ?>>any(), eq(Long.class));
        assertEquals("SELECT NEXT VALUE FOR tmp_test_data_pk_test_data_seq", sql.getValue());
    }

    static class RegressionLikeDAO extends EngineDAO<TestData, Long> {
        RegressionLikeDAO(NamedParameterJdbcTemplate template) {
            super(template, Engine.POSTGRES);
        }

        void call() {
            executeProcedure("p", new SqlParameter[0], new MapSqlParameterSource());
        }
    }
}
