package model.auth;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public final class AuthenticatedSession {
    private final AtomicBoolean active = new AtomicBoolean(true);
    private volatile long maTaiKhoan;
    private volatile String tenDangNhap;
    private volatile String maNhanVien;
    private volatile String hoTenNhanVien;
    private volatile List<VaiTro> vaiTro;
    private volatile List<Quyen> quyen;
    private volatile Set<String> maQuyen;

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
        return isActive() && permissionCode != null && maQuyen.contains(permissionCode);
    }

    public boolean hasAnyPermission(Set<String> permissionCodes) {
        return isActive() && permissionCodes != null
                && permissionCodes.stream().anyMatch(this::hasPermission);
    }

    public boolean isActive() {
        return active.get();
    }

    public synchronized void revoke() {
        if (active.compareAndSet(true, false)) {
            maTaiKhoan = 0;
            tenDangNhap = "";
            maNhanVien = "";
            hoTenNhanVien = "";
            vaiTro = List.of();
            quyen = List.of();
            maQuyen = Set.of();
        }
    }
}
