package cl.kanopus.jdbc.impl.engine;

/**
 *
 * @author Pablo Diaz Saavedra
 * @email pabloandres.diazsaavedra@gmail.com
 */
public class OracleEngine implements CustomEngine {

    private OracleEngine() {
    }

    //Singleton Instance
    private static class SingletonHolder {

        public static final OracleEngine INSTANCE = new OracleEngine();
    }

    public static OracleEngine getInstance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    public StringBuilder createSqlPagination(String sql, int limit, int offset) {
        StringBuilder sqlPagination = new StringBuilder();
        sqlPagination.append("SELECT * FROM (");
        sqlPagination.append("SELECT consulta.*, rownum rn FROM(");
        sqlPagination.append(sql);
        sqlPagination.append(") consulta ");
        sqlPagination.append(") WHERE rn<= ").append(limit + offset);
        if (offset > 0) {
            sqlPagination.append(" AND rn > ").append(offset);
        }

        return sqlPagination;
    }

    @Override
    public String createSqlNextval(String sequence) {
        StringBuilder sqlNextval = new StringBuilder();
        sqlNextval.append("SELECT ").append(sequence).append(".nextval FROM dual");
        return sqlNextval.toString();
    }

    @Override
    public String prepareSQL2Engine(String sql) {
        return sql;
    }
}
