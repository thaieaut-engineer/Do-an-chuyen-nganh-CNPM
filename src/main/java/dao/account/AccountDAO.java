package dao.account;

import config.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import model.account.AccountModels.AccountDraft;
import model.account.AccountModels.AccountRow;
import model.account.AccountModels.EmployeeOption;
import model.account.AccountModels.RoleOption;

public final class AccountDAO {
    public List<AccountRow> find(String keyword, String status) {
        String sql = "SELECT a.MaTaiKhoan, a.TenDangNhap, a.MaNhanVien, n.HoTen, "
                + "a.TrangThai, a.NgayTao, a.LanDangNhapCuoi, "
                + "STUFF((SELECT N', ' + v2.TenVaiTro FROM dbo.TaiKhoanVaiTro tv2 "
                + "JOIN dbo.VaiTro v2 ON v2.MaVaiTro=tv2.MaVaiTro "
                + "WHERE tv2.MaTaiKhoan=a.MaTaiKhoan ORDER BY v2.TenVaiTro "
                + "FOR XML PATH(''), TYPE).value('.', 'nvarchar(max)'),1,2,N'') AS VaiTro "
                + "FROM dbo.TaiKhoan a LEFT JOIN dbo.NhanVien n ON n.MaNhanVien=a.MaNhanVien "
                + "WHERE (?='' OR a.TenDangNhap LIKE ? OR a.MaNhanVien LIKE ? OR n.HoTen LIKE ?) "
                + "AND (?='' OR a.TrangThai=?) ORDER BY a.TenDangNhap";
        List<AccountRow> rows = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            String q = keyword == null ? "" : keyword.trim();
            s.setString(1, q); s.setString(2, "%" + q + "%"); s.setString(3, "%" + q + "%"); s.setString(4, "%" + q + "%");
            String st = status == null ? "" : status;
            s.setString(5, st); s.setString(6, st);
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) rows.add(new AccountRow(r.getLong("MaTaiKhoan"), r.getString("TenDangNhap"),
                        r.getString("MaNhanVien"), r.getString("HoTen"), r.getString("VaiTro"), r.getString("TrangThai"),
                        toLocalDateTime(r.getTimestamp("NgayTao")), toLocalDateTime(r.getTimestamp("LanDangNhapCuoi"))));
            }
            return List.copyOf(rows);
        } catch (SQLException e) { throw new AccountDAOException("Không thể tải danh sách tài khoản.", e); }
    }

    public List<RoleOption> findRoles() {
        String sql = "SELECT MaVaiTro, TenVaiTro, TrangThai FROM dbo.VaiTro WHERE TrangThai=N'HoatDong' ORDER BY TenVaiTro";
        List<RoleOption> result = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql); ResultSet r = s.executeQuery()) {
            while (r.next()) result.add(new RoleOption(r.getString(1), r.getString(2), r.getString(3)));
            return List.copyOf(result);
        } catch (SQLException e) { throw new AccountDAOException("Không thể tải danh sách vai trò.", e); }
    }

    public List<EmployeeOption> findEmployees() {
        return findEmployees(0);
    }

    public List<EmployeeOption> findEmployees(long accountId) {
        String sql = "SELECT n.MaNhanVien,n.HoTen,n.TrangThaiCongTac FROM dbo.NhanVien n "
                + "WHERE n.TrangThaiCongTac IN (N'DangLam',N'ThuViec') AND NOT EXISTS "
                + "(SELECT 1 FROM dbo.TaiKhoan a WHERE a.MaNhanVien=n.MaNhanVien AND a.MaTaiKhoan<>?) "
                + "ORDER BY n.HoTen";
        List<EmployeeOption> result = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setLong(1, accountId);
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) result.add(new EmployeeOption(r.getString(1), r.getString(2), r.getString(3)));
            }
            return List.copyOf(result);
        } catch (SQLException e) { throw new AccountDAOException("Không thể tải danh sách nhân viên. Chi tiết hệ thống: " + e.getMessage(), e); }
    }

    public long insert(AccountDraft d, String hash, long actorId) { return save(d, hash, actorId, true); }
    public void update(AccountDraft d, String hash, long actorId) { save(d, hash, actorId, false); }

    private long save(AccountDraft d, String hash, long actorId, boolean create) {
        try (Connection c = DatabaseConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                long id = d.id();
                if (create) {
                    String sql = "INSERT INTO dbo.TaiKhoan(TenDangNhap,MatKhauHash,MaNhanVien,TrangThai) VALUES(?,?,?,?)";
                    try (PreparedStatement s = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                        s.setString(1,d.username()); s.setString(2,hash); s.setString(3,d.employeeId()); s.setNString(4,d.status()); s.executeUpdate();
                        try (ResultSet r=s.getGeneratedKeys()) { if(!r.next()) throw new SQLException("Không lấy được mã tài khoản."); id=r.getLong(1); }
                    }
                } else {
                    String sql = hash == null ? "UPDATE dbo.TaiKhoan SET TenDangNhap=?,MaNhanVien=?,TrangThai=? WHERE MaTaiKhoan=?"
                            : "UPDATE dbo.TaiKhoan SET TenDangNhap=?,MaNhanVien=?,TrangThai=?,MatKhauHash=? WHERE MaTaiKhoan=?";
                    try (PreparedStatement s=c.prepareStatement(sql)) {
                        s.setString(1,d.username()); s.setString(2,d.employeeId()); s.setNString(3,d.status());
                        if(hash==null) s.setLong(4,id); else { s.setString(4,hash); s.setLong(5,id); }
                        if(s.executeUpdate()!=1) throw new SQLException("Tài khoản không tồn tại.");
                    }
                }
                try (PreparedStatement s=c.prepareStatement("DELETE FROM dbo.TaiKhoanVaiTro WHERE MaTaiKhoan=?")) { s.setLong(1,id); s.executeUpdate(); }
                try (PreparedStatement s=c.prepareStatement("INSERT INTO dbo.TaiKhoanVaiTro(MaTaiKhoan,MaVaiTro,NguoiGan) VALUES(?,?,?)")) {
                    for(String role:d.roleIds()) { s.setLong(1,id); s.setString(2,role); s.setLong(3,actorId); s.addBatch(); }
                    s.executeBatch();
                }
                audit(c, actorId, create ? "TAO_TAI_KHOAN" : "CAP_NHAT_TAI_KHOAN", id);
                c.commit(); return id;
            } catch(SQLException e) { try { c.rollback(); } catch(SQLException x) { e.addSuppressed(x); } throw e; }
        } catch(SQLException e) { throw new AccountDAOException("Không thể lưu tài khoản. Chi tiết hệ thống: " + e.getMessage(), e); }
    }

    public void changeStatus(long id, String status, long actorId) {
        try (Connection c=DatabaseConnection.getConnection(); PreparedStatement s=c.prepareStatement("UPDATE dbo.TaiKhoan SET TrangThai=? WHERE MaTaiKhoan=?")) {
            s.setNString(1,status); s.setLong(2,id); if(s.executeUpdate()!=1) throw new SQLException("Tài khoản không tồn tại.");
        } catch(SQLException e) { throw new AccountDAOException("Không thể cập nhật trạng thái tài khoản.",e); }
    }

    private void audit(Connection c,long actor,String action,long id)throws SQLException {
        try(PreparedStatement s=c.prepareStatement("INSERT INTO dbo.NhatKyHeThong(MaTaiKhoan,HanhDong,DoiTuong,MaDoiTuong,NoiDung) VALUES(?, ?, N'TaiKhoan', ?, ?)")){
            s.setLong(1,actor);s.setNString(2,action);s.setString(3,Long.toString(id));s.setNString(4,action);s.executeUpdate();
        }
    }
    private static java.time.LocalDateTime toLocalDateTime(Timestamp t){return t==null?null:t.toLocalDateTime();}
}
