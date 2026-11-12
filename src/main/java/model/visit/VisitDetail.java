package model.visit;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record VisitDetail(
        long maLuotKham,
        String trangThai,
        LocalDateTime thoiGianTiepNhan,
        LocalDateTime thoiGianKetThuc,
        String ghiChu,
        String maBenhNhan,
        String tenBenhNhan,
        LocalDate ngaySinhBenhNhan,
        String gioiTinhBenhNhan,
        String soDienThoaiBenhNhan,
        String diaChiBenhNhan,
        String maNhanVien,
        String tenBacSi,
        String maChuyenKhoa,
        String tenChuyenKhoa,
        Long maLichHen,
        LocalDateTime thoiGianHen,
        String trangThaiLichHen,
        Long maPhieuKham,
        String trangThaiPhieuKham,
        long chiDinhDangXuLy) {
}
