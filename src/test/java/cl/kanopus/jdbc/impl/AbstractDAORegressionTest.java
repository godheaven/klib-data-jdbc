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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import cl.kanopus.common.data.Paginator;
import cl.kanopus.common.data.enums.SortOrder;
import cl.kanopus.jdbc.config.TestDataSourceConfig;
import cl.kanopus.jdbc.example.entity.TestComposite;
import cl.kanopus.jdbc.example.entity.TestData;
import cl.kanopus.jdbc.example.entity.TestDataHistory;
import cl.kanopus.jdbc.example.entity.TestDataHistoryInherited;
import cl.kanopus.jdbc.example.entity.TestType;
import cl.kanopus.jdbc.example.entity.enums.Color;
import cl.kanopus.jdbc.example.entity.enums.Level;
import cl.kanopus.jdbc.example.entity.enums.Status;
import cl.kanopus.jdbc.exception.DataException;
import cl.kanopus.jdbc.util.SQLQueryDynamic;
import cl.kanopus.jdbc.util.SQLQueryDynamic.Condition;
import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Regression tests of {@link AbstractDAO} executed against PostgreSQL (Testcontainers).
 */
@ContextConfiguration(classes = {TestDataSourceConfig.class})
@ExtendWith(SpringExtension.class)
class AbstractDAORegressionTest {

    @Autowired
    private RegressionDAO dao;

    @Autowired
    private CompositeDAO compositeDAO;

    @Autowired
    private DAOTestImpl daoTest;

    private long countTestData() {
        return daoTest.queryForLong("SELECT count(*) FROM tmp_test_data", new HashMap<>());
    }

    @Test
    void deleteWithoutConditionsIsRejectedAndNothingIsDeleted() {
        long before = countTestData();
        assertThrows(DataException.class, () -> dao.deleteQuery(new SQLQueryDynamic(TestData.class)));

        SQLQueryDynamic notApplied = new SQLQueryDynamic(TestData.class);
        notApplied.addCondition("id", 0L, Condition.EQUAL);
        notApplied.addConditionLike("loginId", "");
        assertThrows(DataException.class, () -> dao.deleteQuery(notApplied));
        assertEquals(before, countTestData());
    }

    @Test
    void deleteWithConditions() {
        HashMap<String, Object> params = new HashMap<>();
        params.put("info", "to-delete");
        dao.execute("INSERT INTO tmp_test_data_history(fk_test_data, info) VALUES (3, :info)", params);
        dao.execute("INSERT INTO tmp_test_data_history(fk_test_data, info) VALUES (3, :info)", params);

        SQLQueryDynamic query = new SQLQueryDynamic(TestDataHistory.class);
        query.addCondition("info", "to-delete", Condition.EQUAL);
        assertTrue(dao.existsQuery(query));
        assertEquals(2, dao.deleteQuery(query));
        assertFalse(dao.existsQuery(query));
    }

    @Test
    void injectionAttemptsAreRejectedOrBound() {
        SQLQueryDynamic order = new SQLQueryDynamic(TestData.class);
        assertThrows(IllegalArgumentException.class, () -> order.setOrderBy("pk_test_data; DROP TABLE tmp_test_type"));
        assertThrows(IllegalArgumentException.class, () -> order.addCondition("1=1 OR pk_test_data", 1, Condition.EQUAL));

        SQLQueryDynamic attack = new SQLQueryDynamic(TestData.class);
        attack.addConditionJson("td_data_json", "->> 'text' = '${value}'", "x' OR '1'='1");
        assertTrue(dao.query(attack).isEmpty());

        SQLQueryDynamic json = new SQLQueryDynamic(TestData.class);
        json.addConditionJson("td_data_json", "->> 'text' = '${value}'", "one");
        List<TestData> records = dao.query(json);
        assertEquals(1, records.size());
        assertEquals("one", records.get(0).getJson().getText());

        SQLQueryDynamic jsonContains = new SQLQueryDynamic(TestData.class);
        jsonContains.addConditionJson("td_data_json", "@> '{\"id\": ${value}}'::jsonb", 1);
        assertEquals(1, dao.query(jsonContains).size());

        // the table still exists
        assertEquals(2, daoTest.queryForLong("SELECT count(*) FROM tmp_test_type", new HashMap<>()));
    }

    @Test
    void existsAndExistsById() {
        SQLQueryDynamic query = new SQLQueryDynamic(TestData.class);
        query.addCondition("loginId", "1234567890", Condition.EQUAL);
        assertTrue(dao.existsQuery(query));

        SQLQueryDynamic none = new SQLQueryDynamic(TestData.class);
        none.addCondition("loginId", "does-not-exist", Condition.EQUAL);
        assertFalse(dao.existsQuery(none));

        assertTrue(dao.existsById(1L));
        assertFalse(dao.existsById(987654L));
    }

