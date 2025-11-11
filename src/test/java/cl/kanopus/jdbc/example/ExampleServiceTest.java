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

import cl.kanopus.common.data.Paginator;
import cl.kanopus.common.data.Searcher;
import cl.kanopus.jdbc.config.TestDataSourceConfig;
import cl.kanopus.jdbc.example.entity.TestData;
import cl.kanopus.jdbc.example.entity.TestType;
import cl.kanopus.jdbc.example.entity.enums.Color;
import cl.kanopus.jdbc.example.entity.enums.Status;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

@ContextConfiguration(classes = {TestDataSourceConfig.class})
@ExtendWith(SpringExtension.class)
class ExampleServiceTest {

    @Autowired
    private ExampleDAO dao;

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    void initDao() {
        if (this.dao == null) {
            this.dao = applicationContext.getBean(ExampleDAO.class);
        }
    }

    @Test
    void exampleFindWithPagination() throws Exception {
        Paginator<TestData> records = dao.findWithPaginator(new Searcher<>());
        Assertions.assertNotNull(records);
        for (TestData data : records.getRecords()) {
            //Print data here
            Assertions.assertNotNull(data);
            Assertions.assertTrue(data.getId() > 0);
        }
    }

    @Test
    void exampleFindWithIterator() {
        Iterator<TestData> iterator = dao.findWithIterator();
        while (iterator.hasNext()) {
            TestData data = iterator.next();
            Assertions.assertNotNull(data);
            Assertions.assertTrue(data.getId() > 0);
        }
    }

    @Test
    void exampleFindQueryIteratorWithSQLQueryDynamicDefaultLimit() {
        Iterator<TestData> iterator = dao.findQueryIteratorWithSQLQueryDynamicDefaultLimit();
        while (iterator.hasNext()) {
            TestData data = iterator.next();
            Assertions.assertNotNull(data);
            Assertions.assertTrue(data.getId() > 0);
        }
    }


    @Test
    void exampleFindQueryIteratorWithSQLQueryDynamicDefaultLimit5() {
        Iterator<TestData> iterator = dao.findQueryIteratorWithSQLQueryDynamicDefaultLimit5();
        while (iterator.hasNext()) {
            TestData data = iterator.next();
            Assertions.assertNotNull(data);
            Assertions.assertTrue(data.getId() > 0);
        }
    }


    @Test
    void exampleGetByID() {
        TestData entity = dao.getById((long) 1);
        Assertions.assertNotNull(entity);
    }

    @Test
    void exampleDeleteByID() {
        int affected = dao.deleteById((long) 35345345);
        Assertions.assertEquals(0, affected);
    }

    @Test
    void examplePersistAutomaticSerial() {

        TestType type = new TestType();
        type.setId(1);

        List<Color> colors = new ArrayList<>();
        colors.add(Color.RED);
        colors.add(Color.BLACK);
        colors.add(Color.RED);

        TestData.TestDataGroup group = new TestData.TestDataGroup();
        group.setText("Data Example");

        TestData entity = new TestData();
        entity.setId(0); //If this field is Zero and serial = true, will no be included into SQL persist
        entity.setColor(Color.RED);
        entity.setSystemId(342);
        entity.setLoginId("test");
        entity.setDate(new Date());
        entity.setLocalDate(LocalDate.now());
        entity.setLocalDateTime(LocalDateTime.now());
        entity.setStatus(Status.SUCCESS);
        entity.setType(type);
        entity.setGroup(group);
        entity.setColors(colors);

        dao.persist(entity);

        TestData entityWithIdZero = dao.getById((long) 0);
        Assertions.assertNull(entityWithIdZero);
    }

    @Test
    void examplePersist() {

        TestType type = new TestType();
        type.setId(1);

        List<Color> colors = new ArrayList<>();
        colors.add(Color.RED);
        colors.add(Color.BLACK);
        colors.add(Color.RED);

        TestData.JsonData jd1 = new TestData.JsonData();
        jd1.setDate(new Date());
        jd1.setEnabled(true);
        jd1.setId(1);
        jd1.setText("text1");

        TestData.JsonData jd2 = new TestData.JsonData();
        jd2.setDate(new Date());
        jd2.setEnabled(false);
        jd2.setId(2);
        jd2.setText("text2");

        List<TestData.JsonData> jsonDataList = new ArrayList<>();
        jsonDataList.add(jd1);
        jsonDataList.add(jd2);

        TestData.TestDataGroup group = new TestData.TestDataGroup();
        group.setText("Data Example");

        TestData entity = new TestData();
        entity.setId(dao.generateID());
        entity.setColor(Color.RED);
        entity.setSystemId(342);
        entity.setLoginId("test");
        entity.setDate(new Date());
        entity.setLocalDate(LocalDate.now());
        entity.setLocalDateTime(LocalDateTime.now());
        entity.setStatus(Status.SUCCESS);
        entity.setType(type);
        entity.setGroup(group);
        entity.setColors(colors);
        entity.setJson(jd1);
        entity.setJsonList(jsonDataList);
        dao.persist(entity);

        TestData entityWithId = dao.getById(entity.getId());
        Assertions.assertNotNull(entityWithId);
    }
}
