package model.prescription;

import java.time.LocalDateTime;

public record PrescriptionListItem(
        long maDonThuoc,
        long maLuotKham,
        String maBenhNhan,
        String tenBenhNhan,
        String soDienThoai,
        String maBacSi,
        String tenBacSi,
        String maChuyenKhoa,
        String tenChuyenKhoa,
        LocalDateTime ngayKe,
        int soLoaiThuoc,
        String trangThai,
        boolean daXuatThuoc) {
}
