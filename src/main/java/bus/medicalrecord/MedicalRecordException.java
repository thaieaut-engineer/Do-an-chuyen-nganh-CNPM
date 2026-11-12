package bus.medicalrecord;

public final class MedicalRecordException extends Exception {
    public MedicalRecordException(String message) {
        super(message);
    }

    public MedicalRecordException(String message, Throwable cause) {
        super(message, cause);
    }
}
