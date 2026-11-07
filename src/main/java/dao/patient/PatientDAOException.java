package dao.patient;

public final class PatientDAOException extends RuntimeException {
    public enum Reason {
        DUPLICATE_DOCUMENT,
        NOT_FOUND,
        REFERENCED_RECORD,
        DATABASE_ERROR
    }

    private final Reason reason;

    public PatientDAOException(Reason reason, String message, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