    @Test
    void compositeKeys() {
        assertTrue(compositeDAO.existsById(new Object[]{1, "A"}));
        assertFalse(compositeDAO.existsById(new Object[]{1, "Z"}));
        assertThrows(DataException.class, () -> compositeDAO.existsById(new Object[]{1}));

        TestComposite entity = compositeDAO.getById(new Object[]{1, "A"});
        assertNotNull(entity);
        assertEquals(0, new BigDecimal("10.50").compareTo(entity.getAmount()));
        // enum with numeric-looking String ids, read through the column alias
        assertEquals(Level.LOW, entity.getLevel());

        List<TestComposite> top = compositeDAO.findTop(2, SortOrder.DESCENDING);
        assertEquals(2, top.size());
        assertEquals(2, top.get(0).getGroup());
        assertEquals("A", top.get(0).getCode());
        assertEquals(1, top.get(1).getGroup());
        assertEquals("B", top.get(1).getCode());
        assertEquals(Level.HIGH, top.get(1).getLevel());

        List<TestComposite> asc = compositeDAO.findTop(1, SortOrder.ASCENDING);
        assertEquals("A", asc.get(0).getCode());
        assertEquals(1, asc.get(0).getGroup());

        TestComposite created = new TestComposite();
        created.setGroup(9);
        created.setCode("NEW");
        created.setAmount(new BigDecimal("1.25"));
        created.setLevel(Level.HIGH);
        compositeDAO.persist(created);
        assertEquals(Level.HIGH, compositeDAO.getById(new Object[]{9, "NEW"}).getLevel());
        created.setAmount(new BigDecimal("2.50"));
        compositeDAO.update(created);
        assertEquals(0, new BigDecimal("2.50").compareTo(compositeDAO.getById(new Object[]{9, "NEW"}).getAmount()));
        assertEquals(1, compositeDAO.deleteById(new Object[]{9, "NEW"}));
        assertThrows(DataException.class, () -> compositeDAO.deleteById(new Object[]{9}));
    }

    @Test
    void findTopUsesKeyOrder() {
        List<TestData> top = dao.findTop(2, SortOrder.DESCENDING);
        assertEquals(2, top.size());
        assertTrue(top.get(0).getId() > top.get(1).getId());
    }

    @Test
    void findPaginatorUsesTheGivenClass() {
        SQLQueryDynamic query = new SQLQueryDynamic("SELECT pk_test_type, name AS tt_name FROM tmp_test_type");
        query.setOrderBy("pk_test_type");
        query.setLimit(1);
        Paginator<TestType> paginator = dao.paginator(query, TestType.class);
        assertEquals(1, paginator.getRecords().size());
        assertEquals(2, paginator.getTotalRecords());
        assertEquals("ONE", paginator.getRecords().get(0).getName());
    }

    @Test
    void findMapsAndRawPagination() {
        SQLQueryDynamic query = new SQLQueryDynamic(TestData.class);
        query.addCondition("systemId", 0L, Condition.GREATER_THAN, true);
        query.setOrderBy("id");
        query.setLimit(2);
        Paginator<Map<String, Object>> maps = dao.maps(query);
        assertEquals(2, maps.getRecords().size());
        assertTrue(maps.getTotalRecords() >= 3);

        List<Map<String, Object>> raw = dao.maps("SELECT pk_test_type FROM tmp_test_type ORDER BY pk_test_type", new HashMap<>(), 1, 1);
        assertEquals(1, raw.size());
        assertEquals(2, ((Number) raw.get(0).get("pk_test_type")).intValue());

        List<String> strings = dao.strings("SELECT name FROM tmp_test_type ORDER BY name", new HashMap<>(), 5, 0);
        assertEquals(List.of("ONE", "TWO"), strings);

        SQLQueryDynamic one = new SQLQueryDynamic(TestData.class);
        one.addCondition("id", 1L, Condition.EQUAL);
        assertEquals(1L, dao.one(one).getId());
    }

    @Test
    void superclassFieldsAndDefaultOrderBy() {
        List<TestDataHistoryInherited> records = dao.findAll(TestDataHistoryInherited.class);
        assertTrue(records.size() >= 3);
        for (int i = 1; i < records.size(); i++) {
            assertTrue(records.get(i - 1).getId() > records.get(i).getId(), "defaultOrderBy DESC");
        }
        records.forEach(r -> assertNotNull(r.getInfo()));
    }

    @Test
    void persistJsonWithUtf8AndTruncatedText() {
        TestType type = new TestType();
        type.setId(1);
        TestData.JsonData json = new TestData.JsonData();
        json.setId(77);
        json.setText("ñandú €");
        TestData.TestDataGroup group = new TestData.TestDataGroup();
        group.setText("utf8");

        TestData entity = new TestData();
        entity.setId(dao.generateID());
        entity.setLoginId("abcdefghijKLMNOP");
        entity.setSystemId(9);
        entity.setDate(new Date());
        entity.setLocalDate(LocalDate.now());
        entity.setLocalDateTime(LocalDateTime.now());
        entity.setStatus(Status.ERROR);
        entity.setColor(Color.RED);
        entity.setType(type);
        entity.setGroup(group);
        entity.setJson(json);
        dao.persist(entity);

        TestData loaded = dao.getById(entity.getId(), true);
        assertEquals("ñandú €", loaded.getJson().getText());
        assertEquals("abcdefghij", loaded.getLoginId());
        assertEquals(Color.RED, loaded.getColor());
        assertEquals("ONE", loaded.getType().getName());
        assertTrue(dao.findById(entity.getId()).isPresent());
        assertEquals(1, dao.deleteById(entity.getId()));
    }

    @Test
    void storedProcedures() {
        SqlParameter[] parameters = {new SqlParameter("a", Types.INTEGER), new SqlParameter("b", Types.INTEGER), new SqlOutParameter("total", Types.INTEGER)};
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("a", 2).addValue("b", 3);
        Map<String, Object> out = dao.procedureOut("tmp_test_sum", parameters, params);
        assertEquals(5, ((Number) out.get("total")).intValue());
        assertEquals(5, dao.function("tmp_test_sum", parameters, params));

        // the output is not a list of the requested type: an empty list is returned instead of failing
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("a", 1);
        inputs.put("b", 1);
        assertTrue(dao.procedureList("tmp_test_sum", parameters, inputs).isEmpty());
    }
}
