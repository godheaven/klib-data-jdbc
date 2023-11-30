package cl.kanopus.jdbc.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * This class allows queries to the database using automatic pagination. The
 * main objective of this class is to run queries that return a huge quantity of
 * records, avoiding problems of memory consumption.
 *
 * This class is only kept in memory {@link #LIMIT} records set and not the
 * whole entire resultset.
 *
 * @author Pablo Diaz Saavedra
 * @email pabloandres.diazsaavedra@gmail.com
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
