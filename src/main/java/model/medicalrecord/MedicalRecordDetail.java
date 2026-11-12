package model.medicalrecord;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MedicalRecordDetail(
        long maPhieuKham,
        long maLuotKham,
        String trangThaiPhieu,
        LocalDateTime thoiGianLap,
        LocalDateTime thoiGianKy,
        String lyDoKham,
        String trieuChung,
        String dauHieuSinhTon,
        String ketQuaLamSang,
        String chanDoan,
        String huongDieuTri,
        String maBenhNhan,
        String tenBenhNhan,
        LocalDate ngaySinh,
        String gioiTinh,
        String soDienThoai,
        String diaChi,
        LocalDateTime thoiGianTiepNhan,
        String trangThaiLuotKham,
        String maNhanVien,
        String tenBacSi,
        String maChuyenKhoa,
        String tenChuyenKhoa) {
}
