package bus.reception;

public final class TiepNhanException extends Exception {
    public TiepNhanException(String message) {
        super(message);
    }

    public TiepNhanException(String message, Throwable cause) {
        super(message, cause);
    }
}
