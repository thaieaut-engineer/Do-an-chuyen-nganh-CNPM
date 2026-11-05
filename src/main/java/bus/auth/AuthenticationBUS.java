package bus.auth;

import dao.auth.AuthenticationDAO;
import dao.auth.AuthenticationDAOException;
import java.util.Arrays;
import java.util.List;
import model.auth.AuthenticatedSession;
import model.auth.NhanVien;
import model.auth.Quyen;
import model.auth.TaiKhoan;
import model.auth.VaiTro;

public final class AuthenticationBUS {
    private static final String INVALID_HASH_MARKER =
            "NOT-A-REAL-HASH-AUTHENTICATION-NOT-IMPLEMENTED";

    private final AuthenticationDAO authenticationDAO;

    public AuthenticationBUS() {
        this(new AuthenticationDAO());
    }

    AuthenticationBUS(AuthenticationDAO authenticationDAO) {
        this.authenticationDAO = authenticationDAO;
    }

    public AuthenticatedSession authenticate(String username, char[] password)
            throws AuthenticationException {
        String normalizedUsername = username == null ? "" : username.trim();
        if (normalizedUsername.isEmpty() || password == null || password.length == 0
                || normalizedUsername.length() > 80) {
            throw new AuthenticationException(
                    AuthenticationException.Reason.INVALID_INPUT,
                    "Nhập tên đăng nhập và mật khẩu hợp lệ.");
        }

        try {
            AuthenticationDAO.AuthenticationData data =
                    authenticationDAO.findAuthenticationData(normalizedUsername);
            if (data == null) {
                throw deny(null, normalizedUsername,
                        AuthenticationException.Reason.INVALID_CREDENTIALS,
                        "Tên đăng nhập hoặc mật khẩu không chính xác.",
                        "Không tìm thấy tài khoản.");
            }

            TaiKhoan account = data.taiKhoan();
            checkAccountStatus(account, normalizedUsername);

            NhanVien employee = data.nhanVien();
            if (employee == null || !isEmployed(employee.getTrangThaiCongTac())) {
                throw deny(account.getMaTaiKhoan(), normalizedUsername,
                        AuthenticationException.Reason.EMPLOYEE_INACTIVE,
                        "Nhân viên không còn trạng thái công tác phù hợp để đăng nhập.",
                        "Nhân viên không hoạt động hoặc chưa được gán.");
            }

            if (account.getMatKhauHash() == null
                    || INVALID_HASH_MARKER.equals(account.getMatKhauHash())) {
                throw deny(account.getMaTaiKhoan(), normalizedUsername,
                        AuthenticationException.Reason.PASSWORD_NOT_CONFIGURED,
                        "Tài khoản chưa được thiết lập mật khẩu hợp lệ. Vui lòng liên hệ quản trị viên.",
                        "Hash mật khẩu chưa được thiết lập.");
            }

            final boolean passwordMatches;
            try {
                passwordMatches = PasswordHasher.verify(password, account.getMatKhauHash());
            } catch (IllegalArgumentException exception) {
                throw deny(account.getMaTaiKhoan(), normalizedUsername,
                        AuthenticationException.Reason.PASSWORD_NOT_CONFIGURED,
                        "Thông tin xác thực tài khoản chưa hợp lệ. Vui lòng liên hệ quản trị viên.",
                        "Định dạng hash mật khẩu không được hỗ trợ.");
            }
            if (!passwordMatches) {
                throw deny(account.getMaTaiKhoan(), normalizedUsername,
                        AuthenticationException.Reason.INVALID_CREDENTIALS,
                        "Tên đăng nhập hoặc mật khẩu không chính xác.",
                        "Mật khẩu không chính xác.");
            }

            List<VaiTro> roles = data.vaiTro();
            List<Quyen> permissions = data.quyen();
            if (roles.isEmpty()) {
                throw deny(account.getMaTaiKhoan(), normalizedUsername,
                        AuthenticationException.Reason.NO_ACTIVE_ROLE,
                        "Tài khoản chưa được gán vai trò đang hoạt động. Vui lòng liên hệ quản trị viên.",
                        "Không có vai trò đang hoạt động.");
            }
            if (permissions.isEmpty()) {
                throw deny(account.getMaTaiKhoan(), normalizedUsername,
                        AuthenticationException.Reason.NO_PERMISSION,
                        "Vai trò của tài khoản chưa được cấp quyền truy cập.",
                        "Không có quyền được cấp.");
            }
            if (!authenticationDAO.completeSuccessfulLogin(
                    account.getMaTaiKhoan(), normalizedUsername)) {
                throw deny(account.getMaTaiKhoan(), normalizedUsername,
                        AuthenticationException.Reason.ACCOUNT_DISABLED,
                        "Tài khoản hoặc trạng thái công tác đã thay đổi. Vui lòng đăng nhập lại.",
                        "Trạng thái tài khoản/nhân viên thay đổi trong lúc đăng nhập.");
            }

            return new AuthenticatedSession(
                    account.getMaTaiKhoan(),
                    account.getTenDangNhap(),
                    employee.getMaNhanVien(),
                    employee.getHoTen(),
                    roles,
                    permissions);
        } catch (AuthenticationDAOException exception) {
            throw new AuthenticationException(
                    AuthenticationException.Reason.DATABASE_ERROR,
                    "Không thể kết nối hoặc truy vấn cơ sở dữ liệu. Kiểm tra cấu hình SQL Server rồi thử lại.",
                    exception);
        }
    }

