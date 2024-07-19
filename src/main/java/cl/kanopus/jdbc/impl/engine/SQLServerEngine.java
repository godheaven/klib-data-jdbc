package cl.kanopus.jdbc.impl.engine;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Pablo Diaz Saavedra
 * @email pabloandres.diazsaavedra@gmail.com
 */
public class SQLServerEngine implements CustomEngine {

    final Pattern pattern = Pattern.compile("([\\\\.a-zA-Z0-9_-]+)::date", Pattern.MULTILINE);

    protected SQLServerEngine() {
    }

    //Singleton Instance
    private static class SingletonHolder {

        public static final SQLServerEngine INSTANCE = new SQLServerEngine();
    }

    public static SQLServerEngine getInstance() {
        return SingletonHolder.INSTANCE;
    }

    @Override
    public StringBuilder createSqlPagination(String sql, int limit, int offset) {
        StringBuilder sqlPagination = new StringBuilder();
        sqlPagination.append(sql);
        sqlPagination.append(" offset ").append(offset);
        sqlPagination.append(" rows fetch next ").append(limit);
        sqlPagination.append(" rows only ");
        return sqlPagination;
    }

    @Override
    public String createSqlNextval(String sequence) {
        StringBuilder sqlNextval = new StringBuilder();
        sqlNextval.append("SELECT NEXT VALUE FOR ").append(sequence);
        return sqlNextval.toString();
    }

    @Override
    public String prepareSQL2Engine(String sql) {
        String newSql = sql.replaceAll("\\|\\|", "+").replaceAll("count\\(\\*\\)", "count_big\\(\\*\\)");

        Matcher matcher = pattern.matcher(newSql);
        return matcher.replaceAll("CAST($1 as date)");
    }
}
