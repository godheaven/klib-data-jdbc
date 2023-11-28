
![Logo](https://www.kanopus.cl/admin/javax.faces.resource/images/logo-gray.png.xhtml?ln=paradise-layout)


# klib-data-jdbc

This project is designed as a programming model that allows mapping the structures of a relational database to entities represented by Java classes.
This simplifies the way you interact with the database and reduces writing SQL code.

Currently this library has support for the following database engines:
- Postgresql
- Oracle
- SQL Server


## Features
- Automatic SQL generation based on annotations
- SQL is prepared in advance avoiding SQL injection
- Mapping of different data types such as String, Long, Integer, Enums, Json, LocalDateTime and others.
- It is possible to define relationships between different tables using @JoinTable
- It is possible to build complex queries using SQLQueryDynamic
- It is possible to group columns of an entity with @ColumnGroup for a better understanding
- Queries that return many records can implement Paginator to obtain a specific number of records.
- Queries that return many records can implement QueryIterator, in this way millions of records could be processed without compromising the performance of the application.
 

## Usage/Examples

1. Simple mapping
```java

import cl.kanopus.jdbc.entity.Mapping;
import cl.kanopus.jdbc.entity.annotation.Column;
import cl.kanopus.jdbc.entity.annotation.Table;

/**
  <pre>
  CREATE TABLE tmp_test_type
  (
  pk_test_type integer NOT NULL,
  name character varying(10) NOT NULL,
  CONSTRAINT tmp_test_type_pkey PRIMARY KEY (pk_test_type)
  );
  ALTER TABLE tmp_test_type OWNER TO user_test;
 
  INSERT INTO tmp_test_type(pk_test_type, name) VALUES(1, 'ONE');
  INSERT INTO tmp_test_type(pk_test_type, name) VALUES(2, 'TWO');
  </pre>
 */
@Table(name = "tmp_test_type", keys = {"pk_test_type"})
public class TestType extends Mapping {

    @Column(name = "pk_test_type")
    private long id;

    @Column(name = "name", length = 10)
    private String name;

    //Getter And Setter

}

```

1. Complex Mapping
```java
import cl.kanopus.jdbc.entity.Mapping;
import cl.kanopus.jdbc.entity.annotation.Column;
import cl.kanopus.jdbc.entity.annotation.ColumnGroup;
import cl.kanopus.jdbc.entity.annotation.JoinTable;
import cl.kanopus.jdbc.entity.annotation.Table;
import cl.kanopus.jdbc.example.entity.enums.Color;
import cl.kanopus.jdbc.example.entity.enums.Status;
import cl.kanopus.jdbc.util.parser.EnumParser;
import cl.kanopus.jdbc.util.parser.JsonListParser;
import cl.kanopus.jdbc.util.parser.JsonParser;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
  <pre>
  CREATE TABLE tmp_test_data
  (
     pk_test_data NUMBER NOT NULL,
     td_system_id NUMBER NOT NULL,
     td_login_id VARCHAR2(10 CHAR) NOT NULL,
     td_date timestamp without time zone NOT NULL,
     td_local_date date NOT NULL,
     td_local_date_time timestamp without time zone NOT NULL,
     td_text VARCHAR2(100 CHAR) NOT NULL,
     td_status VARCHAR2(10 CHAR) NOT NULL,
     td_color_id NUMBER,
     td_color_json JSONB,
     td_data_json JSONB,
     td_list_json JSONB,
     fk_test_type NUMBER NOT NULL
 
  );
 
  -- Primary key
  ALTER TABLE tmp_test_data ADD CONSTRAINT PK_TEST PRIMARY KEY (test_id);
 
  --sequence CREATE SEQUENCE seq_test_data INCREMENT BY 1 NOCACHE NOCYCLE;
  </pre>
 */
@Table(name = "tmp_test_data", sequence = "tmp_test_data_pk_test_data_seq", keys = {"pk_test_data"})
public class TestData extends Mapping {

    @Column(name = "pk_test_data", serial = true)
    private long id;

    @Column(name = "td_system_id")
    private long systemId;

    @Column(name = "td_login_id", length = 10)
    private String loginId;

    @Column(name = "td_date")
    private Date date;

    @Column(name = "td_local_date")
    private LocalDate localDate;

    @Column(name = "td_local_date_time")
    private LocalDateTime localDateTime;

    @Column(name = "td_status", parser = EnumParser.class, parserResult = Status.class)
    private Status status;

    @Column(name = "td_color_id", parser = EnumParser.class, parserResult = Color.class)
    private Color color;

    @Column(name = "td_color_json", parser = JsonListParser.class, parserResult = Color.class)
    private List<Color> colors;

    @Column(name = "td_data_json", parser = JsonParser.class, parserResult = JsonData.class)
    private JsonData json;

    @Column(name = "td_list_json", parser = JsonListParser.class, parserResult = JsonData.class)
    private List<JsonData> jsonList;

    @JoinTable(table = TestType.class, foreignKey = "fk_test_type", lazy = true)
    private TestType type;

    @ColumnGroup(result = TestDataGroup.class, nullable = true)
    private TestDataGroup group;

    //Getter And Setter

    public static class TestDataGroup {

        @Column(name = "td_text", length = 100, auditable = false)
        private String text;

        //Getter And Setter

    }

    public static class JsonData {

        private int id;
        private String text;
        private Date date;
        private boolean enabled;
        
        //Getter And Setter

    }
}

```

3. Crud Operations
```java

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

```


## Authors

- [@pabloandres.diazsaavedra](https://www.linkedin.com/in/pablo-diaz-saavedra-4b7b0522/)


## License

This is free software and I hope you enjoy it.

[![GPLv3 License](https://img.shields.io/badge/License-GPL%20v3-yellow.svg)](https://opensource.org/licenses/)





## Support

For support, email pabloandres.diazsaavedra@gmail.com

