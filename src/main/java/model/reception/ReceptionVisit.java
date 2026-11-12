package model.reception;

import java.time.LocalDateTime;

public record ReceptionVisit(
        long maLuotKham,
        long maLichHen,
        String maBenhNhan,
        String hoTenBenhNhan,
        String maNhanVien,
        String tenBacSi,
        LocalDateTime thoiGianTiepNhan,
        String trangThai) {
}
