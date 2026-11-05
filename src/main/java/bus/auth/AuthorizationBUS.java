package bus.auth;

import java.util.Map;
import java.util.Set;
import model.auth.AuthenticatedSession;

public final class AuthorizationBUS {
    private static final Map<String, Set<String>> PAGE_PERMISSIONS = Map.ofEntries(
            Map.entry("Bệnh nhân", Set.of("BN_XEM")),
            Map.entry("Lịch hẹn", Set.of("LH_QUANLY")),
            Map.entry("Tiếp nhận bệnh nhân", Set.of("BN_SUA")),
            Map.entry("Lượt khám", Set.of("KHAM_HOSO", "LH_QUANLY")),
            Map.entry("Phiếu khám", Set.of("KHAM_HOSO")),
            Map.entry("Chỉ định dịch vụ", Set.of("KHAM_CHIDINH")),
            Map.entry("Kê đơn thuốc", Set.of("DONTHUOC_KE")),
            Map.entry("Danh mục thuốc", Set.of("KHO_XEM")),
            Map.entry("Kho thuốc", Set.of("KHO_XEM")),
            Map.entry("Kho hóa chất", Set.of("KHO_XEM")),
            Map.entry("Viện phí", Set.of("VIENPHI_TINH")),
            Map.entry("Hóa đơn", Set.of("HOADON_QUANLY")),
            Map.entry("Thanh toán", Set.of("THANHTOAN_THU")),
            Map.entry("Bảo hiểm y tế", Set.of("BN_XEM")),
            Map.entry("Nhân viên", Set.of("NHANSU_XEM")),
            Map.entry("Bác sĩ", Set.of("NHANSU_XEM")),
            Map.entry("Lịch làm việc", Set.of("NHANSU_XEM")),
            Map.entry("Thiết bị", Set.of("THIETBI_QUANLY")),
            Map.entry("Đánh giá nhân viên", Set.of("NHANSU_XEM")),
            Map.entry("Tài khoản", Set.of("TAIKHOAN_QUANLY")),
            Map.entry("Phân quyền", Set.of("TAIKHOAN_QUANLY")),
            Map.entry("Cấu hình hệ thống", Set.of("CAUHINH_QUANLY")),
            Map.entry("Nhật ký hệ thống", Set.of("NHATKY_XEM")),
            Map.entry("Thông báo", Set.of("TAIKHOAN_QUANLY")),
            Map.entry("Báo cáo", Set.of("BAOCAO_XEM"))
    );

    public boolean canAccessPage(AuthenticatedSession session, String page) {
        if (session == null || page == null) {
            return false;
        }
        if ("Tổng quan".equals(page)) {
            return true;
        }
        Set<String> requiredPermissions = PAGE_PERMISSIONS.get(page);
        return requiredPermissions != null && session.hasAnyPermission(requiredPermissions);
    }

    public void requirePageAccess(AuthenticatedSession session, String page) {
        if (!canAccessPage(session, page)) {
            throw new SecurityException("Bạn không có quyền truy cập chức năng này.");
        }
    }

    public void requirePermission(AuthenticatedSession session, String permissionCode) {
        if (session == null || !session.hasPermission(permissionCode)) {
            throw new SecurityException("Bạn không có quyền thực hiện thao tác này.");
        }
    }
}
