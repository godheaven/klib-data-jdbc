package cl.kanopus.jdbc;

import cl.kanopus.common.data.enums.SortOrder;
import cl.kanopus.jdbc.exception.DataException;
import java.util.List;
import java.util.Optional;

public interface DAOInterface<T, I> {

    long generateID() throws DataException;

    T persist(T entity) throws DataException;

    T update(T entity) throws DataException;

    int deleteById(I id) throws DataException;

    Optional<T> findById(I id) throws DataException;

    T getById(I id) throws DataException;

    T getById(I id, boolean loadAll) throws DataException;

    List<T> findAll() throws DataException;

    List<T> findTop(int limit, SortOrder sortOrder) throws DataException;

    boolean existsById(I id) throws DataException;

}
