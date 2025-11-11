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
package cl.kanopus.jdbc.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * This class allows queries to the database using automatic pagination. The
 * main objective of this class is to run queries that return a huge quantity of
 * records, avoiding problems of memory consumption.
 * <p>
 * This class is only kept in memory {limit} records set and not the
 * whole entire resultset.
 *
 * @author Pablo Diaz Saavedra
 *
 */
public abstract class QueryIterator<T> implements Iterator<T> {

    private int limit = 1000;
    private int offset = 0;
    private Collection<T> list = new ArrayList<>();
    private Iterator<T> iterator = list.iterator();
    private boolean execute = true;

    protected QueryIterator() {
    }

    protected QueryIterator(int limit) {
        this.limit = limit;
    }

    public abstract List<T> getData(int limit, int offset);

    @Override
    public final boolean hasNext() {
        boolean hasNext = iterator.hasNext();
        if (!hasNext && execute) {
            list = getData(limit, offset);
            iterator = list.iterator();
            hasNext = iterator.hasNext();
            if (list.isEmpty()) {
                execute = false;
            }
            offset += limit;
        }
        return hasNext;
    }

    @Override
    public final T next() {
        return iterator.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
