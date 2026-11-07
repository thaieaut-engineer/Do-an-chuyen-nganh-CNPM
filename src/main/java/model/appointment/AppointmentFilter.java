package model.appointment;

import java.time.LocalDate;

public record AppointmentFilter(
        String searchText,
        LocalDate fromDate,
        LocalDate toDate,
        String status,
        String doctorId,
        String specialtyId,
        int page,
        int pageSize) {
}
