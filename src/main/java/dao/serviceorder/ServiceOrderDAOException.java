package dao.serviceorder;

public final class ServiceOrderDAOException extends RuntimeException {
    public ServiceOrderDAOException(String message) {
        super(message);
    }

    public ServiceOrderDAOException(String message, Throwable cause) {
        super(message, cause);
    }
}
