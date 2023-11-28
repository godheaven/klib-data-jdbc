package cl.kanopus.jdbc.example;

import cl.kanopus.jdbc.example.entity.TestData;
import cl.kanopus.jdbc.example.entity.TestType;
import cl.kanopus.jdbc.example.entity.enums.Color;
import cl.kanopus.jdbc.example.entity.enums.Status;
import cl.kanopus.common.data.Paginator;
import cl.kanopus.common.data.Searcher;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 *
 * @author Pablo Diaz Saavedra
 * @email pabloandres.diazsaavedra@gmail.com
 *
 */
@ContextConfiguration(locations = "classpath:applicationContext.xml")
@ExtendWith(SpringExtension.class)
public class ExampleService {

    @Autowired
    private ExampleDAO dao;

    @Test
    public void exampleFindWithPagination() throws Exception {
        Searcher searcher = new Searcher();
        Paginator<TestData> records = dao.findWithPaginator(searcher);
        for (TestData data : records.getRecords()) {
            //Print data here
        }
    }

    @Test
    public void exampleFindWithIterator() {
        Iterator<TestData> iterator = dao.findWithIterator();
        while (iterator.hasNext()) {
            TestData data = iterator.next();
            //Print data here
        }
    }

    @Test
    public void exampleGetByID() {
        TestData entity = dao.getByID((long) 1);
        Assertions.assertNotNull(entity);
    }

    @Test
    public void exampleDeleteByID() {
        int affected = dao.deleteByID((long) 35345345);
        Assertions.assertTrue(affected == 0);
    }

    @Test
    public void examplePersistAutomaticSerial() {

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

        TestData entityWithIdZero = dao.getByID((long) 0);
        Assertions.assertNull(entityWithIdZero);
    }

    @Test
    public void examplePersist() {

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

        TestData entityWithId = dao.getByID(entity.getId());
        Assertions.assertNotNull(entityWithId);
    }
}
