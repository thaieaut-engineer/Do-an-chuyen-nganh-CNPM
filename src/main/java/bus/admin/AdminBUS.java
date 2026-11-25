package bus.admin;

import bus.auth.AuthorizationBUS;
import dao.admin.AdminDAO;
import model.auth.AuthenticatedSession;

/** Nghiệp vụ tra cứu các phân hệ quản trị hệ thống. */
public final class AdminBUS {
    private final AdminDAO dao;
    private final AuthorizationBUS authorization;

    public AdminBUS() {
        this(new AdminDAO(), new AuthorizationBUS());
    }

    AdminBUS(AdminDAO dao, AuthorizationBUS authorization) {
        this.dao = dao;
        this.authorization = authorization;
    }

    public AdminDAO.Result search(AuthenticatedSession session, String module, String keyword) {
        if (module == null || module.isBlank()) {
            throw new AdminException("Phân hệ quản trị không hợp lệ.");
        }
        try {
            authorization.requirePageAccess(session, module);
        } catch (SecurityException exception) {
            throw new AdminException(exception.getMessage(), exception);
        }

        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.length() > 120) {
            throw new AdminException("Từ khóa tìm kiếm không được vượt quá 120 ký tự.");
        }
        try {
            return dao.find(module, normalizedKeyword);
        } catch (RuntimeException exception) {
            throw new AdminException("Không thể tải dữ liệu quản trị.", exception);
        }
    }
}
