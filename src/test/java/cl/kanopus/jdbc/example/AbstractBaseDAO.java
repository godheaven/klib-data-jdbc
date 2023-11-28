package cl.kanopus.jdbc.example;

import cl.kanopus.jdbc.entity.Mapping;
import cl.kanopus.jdbc.impl.AbstractDAO;
import cl.kanopus.jdbc.impl.engine.Engine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * This abstract class defines methods for data access that are common,
 * generally, all kinds of data access DAO must implement this class.Thus it is
 * given safely access the Connection database.The JdbcTemplate property is kept
 * private and gives access to the database through the methods implemented in
 * this AbstractDAO.
 *
 * @author Pablo Diaz Saavedra
 * @param <T>
 * @param <ID>
 * @email pabloandres.diazsaavedra@gmail.com
 */
public abstract class AbstractBaseDAO<T extends Mapping, ID> extends AbstractDAO<T, ID> {

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    protected NamedParameterJdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }

    @Override
    protected Engine getEngine() {
        return Engine.POSTGRES;
    }

}
