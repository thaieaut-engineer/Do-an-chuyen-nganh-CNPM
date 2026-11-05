package bus.auth;

import dao.auth.AuthenticationDAO;
import dao.auth.AuthenticationDAOException;
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
