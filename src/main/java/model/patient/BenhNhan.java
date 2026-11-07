package model.patient;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record BenhNhan(
        String maBenhNhan,
        String hoTen,
        LocalDate ngaySinh,
        String gioiTinh,
        String soGiayTo,
        String soDienThoai,
        String email,
        String diaChi,
        LocalDateTime ngayTaoHoSo,
        String trangThai) {
}