    public void changePassword(
            AuthenticatedSession session,
            char[] currentPassword,
            char[] newPassword,
            char[] confirmation) throws AuthenticationException {
        requireActiveSession(session);
        synchronized (session) {
            changePasswordForSession(session, currentPassword, newPassword, confirmation);
        }
    }

    private void changePasswordForSession(
            AuthenticatedSession session,
            char[] currentPassword,
            char[] newPassword,
            char[] confirmation) throws AuthenticationException {
        requireActiveSession(session);
        if (currentPassword == null || currentPassword.length == 0
                || newPassword == null || newPassword.length == 0
                || confirmation == null || confirmation.length == 0) {
            throw new AuthenticationException(
                    AuthenticationException.Reason.INVALID_INPUT,
                    "Vui lòng nhập đầy đủ mật khẩu hiện tại, mật khẩu mới và xác nhận.");
        }
        if (!Arrays.equals(newPassword, confirmation)) {
            throw new AuthenticationException(
                    AuthenticationException.Reason.PASSWORD_CONFIRMATION_MISMATCH,
                    "Mật khẩu mới và xác nhận mật khẩu không trùng khớp.");
        }
        if (newPassword.length < 8) {
            throw new AuthenticationException(
                    AuthenticationException.Reason.PASSWORD_POLICY_VIOLATION,
                    "Mật khẩu mới phải có ít nhất 8 ký tự.");
        }

        try {
            long accountId = session.getAccountId();
            String currentHash = authenticationDAO.findPasswordHash(accountId);
            if (currentHash == null) {
                session.revoke();
                throw new AuthenticationException(
                        AuthenticationException.Reason.SESSION_EXPIRED,
                        "Tài khoản không còn hoạt động. Vui lòng đăng nhập lại.");
            }

            final boolean currentPasswordMatches;
            try {
                currentPasswordMatches = PasswordHasher.verify(currentPassword, currentHash);
            } catch (IllegalArgumentException exception) {
                authenticationDAO.recordFailedPasswordChange(
                        accountId, "Không thể xác minh hash mật khẩu hiện tại.");
                throw new AuthenticationException(
                        AuthenticationException.Reason.PASSWORD_NOT_CONFIGURED,
                        "Thông tin xác thực tài khoản chưa hợp lệ. Vui lòng liên hệ quản trị viên.");
            }
            if (!currentPasswordMatches) {
                authenticationDAO.recordFailedPasswordChange(
                        accountId, "Xác minh mật khẩu hiện tại không thành công.");
                throw new AuthenticationException(
                        AuthenticationException.Reason.CURRENT_PASSWORD_INCORRECT,
                        "Mật khẩu hiện tại không chính xác.");
            }
            if (Arrays.equals(currentPassword, newPassword)) {
                throw new AuthenticationException(
                        AuthenticationException.Reason.PASSWORD_POLICY_VIOLATION,
                        "Mật khẩu mới phải khác mật khẩu hiện tại.");
            }

            String newHash = PasswordHasher.hash(newPassword);
            if (!session.isActive()) {
                throw new AuthenticationException(
                        AuthenticationException.Reason.SESSION_EXPIRED,
                        "Phiên đăng nhập đã kết thúc. Vui lòng đăng nhập lại.");
            }
            if (!authenticationDAO.changePassword(accountId, currentHash, newHash)) {
                throw new AuthenticationException(
                        AuthenticationException.Reason.SESSION_EXPIRED,
                        "Thông tin tài khoản đã thay đổi. Vui lòng đăng nhập lại.");
            }
        } catch (AuthenticationDAOException exception) {
            throw new AuthenticationException(
                    AuthenticationException.Reason.DATABASE_ERROR,
                    "Không thể đổi mật khẩu do lỗi truy cập cơ sở dữ liệu. Vui lòng thử lại.",
                    exception);
        }
    }

