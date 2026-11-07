package model.appointment;

public record AppointmentDoctor(
        String maNhanVien,
        String hoTen,
        String maChuyenKhoa,
        String tenChuyenKhoa) {
    @Override
    public String toString() {
        return hoTen;
    }
}
