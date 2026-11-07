package model.appointment;

public record AppointmentPatient(
        String maBenhNhan,
        String hoTen,
        String soDienThoai) {
    @Override
    public String toString() {
        String phone = soDienThoai == null || soDienThoai.isBlank()
                ? "" : " - " + soDienThoai;
        return hoTen + " (" + maBenhNhan + ")" + phone;
    }
}
