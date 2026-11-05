package model.auth;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class AuthenticatedSession {
    private final long maTaiKhoan;
    private final String tenDangNhap;
    private final String maNhanVien;
    private final String hoTenNhanVien;
    private final List<VaiTro> vaiTro;
    private final List<Quyen> quyen;
    private final Set<String> maQuyen;

    public AuthenticatedSession(
            long maTaiKhoan,
            String tenDangNhap,
            String maNhanVien,
            String hoTenNhanVien,
            List<VaiTro> vaiTro,
            List<Quyen> quyen) {
        this.maTaiKhoan = maTaiKhoan;
        this.tenDangNhap = tenDangNhap;
        this.maNhanVien = maNhanVien;
        this.hoTenNhanVien = hoTenNhanVien;
        this.vaiTro = vaiTro.stream()
                .map(role -> new VaiTro(
                        role.getMaVaiTro(), role.getTenVaiTro(), role.getTrangThai()))
                .toList();
        this.quyen = quyen.stream()
                .map(permission -> new Quyen(
                        permission.getMaQuyen(),
                        permission.getTenQuyen(),
                        permission.getNhomChucNang(),
                        permission.getMoTa()))
                .toList();
        this.maQuyen = this.quyen.stream()
                .map(Quyen::getMaQuyen)
                .collect(Collectors.toUnmodifiableSet());
    }

    public long getAccountId() {
        return maTaiKhoan;
    }

    public String getUsername() {
        return tenDangNhap;
    }

    public String getEmployeeId() {
        return maNhanVien;
    }

    public String getEmployeeName() {
        return hoTenNhanVien;
    }

    public List<VaiTro> getRoles() {
        return vaiTro.stream()
                .map(role -> new VaiTro(
                        role.getMaVaiTro(), role.getTenVaiTro(), role.getTrangThai()))
                .toList();
    }

    public String getDisplayRole() {
        return vaiTro.stream()
                .map(VaiTro::getTenVaiTro)
                .collect(Collectors.joining(", "));
    }

    public List<Quyen> getPermissions() {
        return quyen.stream()
                .map(permission -> new Quyen(
                        permission.getMaQuyen(),
                        permission.getTenQuyen(),
                        permission.getNhomChucNang(),
                        permission.getMoTa()))
                .toList();
    }

    public boolean hasPermission(String permissionCode) {
        return maQuyen.contains(permissionCode);
    }

    public boolean hasAnyPermission(Set<String> permissionCodes) {
        return permissionCodes != null
                && permissionCodes.stream().anyMatch(this::hasPermission);
    }
}
