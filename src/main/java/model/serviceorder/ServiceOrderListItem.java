package model.serviceorder;

import java.time.LocalDateTime;

public record ServiceOrderListItem(
        long maChiDinh,
        long maLuotKham,
        long maPhieuKham,
        String maBenhNhan,
        String tenBenhNhan,
        String maDichVu,
        String tenDichVu,
        String loaiDichVu,
        String maBacSi,
        String tenBacSi,
        LocalDateTime thoiGianChiDinh,
        String trangThaiLuotKham,
        String trangThai,
        String ghiChu,
        boolean daTinhPhi) {
}
