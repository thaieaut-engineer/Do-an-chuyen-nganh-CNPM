package model.prescription;

import java.util.List;

public record PrescriptionPage(
        List<PrescriptionListItem> prescriptions,
        long totalCount,
        int page,
        int pageSize) {
    public PrescriptionPage {
        prescriptions = List.copyOf(prescriptions);
    }

    public int totalPages() {
        return Math.max(1, (int) Math.ceil((double) totalCount / pageSize));
    }
}
