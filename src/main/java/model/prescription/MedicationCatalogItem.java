package model.prescription;

public record MedicationCatalogItem(
        String maThuoc,
        String tenThuoc,
        String hoatChat,
        String hamLuong,
        String dangBaoChe,
        String donViTinh,
        String duongDungMacDinh,
        String trangThai) {
    public boolean dangSuDung() {
        return "DangSuDung".equals(trangThai);
    }

    @Override
    public String toString() {
        return maThuoc + " · " + tenThuoc
                + (hamLuong == null || hamLuong.isBlank() ? "" : " · " + hamLuong)
                + (donViTinh == null || donViTinh.isBlank() ? "" : " · " + donViTinh);
    }
}
