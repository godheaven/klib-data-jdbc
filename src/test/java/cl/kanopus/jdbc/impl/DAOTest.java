package cl.kanopus.jdbc.impl;

import cl.kanopus.jdbc.DAOInterface;
import cl.kanopus.jdbc.entity.Mapping;
import cl.kanopus.jdbc.example.entity.TestData;
import cl.kanopus.jdbc.util.SQLQueryDynamic;
import java.util.HashMap;
import java.util.List;

public interface DAOTest extends DAOInterface<TestData, Long> {

    Integer queryForInteger(String sql, HashMap<String, ?> params);

    Long queryForLong(String sql, HashMap<String, ?> params);

    String queryForString(String sql, HashMap<String, ?> params);

    List<TestData> find(String sql, HashMap<String, ?> params);

    List<?> find(String sql, HashMap<String, ?> params, Class<? extends Mapping> clazz);

    List<?> find(String sql, HashMap<String, ?> params, Class<? extends Mapping> clazz, int limit, int offset);

    List<String> findStrings(String sql, HashMap<String, ?> params);

    List<Long> findLongs(String sql, HashMap<String, ?> params);

    List<?> find(SQLQueryDynamic sqlQuery);

}
