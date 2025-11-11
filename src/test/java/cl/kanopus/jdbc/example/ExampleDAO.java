/*-
 * !--
 * For support and inquiries regarding this library, please contact:
 *   soporte@kanopus.cl
 * 
 * Project website:
 *   https://www.kanopus.cl
 * %%
 * Copyright (C) 2025 Pablo Díaz Saavedra
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * --!
 */
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

@Repository
public class ExampleDAO extends AbstractBaseDAO<TestData, Long> implements DAOInterface<TestData, Long> {

    /**
     * Finds records with pagination support.
     *
     * @param searcher Search criteria including limit and offset.
     * @return A Paginator object containing the results.
     * @throws DataException If a data access error occurs.
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
     * @return An iterator over TestData records.
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
