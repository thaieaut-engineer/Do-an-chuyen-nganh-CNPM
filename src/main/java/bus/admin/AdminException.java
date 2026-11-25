package bus.admin;

public final class AdminException extends RuntimeException {
    public AdminException(String message) {
        super(message);
    }

    public AdminException(String message, Throwable cause) {
        super(message, cause);
    }
}
