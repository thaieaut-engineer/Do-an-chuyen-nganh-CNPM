package model.serviceorder;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EligibleOrderVisit(
        long maLuotKham,
        long maPhieuKham,
        String trangThaiPhieuKham,
        String maBenhNhan,
        String tenBenhNhan,
        String gioiTinh,
        LocalDate ngaySinh,
        String maNhanVien,
        String tenBacSi,
        String tenChuyenKhoa,
        LocalDateTime thoiGianTiepNhan,
        String trangThaiLuotKham) {
    @Override
    public String toString() {
        return "Lượt " + maLuotKham + " · " + tenBenhNhan
                + " · " + tenBacSi + " · " + trangThaiLuotKham;
    }
}
