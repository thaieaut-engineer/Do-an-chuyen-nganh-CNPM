package dao.visit;

public final class VisitDAOException extends RuntimeException {
    public VisitDAOException(String message) {
        super(message);
    }

    public VisitDAOException(String message, Throwable cause) {
        super(message, cause);
    }
}
