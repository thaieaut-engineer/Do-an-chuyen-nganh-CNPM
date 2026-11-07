package model.appointment;

import java.time.LocalDate;
import java.time.LocalTime;

public record AppointmentSlot(
        long maKhungGio,
        LocalDate ngayKham,
        LocalTime gioBatDau,
        LocalTime gioKetThuc,
        int soLuongToiDa,
        int soLuongDaDat) {
    @Override
    public String toString() {
        return gioBatDau + " - " + gioKetThuc
                + " (" + soLuongDaDat + "/" + soLuongToiDa + ")";
    }
}
