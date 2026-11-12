package dao.medicalrecord;

public final class MedicalRecordDAOException extends RuntimeException {
    public MedicalRecordDAOException(String message) {
        super(message);
    }

    public MedicalRecordDAOException(String message, Throwable cause) {
        super(message, cause);
    }
}
