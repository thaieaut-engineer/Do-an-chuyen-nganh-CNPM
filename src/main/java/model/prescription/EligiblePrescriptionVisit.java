package model.prescription;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record EligiblePrescriptionVisit(
        long maLuotKham,
        String maBenhNhan,
        String tenBenhNhan,
        LocalDate ngaySinh,
        String gioiTinh,
        String soDienThoai,
        String maBacSi,
        String tenBacSi,
        String tenChuyenKhoa,
        LocalDateTime thoiGianTiepNhan,
        String trangThaiLuotKham,
        Long maPhieuKham,
        String trangThaiPhieuKham) {
    @Override
    public String toString() {
        return "Lượt " + maLuotKham + " · " + tenBenhNhan
                + " · " + tenBacSi + " · " + trangThaiLuotKham;
    }
}
