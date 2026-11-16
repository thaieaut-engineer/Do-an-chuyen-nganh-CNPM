package dao.warehouse;

public final class WarehouseDAOException extends RuntimeException {
    public WarehouseDAOException(String message) {
        super(message);
    }

    public WarehouseDAOException(String message, Throwable cause) {
        super(message, cause);
    }
}
