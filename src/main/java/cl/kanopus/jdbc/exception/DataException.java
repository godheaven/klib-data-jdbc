package cl.kanopus.jdbc.exception;

import org.springframework.dao.DataAccessException;

public class DataException extends DataAccessException {

    private static final long serialVersionUID = 1L;

    private final String error;

    public DataException(String error) {
        super(error);
        this.error = error;
    }

    public DataException(String error, Throwable trow) {
        super(error, trow);
        this.error = error;

    }

    public String getError() {
        return error;
    }

}
