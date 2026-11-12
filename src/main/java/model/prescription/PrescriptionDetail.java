package model.prescription;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PrescriptionDetail(
        long maDonThuoc,
        long maLuotKham,
        String maBacSi,
        String tenBacSi,
        String tenChuyenKhoa,
        LocalDateTime ngayKe,
        String loiDan,
        String trangThai,
        String trangThaiLuotKham,
        String maBenhNhan,
        String tenBenhNhan,
        LocalDate ngaySinh,
        String gioiTinh,
        String soDienThoai,
        LocalDateTime thoiGianTiepNhan,
        boolean daXuatThuoc,
        List<PrescriptionLineDetail> lines) {
    public PrescriptionDetail {
        lines = List.copyOf(lines);
    }
}
