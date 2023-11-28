package cl.kanopus.jdbc.impl.engine;

/**
 *
 * @author Pablo Diaz Saavedra
 * @email pabloandres.diazsaavedra@gmail.com
 */
public class PostgresEngine implements CustomEngine {

    private PostgresEngine() {
    }

    //Singleton Instance
    private static class SingletonHolder {

        public static final PostgresEngine INSTANCE = new PostgresEngine();
    }

    public static PostgresEngine getInstance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    public StringBuilder createSqlPagination(String sql, int limit, int offset) {
        StringBuilder sqlPagination = new StringBuilder();
        sqlPagination.append(sql);
        sqlPagination.append(" limit ").append(limit);
        if (offset > 0) {
            sqlPagination.append(" offset ").append(offset);
        }
        return sqlPagination;
    }

    @Override
    public String createSqlNextval(String sequence) {
        StringBuilder sqlNextval = new StringBuilder();
        sqlNextval.append("SELECT nextval('").append(sequence).append("')");
        return sqlNextval.toString();
    }

    @Override
    public String prepareSQL2Engine(String sql) {
        return sql;
    }

}
