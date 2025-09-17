package cl.kanopus.jdbc.example;

import cl.kanopus.common.data.Paginator;
import cl.kanopus.common.data.Searcher;
import cl.kanopus.jdbc.DAOInterface;
import cl.kanopus.jdbc.example.entity.TestData;
import cl.kanopus.jdbc.exception.DataException;
import cl.kanopus.jdbc.util.QueryIterator;
import cl.kanopus.jdbc.util.SQLQueryDynamic;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Pablo Diaz Saavedra
 * @email pabloandres.diazsaavedra@gmail.com
 *
 */
@Repository
public class ExampleDAO extends AbstractBaseDAO<TestData, Long> implements DAOInterface<TestData, Long> {

    /**
     * Get a list of records with a limit
     *
     * @param searcher
     * @return
     */
    public Paginator<TestData> findWithPaginator(Searcher searcher) throws DataException {
        SQLQueryDynamic query = new SQLQueryDynamic(TestData.class);
        query.setLimit(searcher.getLimit());
        query.setOffset(searcher.getOffset());
        return super.findPaginator(query);
    }

    /**
     * Gets a list of all records but loading into memory only 250 records.This
     * is ideal for traversing millions of records from the database without
     * causing overhead.
     *
     * @return
     */
    public Iterator<TestData> findWithIterator() {
        String sql = "SELECT * FROM tmp_test_data";
        final HashMap<String, String> params = new HashMap<>();

        return new QueryIterator<TestData>(3) {
            @Override
            public List<TestData> getData(int limit, int offset) {
                return find(sql, params, TestData.class, limit, offset);
            }
        };

    }

    public Iterator<TestData> findQueryIteratorWithSQLQueryDynamicDefaultLimit() {
        SQLQueryDynamic query = new SQLQueryDynamic(TestData.class);
        query.setOrderBy("pk_test_data");
        return super.findQueryIterator(query);
    }


    public Iterator<TestData> findQueryIteratorWithSQLQueryDynamicDefaultLimit5() {
        SQLQueryDynamic query = new SQLQueryDynamic(TestData.class);
        query.setOrderBy("pk_test_data");
        query.setLimit(5);
        return super.findQueryIterator(query);
    }
}
