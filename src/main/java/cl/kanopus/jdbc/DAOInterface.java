package cl.kanopus.jdbc;

import cl.kanopus.jdbc.exception.DataException;
import cl.kanopus.common.data.enums.SortOrder;
import java.util.List;

public interface DAOInterface<T, I> {

    long generateID() throws DataException;

    void persist(T entity) throws DataException;

    int update(T entity) throws DataException;

    int deleteByID(I id) throws DataException;

    T getByID(I id) throws DataException;

    T getByID(I id, boolean loadAll) throws DataException;

    List<T> findAll() throws DataException;

    List<T> findTop(int limit, SortOrder sortOrder) throws DataException;

}
