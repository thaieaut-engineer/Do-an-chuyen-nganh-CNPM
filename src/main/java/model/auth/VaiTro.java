package model.auth;

public final class VaiTro {
    private String maVaiTro;
    private String tenVaiTro;
    private String trangThai;

    public VaiTro() {
    }

    public VaiTro(String maVaiTro, String tenVaiTro, String trangThai) {
        this.maVaiTro = maVaiTro;
        this.tenVaiTro = tenVaiTro;
        this.trangThai = trangThai;
    }

    public String getMaVaiTro() {
        return maVaiTro;
    }

    public void setMaVaiTro(String maVaiTro) {
        this.maVaiTro = maVaiTro;
    }

    public String getTenVaiTro() {
        return tenVaiTro;
    }

    public void setTenVaiTro(String tenVaiTro) {
        this.tenVaiTro = tenVaiTro;
    }

    public String getTrangThai() {
        return trangThai;
    }

    public void setTrangThai(String trangThai) {
        this.trangThai = trangThai;
    }
}
