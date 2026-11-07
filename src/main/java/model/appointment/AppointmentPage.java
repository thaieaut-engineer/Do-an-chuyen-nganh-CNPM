package model.appointment;

import java.util.List;

public record AppointmentPage(
        List<LichHen> appointments,
        long totalCount,
        int page,
        int pageSize) {
    public AppointmentPage {
        appointments = List.copyOf(appointments);
    }

    public int totalPages() {
        return Math.max(1, (int) ((totalCount + pageSize - 1) / pageSize));
    }
}
