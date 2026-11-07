package model.appointment;

public record AppointmentDraft(
        String maBenhNhan,
        String maNhanVien,
        String maChuyenKhoa,
        long maKhungGio,
        String lyDoKham,
        String ghiChu) {
}
