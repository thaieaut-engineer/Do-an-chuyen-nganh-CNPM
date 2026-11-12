package model.visit;

import java.time.LocalDate;

public record VisitFilter(
        String searchText,
        LocalDate fromDate,
        LocalDate toDate,
        String status,
        String doctorId,
        String specialtyId,
        int page,
        int pageSize) {
}
