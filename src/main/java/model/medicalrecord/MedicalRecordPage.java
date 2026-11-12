package model.medicalrecord;

import java.util.List;

public record MedicalRecordPage(
        List<MedicalRecordListItem> records,
        long totalCount,
        int page,
        int pageSize) {
    public MedicalRecordPage {
        records = List.copyOf(records);
    }

    public int totalPages() {
        return Math.max(1, (int) Math.ceil((double) totalCount / pageSize));
    }
}
