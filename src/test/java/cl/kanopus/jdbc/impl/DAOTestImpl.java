package cl.kanopus.jdbc.impl;

import cl.kanopus.jdbc.impl.engine.Engine;
import cl.kanopus.jdbc.entity.Mapping;
import cl.kanopus.jdbc.example.entity.TestData;
import cl.kanopus.jdbc.util.SQLQueryDynamic;
import java.util.HashMap;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 *
 * @author godheaven
 */
@Repository
public class DAOTestImpl extends AbstractDAO<TestData, Long> implements DAOTest {

    @Autowired
    @Qualifier("jdbcTemplateTest")
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    protected NamedParameterJdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    @Override
    protected Engine getEngine() {
        return Engine.POSTGRES;
    }

    @Override
    public Integer queryForInteger(String sql, HashMap<String, ?> params) {
        return super.queryForInteger(sql, params);
    }

    @Override
    public Long queryForLong(String sql, HashMap<String, ?> params) {
        return super.queryForLong(sql, params);
    }

    @Override
    public String queryForString(String sql, HashMap<String, ?> params) {
        return super.queryForString(sql, params);
    }

    @Override
    public List<TestData> find(String sql, HashMap<String, ?> params) {
        return super.find(sql, params);
    }

    @Override
    public List find(String sql, HashMap<String, ?> params, Class<? extends Mapping> clazz) {
        return super.find(sql, params, clazz);
    }

    @Override
    public List<?> find(String sql, HashMap<String, ?> params, Class<? extends Mapping> clazz, int limit, int offset) {
        return super.find(sql, params, clazz, limit, offset);
    }

    @Override
    public List<String> findStrings(String sql, HashMap<String, ?> params) {
        return super.findStrings(sql, params);
    }

    @Override
    public List<Long> findLongs(String sql, HashMap<String, ?> params) {
        return super.findLongs(sql, params);
    }

    @Override
    public List<?> find(SQLQueryDynamic sqlQuery) {
        return super.find(sqlQuery);
    }

}
