package cl.kanopus.jdbc.impl;

import cl.kanopus.common.data.enums.SortOrder;
import cl.kanopus.jdbc.example.entity.TestData;
import cl.kanopus.jdbc.example.entity.TestDataEmpty;
import cl.kanopus.jdbc.example.entity.TestDataHistory;
import cl.kanopus.jdbc.example.entity.TestType;
import cl.kanopus.jdbc.example.entity.enums.Color;
import cl.kanopus.jdbc.example.entity.enums.Status;
import cl.kanopus.jdbc.util.SQLQueryDynamic;
import cl.kanopus.jdbc.util.SQLQueryDynamic.Condition;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ContextConfiguration(locations = "classpath:applicationContext.xml")
@ExtendWith(SpringExtension.class)
public class AbstractDAOTest {

    @Autowired
    private DAOTest daoTest;

    @Test
    public void testDeleteByID_long() throws Exception {
        long id = 0L;
        int expResult = 0;
        int result = daoTest.deleteByID(id);
        Assertions.assertEquals(expResult, result);
    }

    @Test
    public void testFindAll() throws Exception {

        List<TestData> records = daoTest.findAll();
        Assertions.assertNotNull(records);
        Assertions.assertTrue(records.size() >= 0);
        for (TestData data : records) {
            Assertions.assertNotNull(data.getId());
            Assertions.assertNotNull(data.getLoginId());
            Assertions.assertNotNull(data.getSystemId());
            Assertions.assertNotNull(data.getStatus());
            Assertions.assertNotNull(data.getDate());
            Assertions.assertNotNull(data.getLocalDate());
            Assertions.assertNotNull(data.getLocalDateTime());
            Assertions.assertNotNull(data.getColor());
            Assertions.assertNotNull(data.getGroup());
            Assertions.assertNotNull(data.getGroup().getText());
            Assertions.assertTrue(data.getStatus() == Status.SUCCESS || data.getStatus() == Status.ERROR);
            Assertions.assertTrue(data.getColor() == Color.BLACK || data.getColor() == Color.RED);
        }

    }

    @Test
    public void testFindAllEmpty() throws Exception {
        List<TestDataEmpty> records = daoTest.findAll(TestDataEmpty.class);
        Assertions.assertNotNull(records);
        Assertions.assertTrue(records.isEmpty());
    }

    @Test
    public void testFindTop() throws Exception {
        int limit = 2;
        SortOrder sortOrder = SortOrder.ASCENDING;

        List<TestData> records = daoTest.findTop(limit, sortOrder);
        Assertions.assertNotNull(records);
        Assertions.assertTrue(records.size() == 2);
        for (TestData data : records) {
            Assertions.assertNotNull(data.getId());
            Assertions.assertNotNull(data.getLoginId());
            Assertions.assertNotNull(data.getSystemId());
            Assertions.assertNotNull(data.getStatus());
            Assertions.assertNotNull(data.getDate());
            Assertions.assertNotNull(data.getLocalDate());
            Assertions.assertNotNull(data.getLocalDateTime());
            Assertions.assertNotNull(data.getColor());
            Assertions.assertNotNull(data.getGroup());
            Assertions.assertNotNull(data.getGroup().getText());
            Assertions.assertTrue(data.getStatus() == Status.SUCCESS || data.getStatus() == Status.ERROR);
            Assertions.assertTrue(data.getColor() == Color.BLACK || data.getColor() == Color.RED);
        }
    }

    @Test
    public void testGenerateID() throws Exception {
        long id = daoTest.generateID();
        Assertions.assertTrue(id > 0);
    }

    @Test
    public void testGetByID() throws Exception {

        long id = 1;
        TestData data = daoTest.getByID(id);
        Assertions.assertNotNull(data);
        Assertions.assertNotNull(data.getId());
        Assertions.assertNotNull(data.getLoginId());
        Assertions.assertNotNull(data.getSystemId());
        Assertions.assertNotNull(data.getStatus());
        Assertions.assertNotNull(data.getDate());
        Assertions.assertNotNull(data.getLocalDate());
        Assertions.assertNotNull(data.getLocalDateTime());
        Assertions.assertNotNull(data.getGroup());
        Assertions.assertNotNull(data.getGroup().getText());
        Assertions.assertEquals(id, data.getId());
        Assertions.assertEquals(Status.SUCCESS, data.getStatus());
        Assertions.assertEquals(Color.BLACK, data.getColor());
    }

    @Test
    public void testQueryForString() {
        String sql = "SELECT td_login_id FROM tmp_test_data WHERE td_status!=:status limit 1";
        HashMap params = new HashMap();
        params.put("status", "NON-EXISTING STATUS");

        String result = daoTest.queryForString(sql, params);

        Assertions.assertNotNull(result);
    }

