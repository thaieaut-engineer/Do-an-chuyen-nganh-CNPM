package model.auth;

public final class Quyen {
    private String maQuyen;
    private String tenQuyen;
    private String nhomChucNang;
    private String moTa;

    public Quyen() {
    }

    public Quyen(String maQuyen, String tenQuyen, String nhomChucNang, String moTa) {
        this.maQuyen = maQuyen;
        this.tenQuyen = tenQuyen;
        this.nhomChucNang = nhomChucNang;
        this.moTa = moTa;
    }

    public String getMaQuyen() {
        return maQuyen;
    }

    public void setMaQuyen(String maQuyen) {
        this.maQuyen = maQuyen;
    }

    public String getTenQuyen() {
        return tenQuyen;
    }

    public void setTenQuyen(String tenQuyen) {
        this.tenQuyen = tenQuyen;
    }

    public String getNhomChucNang() {
        return nhomChucNang;
    }

    public void setNhomChucNang(String nhomChucNang) {
        this.nhomChucNang = nhomChucNang;
    }

    public String getMoTa() {
        return moTa;
    }

    public void setMoTa(String moTa) {
        this.moTa = moTa;
    }
}
