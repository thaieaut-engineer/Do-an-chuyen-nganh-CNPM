package model.auth;

public final class TaiKhoan {
    private long maTaiKhoan;
    private String tenDangNhap;
    private String matKhauHash;
    private String maNhanVien;
    private String trangThai;

    public TaiKhoan() {
    }

    public TaiKhoan(
            long maTaiKhoan,
            String tenDangNhap,
            String matKhauHash,
            String maNhanVien,
            String trangThai) {
        this.maTaiKhoan = maTaiKhoan;
        this.tenDangNhap = tenDangNhap;
        this.matKhauHash = matKhauHash;
        this.maNhanVien = maNhanVien;
        this.trangThai = trangThai;
    }

    public long getMaTaiKhoan() {
        return maTaiKhoan;
    }

    public void setMaTaiKhoan(long maTaiKhoan) {
        this.maTaiKhoan = maTaiKhoan;
    }

    public String getTenDangNhap() {
        return tenDangNhap;
    }

    public void setTenDangNhap(String tenDangNhap) {
        this.tenDangNhap = tenDangNhap;
    }

    public String getMatKhauHash() {
        return matKhauHash;
    }

    public void setMatKhauHash(String matKhauHash) {
        this.matKhauHash = matKhauHash;
    }

    public String getMaNhanVien() {
        return maNhanVien;
    }

    public void setMaNhanVien(String maNhanVien) {
        this.maNhanVien = maNhanVien;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }
}
