package dao.prescription;

public final class PrescriptionDAOException extends RuntimeException {
    public PrescriptionDAOException(String message) {
        super(message);
    }

    public PrescriptionDAOException(String message, Throwable cause) {
        super(message, cause);
    }
}
