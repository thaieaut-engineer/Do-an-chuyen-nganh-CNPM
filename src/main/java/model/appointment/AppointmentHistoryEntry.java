package model.appointment;

import java.time.LocalDateTime;

public record AppointmentHistoryEntry(
        LocalDateTime thoiDiem,
        String nguoiThayDoi,
        String hanhDong,
        String giaTriCu,
        String giaTriMoi,
        String lyDo) {
}
