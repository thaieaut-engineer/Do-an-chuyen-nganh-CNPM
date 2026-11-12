package model.serviceorder;

import java.math.BigDecimal;

public record ServiceCatalogItem(
        String maDichVu,
        String tenDichVu,
        String loaiDichVu,
        String donViTinh,
        String moTa,
        String trangThai,
        BigDecimal giaHienHanh) {
    public boolean dangApDung() {
        return "DangApDung".equals(trangThai);
    }

    @Override
    public String toString() {
        String price = giaHienHanh == null
                ? " · Chưa có giá hiện hành"
                : " · " + giaHienHanh.toPlainString() + " đ";
        return maDichVu + " · " + tenDichVu + price;
    }
}
