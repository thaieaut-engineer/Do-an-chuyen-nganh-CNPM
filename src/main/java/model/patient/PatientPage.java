package model.patient;

import java.util.List;

public record PatientPage(List<BenhNhan> patients, long totalCount, int page, int pageSize) {
    public PatientPage {
        patients = List.copyOf(patients);
    }

    public int totalPages() {
        return Math.max(1, (int) ((totalCount + pageSize - 1) / pageSize));
    }
}
