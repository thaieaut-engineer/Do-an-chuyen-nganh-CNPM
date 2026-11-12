package model.medicalrecord;

import java.time.LocalDate;

public record MedicalRecordFilter(
        String searchText,
        LocalDate fromDate,
        LocalDate toDate,
        String status,
        String doctorId,
        String specialtyId,
        int page,
        int pageSize) {
}
