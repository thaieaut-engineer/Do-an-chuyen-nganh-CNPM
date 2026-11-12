package bus.visit;

public final class VisitException extends Exception {
    public VisitException(String message) {
        super(message);
    }

    public VisitException(String message, Throwable cause) {
        super(message, cause);
    }
}
