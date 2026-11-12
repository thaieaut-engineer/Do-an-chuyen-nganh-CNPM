package model.prescription;

public record PrescriptionStats(
        long total,
        long createdToday,
        long drafts,
        long signed,
        long sentOrDispensed) {
}
