package model.reception;

import java.time.LocalDateTime;

public record ReceptionAppointment(
        long maLichHen,
        String maBenhNhan,
        String hoTenBenhNhan,
        String soDienThoaiBenhNhan,
        String maNhanVien,
        String tenBacSi,
        String maChuyenKhoa,
        String tenChuyenKhoa,
        LocalDateTime thoiGianHen,
        String lyDoKham,
        String trangThai,
        Long maLuotKham,
        LocalDateTime thoiGianTiepNhan,
        String trangThaiLuotKham) {
}
