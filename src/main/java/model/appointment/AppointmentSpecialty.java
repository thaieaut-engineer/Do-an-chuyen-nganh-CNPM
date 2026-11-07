package model.appointment;

public record AppointmentSpecialty(String maChuyenKhoa, String tenChuyenKhoa) {
    @Override
    public String toString() {
        return tenChuyenKhoa;
    }
}
