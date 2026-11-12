package model.medicalrecord;

import java.time.LocalDateTime;

public record MedicalRecordListItem(
        long maPhieuKham,
        long maLuotKham,
        String maBenhNhan,
        String tenBenhNhan,
        String tenBacSi,
        String maNhanVien,
        String maChuyenKhoa,
        String tenChuyenKhoa,
        LocalDateTime thoiGianLap,
        String trangThai,
        boolean canEdit) {
}
