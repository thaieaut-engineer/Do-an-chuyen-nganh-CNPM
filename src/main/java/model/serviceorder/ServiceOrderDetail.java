package model.serviceorder;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ServiceOrderDetail(
        long maChiDinh,
        long maLuotKham,
        long maPhieuKham,
        String trangThaiPhieuKham,
        String trangThaiLuotKham,
        String maBenhNhan,
        String tenBenhNhan,
        LocalDate ngaySinh,
        String gioiTinh,
        String maBacSi,
        String tenBacSi,
        String tenChuyenKhoa,
        String maDichVu,
        String tenDichVu,
        String loaiDichVu,
        String donViTinh,
        LocalDateTime thoiGianChiDinh,
        LocalDateTime thoiGianThucHien,
        String trangThai,
        String maNhanVienThucHien,
        String tenNhanVienThucHien,
        String ketQua,
        String ghiChu,
        boolean daTinhPhi) {
}
