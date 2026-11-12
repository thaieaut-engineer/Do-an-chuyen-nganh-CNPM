package model.visit;

import java.time.LocalDateTime;

public record VisitListItem(
        long maLuotKham,
        String maBenhNhan,
        String tenBenhNhan,
        String maNhanVien,
        String tenBacSi,
        String maChuyenKhoa,
        String tenChuyenKhoa,
        Long maLichHen,
        LocalDateTime thoiGianTiepNhan,
        LocalDateTime thoiGianKetThuc,
        String trangThai) {
}
