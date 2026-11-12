package model.prescription;

import java.time.LocalDate;

public record PrescriptionFilter(
        String searchText,
        LocalDate fromDate,
        LocalDate toDate,
        String status,
        String doctorId,
        String specialtyId,
        int page,
        int pageSize) {
}
