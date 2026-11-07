package dao.appointment;

public final class AppointmentDAOException extends RuntimeException {
    public enum Reason {
        NOT_FOUND,
        INVALID_REFERENCE,
        SLOT_UNAVAILABLE,
        SCHEDULE_UNAVAILABLE,
        CONFLICT,
        INVALID_STATE,
        DATABASE_ERROR
    }

    private final Reason reason;

    public AppointmentDAOException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
