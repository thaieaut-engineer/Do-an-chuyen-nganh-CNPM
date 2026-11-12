package model.medicalrecord;

import java.time.LocalDateTime;

public record EligibleVisit(
        long maLuotKham,
        String maBenhNhan,
        String tenBenhNhan,
        String soDienThoai,
        String maNhanVien,
        String tenBacSi,
        String maChuyenKhoa,
        String tenChuyenKhoa,
        LocalDateTime thoiGianTiepNhan,
        String trangThai) {
    @Override
    public String toString() {
        return "Lượt " + maLuotKham + " · " + tenBenhNhan
                + " · " + tenBacSi + " · " + trangThai;
    }
}
