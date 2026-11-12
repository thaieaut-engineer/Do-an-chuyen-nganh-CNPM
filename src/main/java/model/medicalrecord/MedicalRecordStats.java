package model.medicalrecord;

public record MedicalRecordStats(
        long total,
        long createdToday,
        long drafts,
        long completed) {
}
