package model.appointment;

import java.time.LocalDateTime;

public record LichHen(
        long maLichHen,
        String maBenhNhan,
        String tenBenhNhan,
        String soDienThoaiBenhNhan,
        Long maKhungGio,
        String maNhanVien,
        String tenBacSi,
        String maChuyenKhoa,
        String tenChuyenKhoa,
        LocalDateTime thoiGianHen,
        String lyDoKham,
        String trangThai,
        String nguoiTao,
        String tenNguoiTao,
        String ghiChu) {
}
