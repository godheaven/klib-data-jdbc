package cl.kanopus.jdbc.entity.mapper;

import org.springframework.jdbc.core.RowMapper;

/**
 *
 * AbstractRowMapper class which implements RowMapper interface.
 *
 * RowMapper is used to convert the ResultSet into domain specific object. All
 * class of types RowMapper must be implemented this abstract class and override
 * the mapRow() method.
 *
 * @author Pablo Diaz Saavedra
 * @email pabloandres.diazsaavedra@gmail.com
 * @param <T>
 */
public abstract class AbstractRowMapper<T> implements RowMapper<T> {

}
