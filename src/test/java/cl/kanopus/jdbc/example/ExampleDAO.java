package cl.kanopus.jdbc.example;

import cl.kanopus.jdbc.DAOInterface;
import cl.kanopus.jdbc.example.entity.TestData;
import cl.kanopus.jdbc.exception.DataException;
import cl.kanopus.common.data.Paginator;
import cl.kanopus.jdbc.util.QueryIterator;
import cl.kanopus.jdbc.util.SQLQueryDynamic;
import cl.kanopus.common.data.Searcher;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.springframework.stereotype.Repository;

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
        final HashMap params = new HashMap();

        Iterator<TestData> iterator = new QueryIterator<TestData>() {
            @Override
            public List<TestData> getData(int limit, int offset) {
                return find(sql, params, TestData.class, limit, offset);
            }
        };
        return iterator;

    }
}
