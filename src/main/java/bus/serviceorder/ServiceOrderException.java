package bus.serviceorder;

public final class ServiceOrderException extends Exception {
    public ServiceOrderException(String message) {
        super(message);
    }

    public ServiceOrderException(String message, Throwable cause) {
        super(message, cause);
    }
}
