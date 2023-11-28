package cl.kanopus.jdbc.impl.engine;

/**
 *
 * @author Pablo Diaz Saavedra
 * @email pabloandres.diazsaavedra@gmail.com
 */
public interface CustomEngine {

    abstract StringBuilder createSqlPagination(String sql, int limit, int offset);

    abstract String createSqlNextval(String sequence);

    abstract String prepareSQL2Engine(String sql);

}
