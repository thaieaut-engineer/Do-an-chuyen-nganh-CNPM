package model.prescription;

import java.math.BigDecimal;

public record PrescriptionLineDraft(
        Long maChiTietDon,
        String maThuoc,
        String lieuDung,
        String duongDung,
        String tanSuat,
        String thoiGianDung,
        BigDecimal soLuong,
        String donViTinh,
        String huongDan) {
}
