package model.serviceorder;

import java.time.LocalDate;

public record ServiceOrderFilter(
        String searchText,
        LocalDate fromDate,
        LocalDate toDate,
        String serviceType,
        String status,
        int page,
        int pageSize) {
}
