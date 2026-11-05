package model.auth;

public final class NhanVien {
    private String maNhanVien;
    private String hoTen;
    private String trangThaiCongTac;

    public NhanVien() {
    }

    public NhanVien(String maNhanVien, String hoTen, String trangThaiCongTac) {
        this.maNhanVien = maNhanVien;
        this.hoTen = hoTen;
        this.trangThaiCongTac = trangThaiCongTac;
    }

    public String getMaNhanVien() {
        return maNhanVien;
    }

    public void setMaNhanVien(String maNhanVien) {
        this.maNhanVien = maNhanVien;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public String getTrangThaiCongTac() {
        return trangThaiCongTac;
    }

    public void setTrangThaiCongTac(String trangThaiCongTac) {
        this.trangThaiCongTac = trangThaiCongTac;
    }
}
