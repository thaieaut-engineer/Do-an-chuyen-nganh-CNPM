package model.prescription;

import java.math.BigDecimal;

public record PrescriptionLineDetail(
        long maChiTietDon,
        String maThuoc,
        String tenThuoc,
        String hoatChat,
        String hamLuong,
        String dangBaoChe,
        String donViTinhThuoc,
        String trangThaiThuoc,
        String lieuDung,
        String duongDung,
        String tanSuat,
        String thoiGianDung,
        BigDecimal soLuong,
        String donViTinh,
        String huongDan,
        boolean daXuat) {
}