    @Test
    public void testPersist() throws Exception {
        long id = daoTest.generateID();

        TestData.TestDataGroup group = new TestData.TestDataGroup();
        group.setText("Some text no auditable");

        TestType type = new TestType();
        type.setId(1);

        TestData test = new TestData();
        test.setId(id);
        test.setLoginId("12345678901234567890");
        test.setSystemId(1);
        test.setDate(new Date());
        test.setLocalDate(LocalDate.now());
        test.setLocalDateTime(LocalDateTime.now());
        test.setGroup(group);
        test.setStatus(Status.SUCCESS);
        test.setColor(Color.BLACK);
        test.setType(type);
        daoTest.persist(test);
    }

    @Test
    public void testPersistAutomaticSequence() throws Exception {
        TestData.TestDataGroup group = new TestData.TestDataGroup();
        group.setText("Some text no auditable");

        TestType type = new TestType();
        type.setId(1);

        TestData test = new TestData();
        test.setLoginId("12345678901234567890");
        test.setSystemId(1);
        test.setDate(new Date());
        test.setLocalDate(LocalDate.now());
        test.setLocalDateTime(LocalDateTime.now());
        test.setGroup(group);
        test.setStatus(Status.SUCCESS);
        test.setColor(Color.BLACK);
        test.setType(type);
        daoTest.persist(test);
    }

    @Test
    public void testUpdate() throws Exception {
        long id = 1;

        TestData.TestDataGroup group = new TestData.TestDataGroup();
        group.setText("Some text no auditable 2");

        TestData test = new TestData();
        test.setId(id);
        test.setLoginId("pd47753X");
        test.setDate(new Date());
        test.setLocalDate(LocalDate.now());
        test.setLocalDateTime(LocalDateTime.now());
        test.setSystemId(2);
        test.setStatus(Status.SUCCESS);
        test.setColor(Color.BLACK);
        daoTest.update(test);
    }

    @Test
    public void testQueryForInteger() {
        HashMap<String, String> params = new HashMap<>();
        String sql = "SELECT pk_test_data FROM tmp_test_data WHERE td_status!=:status limit 1";
        params.put("status", "NON-EXISTING STATUS");

        Integer result = daoTest.queryForInteger(sql, params);
        Assertions.assertNotNull(result);
    }

    @Test
    public void testQueryForLong() {
        HashMap<String, String> params = new HashMap<>();
        String sql = "SELECT pk_test_data FROM tmp_test_data WHERE td_status!=:status limit 1";
        params.put("status", "NON-EXISTING STATUS");

        Long result = daoTest.queryForLong(sql, params);
        Assertions.assertNotNull(result);
    }

    @Test
    public void testFind_String_HashMap() {
        HashMap<String, String> params = new HashMap<>();
        String sql = "SELECT pk_test_data_history FROM tmp_test_data_history limit 1";

        List<TestData> result = daoTest.find(sql, params);
        Assertions.assertNotNull(result);
    }

    @Test
    public void testFind_String_HashMap_Class() {
        HashMap<String, String> params = new HashMap<>();
        String sql = "SELECT pk_test_data_history FROM tmp_test_data_history limit 1";

        List<TestDataHistory> result = (List<TestDataHistory>) daoTest.find(sql, params, TestDataHistory.class);
        Assertions.assertNotNull(result);

    }

    @Test
    public void testFind_String_HashMap_Class_Limit_Offset() {
        HashMap<String, String> params = new HashMap<>();
        String sql = "SELECT pk_test_data_history FROM tmp_test_data_history";

        int limit = 1;
        int offset = 2;
        List<TestDataHistory> result = (List<TestDataHistory>) daoTest.find(sql, params, TestDataHistory.class, limit, offset);
        Assertions.assertNotNull(result);

    }

    @Test
    public void testFind_SQLQueryDynamic_Class() {
        SQLQueryDynamic sqlQuery = new SQLQueryDynamic(TestData.class);
        sqlQuery.addCondition("td_login_id", "1234567890", Condition.EQUAL);
        sqlQuery.setTotalResultCount(2);

        List result = daoTest.find(sqlQuery);
        Assertions.assertNotNull(result);
    }

    @Test
    public void testFind_SQLQueryDynamic_Like() {
        SQLQueryDynamic sqlQuery = new SQLQueryDynamic(TestData.class);
        sqlQuery.addConditionLikesSmart(new String[]{"td_login_id", "td_status"}, "1234567890");
        sqlQuery.setTotalResultCount(2);

        List result = daoTest.find(sqlQuery);
        Assertions.assertNotNull(result);
    }

    @Test
    public void testFindStrings() {
        HashMap params = new HashMap();
        String sql = "SELECT td_text FROM tmp_test_data";
        List<String> result = daoTest.findStrings(sql, params);
        Assertions.assertNotNull(result);

    }

    @Test
    public void testFindLongs() {
        HashMap params = new HashMap();
        String sql = "SELECT pk_test_data FROM tmp_test_data";
        List<Long> result = daoTest.findLongs(sql, params);
        Assertions.assertNotNull(result);
    }

}