    public void logout(AuthenticatedSession session) throws AuthenticationException {
        if (session == null) {
            return;
        }
        synchronized (session) {
            if (!session.isActive()) {
                return;
            }
            long accountId = session.getAccountId();
            String username = session.getUsername();
            try {
                authenticationDAO.recordLogout(accountId, username);
            } catch (AuthenticationDAOException exception) {
                throw new AuthenticationException(
                        AuthenticationException.Reason.DATABASE_ERROR,
                        "Đã kết thúc phiên đăng nhập nhưng không thể ghi nhật ký đăng xuất.",
                        exception);
            } finally {
                session.revoke();
            }
        }
    }

    private void requireActiveSession(AuthenticatedSession session)
            throws AuthenticationException {
        if (session == null || !session.isActive() || session.getAccountId() <= 0) {
            throw new AuthenticationException(
                    AuthenticationException.Reason.SESSION_EXPIRED,
                    "Phiên đăng nhập đã kết thúc. Vui lòng đăng nhập lại.");
        }
    }

    private void checkAccountStatus(TaiKhoan account, String username)
            throws AuthenticationException {
        switch (account.getTrangThai() == null ? "" : account.getTrangThai()) {
            case "HoatDong":
                return;
            case "Khoa":
                throw deny(account.getMaTaiKhoan(), username,
                        AuthenticationException.Reason.ACCOUNT_LOCKED,
                        "Tài khoản đang bị khóa. Vui lòng liên hệ quản trị viên.",
                        "Tài khoản bị khóa.");
            case "ChoKichHoat":
                throw deny(account.getMaTaiKhoan(), username,
                        AuthenticationException.Reason.ACCOUNT_PENDING,
                        "Tài khoản chưa được kích hoạt.",
                        "Tài khoản chưa kích hoạt.");
            case "ThuHoi":
                throw deny(account.getMaTaiKhoan(), username,
                        AuthenticationException.Reason.ACCOUNT_REVOKED,
                        "Tài khoản đã bị thu hồi.",
                        "Tài khoản đã bị thu hồi.");
            default:
                throw deny(account.getMaTaiKhoan(), username,
                        AuthenticationException.Reason.ACCOUNT_DISABLED,
                        "Tài khoản không ở trạng thái được phép đăng nhập.",
                        "Trạng thái tài khoản không hợp lệ.");
        }
    }

    private boolean isEmployed(String employeeStatus) {
        return "DangLam".equals(employeeStatus) || "ThuViec".equals(employeeStatus);
    }

    private AuthenticationException deny(
            Long accountId,
            String username,
            AuthenticationException.Reason reason,
            String userMessage,
            String auditMessage) {
        authenticationDAO.recordFailedLogin(accountId, username, auditMessage);
        return new AuthenticationException(reason, userMessage);
    }
}
