package dao.auth;

import config.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import model.auth.NhanVien;
import model.auth.Quyen;
import model.auth.TaiKhoan;
import model.auth.VaiTro;

public final class AuthenticationDAO {
    public AuthenticationData findAuthenticationData(String username) {
        try {
            try (Connection connection = DatabaseConnection.getConnection()) {
                TaiKhoan account;
                NhanVien employee;
                String accountSql = "SELECT a.MaTaiKhoan, a.TenDangNhap, a.MatKhauHash, "
                        + "a.MaNhanVien, a.TrangThai, n.HoTen, n.TrangThaiCongTac "
                        + "FROM dbo.TaiKhoan a "
                        + "LEFT JOIN dbo.NhanVien n ON n.MaNhanVien = a.MaNhanVien "
                        + "WHERE a.TenDangNhap = ?";
                try (PreparedStatement statement = connection.prepareStatement(accountSql)) {
                    statement.setString(1, username);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            return null;
                        }
                        account = new TaiKhoan(
                                result.getLong("MaTaiKhoan"),
                                result.getString("TenDangNhap"),
                                result.getString("MatKhauHash"),
                                result.getString("MaNhanVien"),
                                result.getString("TrangThai"));

                        String employeeId = result.getString("MaNhanVien");
                        String employeeName = result.getString("HoTen");
                        String employeeStatus = result.getString("TrangThaiCongTac");
                        employee = employeeId == null || employeeName == null
                                ? null
                                : new NhanVien(employeeId, employeeName, employeeStatus);
                    }
                }

                List<VaiTro> roles = findActiveRoles(connection, account.getMaTaiKhoan());
                List<Quyen> permissions = findPermissions(connection, account.getMaTaiKhoan());
                return new AuthenticationData(account, employee, roles, permissions);
            }
        } catch (SQLException exception) {
            throw new AuthenticationDAOException("Không thể đọc dữ liệu xác thực.", exception);
        }
    }

    public boolean completeSuccessfulLogin(long accountId, String username) {
        try {
            try (Connection connection = DatabaseConnection.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    String updateSql = "UPDATE a "
                            + "SET LanDangNhapCuoi = SYSDATETIME() "
                            + "FROM dbo.TaiKhoan a "
                            + "WHERE a.MaTaiKhoan = ? AND a.TrangThai = N'HoatDong' "
                            + "AND EXISTS (SELECT 1 FROM dbo.NhanVien n "
                            + "WHERE n.MaNhanVien = a.MaNhanVien "
                            + "AND n.TrangThaiCongTac IN (N'DangLam', N'ThuViec'))";
                    try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
                        statement.setLong(1, accountId);
                        if (statement.executeUpdate() != 1) {
                            connection.rollback();
                            return false;
                        }
                    }
                    insertLoginAudit(connection, accountId, username, true,
                            "Đăng nhập thành công.");
                    connection.commit();
                    return true;
                } catch (SQLException exception) {
                    rollback(connection, exception);
                    throw exception;
                }
            }
        } catch (SQLException exception) {
            throw new AuthenticationDAOException("Không thể ghi nhận đăng nhập thành công.", exception);
        }
    }

    public void recordFailedLogin(Long accountId, String username, String description) {
        try {
            try (Connection connection = DatabaseConnection.getConnection()) {
                connection.setAutoCommit(false);
                try {
                    insertLoginAudit(connection, accountId, username, false, description);
                    connection.commit();
                } catch (SQLException exception) {
                    rollback(connection, exception);
                    throw exception;
                }
            }
        } catch (SQLException exception) {
            throw new AuthenticationDAOException("Không thể ghi nhật ký đăng nhập thất bại.", exception);
        }
    }

    public String findPasswordHash(long accountId) {
        String sql = "SELECT MatKhauHash FROM dbo.TaiKhoan "
                + "WHERE MaTaiKhoan = ? AND TrangThai = N'HoatDong'";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accountId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString("MatKhauHash") : null;
            }
        } catch (SQLException exception) {
            throw new AuthenticationDAOException("Không thể đọc thông tin xác thực tài khoản.", exception);
        }
    }

    public boolean changePassword(long accountId, String currentHash, String newHash) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String sql = "UPDATE a "
                        + "SET MatKhauHash = ? "
                        + "FROM dbo.TaiKhoan a "
                        + "WHERE a.MaTaiKhoan = ? AND a.MatKhauHash = ? "
                        + "AND a.TrangThai = N'HoatDong' "
                        + "AND EXISTS (SELECT 1 FROM dbo.NhanVien n "
                        + "WHERE n.MaNhanVien = a.MaNhanVien "
                        + "AND n.TrangThaiCongTac IN (N'DangLam', N'ThuViec'))";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, newHash);
                    statement.setLong(2, accountId);
                    statement.setString(3, currentHash);
                    if (statement.executeUpdate() != 1) {
                        connection.rollback();
                        return false;
                    }
                }
                insertSecurityAudit(connection, accountId, "DOI_MAT_KHAU_THANH_CONG",
                        "TaiKhoan", Long.toString(accountId), "Đổi mật khẩu thành công.");
                connection.commit();
                return true;
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            throw new AuthenticationDAOException("Không thể cập nhật mật khẩu.", exception);
        }
    }

    public void recordFailedPasswordChange(long accountId, String description) {
        recordSecurityEvent(accountId, "DOI_MAT_KHAU_THAT_BAI", description);
    }

    public void recordLogout(long accountId, String username) {
        recordSecurityEvent(accountId, "DANG_XUAT",
                "Đăng xuất tài khoản " + username + ".");
    }

    private void recordSecurityEvent(long accountId, String action, String description) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                insertSecurityAudit(connection, accountId, action,
                        "TaiKhoan", Long.toString(accountId), description);
                connection.commit();
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            throw new AuthenticationDAOException("Không thể ghi nhật ký bảo mật.", exception);
        }
    }

    private void insertSecurityAudit(
            Connection connection,
            long accountId,
            String action,
            String object,
            String objectId,
            String description) throws SQLException {
        String sql = "INSERT INTO dbo.NhatKyHeThong "
                + "(MaTaiKhoan, HanhDong, DoiTuong, MaDoiTuong, NoiDung) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accountId);
            statement.setNString(2, action);
            statement.setNString(3, object);
            statement.setString(4, objectId);
            statement.setNString(5, description);
            statement.executeUpdate();
        }
    }

    private List<VaiTro> findActiveRoles(Connection connection, long accountId)
            throws SQLException {
        String sql = "SELECT DISTINCT v.MaVaiTro, v.TenVaiTro, v.TrangThai "
                + "FROM dbo.TaiKhoanVaiTro tv "
                + "JOIN dbo.VaiTro v ON v.MaVaiTro = tv.MaVaiTro "
                + "WHERE tv.MaTaiKhoan = ? AND v.TrangThai = N'HoatDong' "
                + "ORDER BY v.TenVaiTro";
        List<VaiTro> roles = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accountId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    roles.add(new VaiTro(
                            result.getString("MaVaiTro"),
                            result.getString("TenVaiTro"),
                            result.getString("TrangThai")));
                }
            }
        }
        return List.copyOf(roles);
    }

    private List<Quyen> findPermissions(Connection connection, long accountId)
            throws SQLException {
        String sql = "SELECT DISTINCT q.MaQuyen, q.TenQuyen, q.NhomChucNang, q.MoTa "
                + "FROM dbo.TaiKhoanVaiTro tv "
                + "JOIN dbo.VaiTro v ON v.MaVaiTro = tv.MaVaiTro "
                + "JOIN dbo.VaiTroQuyen vq ON vq.MaVaiTro = v.MaVaiTro "
                + "JOIN dbo.Quyen q ON q.MaQuyen = vq.MaQuyen "
                + "WHERE tv.MaTaiKhoan = ? AND v.TrangThai = N'HoatDong' "
                + "ORDER BY q.MaQuyen";
        List<Quyen> permissions = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accountId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    permissions.add(new Quyen(
                            result.getString("MaQuyen"),
                            result.getString("TenQuyen"),
                            result.getString("NhomChucNang"),
                            result.getString("MoTa")));
                }
            }
        }
        return List.copyOf(permissions);
    }

    private void insertLoginAudit(
            Connection connection,
            Long accountId,
            String username,
            boolean successful,
            String description) throws SQLException {
        String sql = "INSERT INTO dbo.NhatKyHeThong "
                + "(MaTaiKhoan, HanhDong, DoiTuong, MaDoiTuong, NoiDung) "
                + "VALUES (?, ?, N'TaiKhoan', ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (accountId == null) {
                statement.setNull(1, Types.BIGINT);
            } else {
                statement.setLong(1, accountId);
            }
            statement.setNString(2,
                    successful ? "DANG_NHAP_THANH_CONG" : "DANG_NHAP_THAT_BAI");
            statement.setString(3, username);
            statement.setNString(4, description);
            statement.executeUpdate();
        }
    }

    private void rollback(Connection connection, SQLException originalException) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            originalException.addSuppressed(rollbackException);
        }
    }

    public record AuthenticationData(
            TaiKhoan taiKhoan,
            NhanVien nhanVien,
            List<VaiTro> vaiTro,
            List<Quyen> quyen) {
        public AuthenticationData {
            vaiTro = List.copyOf(vaiTro);
            quyen = List.copyOf(quyen);
        }
    }
}
