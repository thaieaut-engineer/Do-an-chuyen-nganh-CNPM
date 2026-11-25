package dao.device;

import config.DatabaseConnection;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import model.device.DeviceModels.Device;
import model.device.DeviceModels.DeviceDraft;
import model.device.DeviceModels.TypeOption;

public final class DeviceDAO {
    public List<Device> find(String keyword) {
        String sql = """
                SELECT t.MaThietBi,t.TenThietBi,t.MaLoaiThietBi,COALESCE(l.TenLoaiThietBi,N''),
                       t.Model,t.SoSerial,t.HangSanXuat,t.NgayMua,t.NgayDuaVaoSuDung,
                       t.ViTri,t.TinhTrang
                FROM dbo.ThietBi t LEFT JOIN dbo.LoaiThietBi l ON l.MaLoaiThietBi=t.MaLoaiThietBi
                WHERE (?='' OR t.MaThietBi LIKE ? OR t.TenThietBi LIKE ? OR t.SoSerial LIKE ?)
                ORDER BY t.TenThietBi
                """;
        List<Device> result = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            s.setString(1, keyword); s.setString(2, "%" + keyword + "%");
            s.setString(3, "%" + keyword + "%"); s.setString(4, "%" + keyword + "%");
            try (ResultSet r = s.executeQuery()) {
                while (r.next()) {
                    result.add(new Device(r.getString(1), r.getString(2), r.getString(3), r.getString(4),
                            r.getString(5), r.getString(6), r.getString(7), date(r.getDate(8)),
                            date(r.getDate(9)), r.getString(10), r.getString(11)));
                }
            }
            return List.copyOf(result);
        } catch (SQLException e) { throw error("tải danh sách thiết bị", e); }
    }

    public List<TypeOption> types() {
        List<TypeOption> result = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection();
             PreparedStatement s = c.prepareStatement("SELECT MaLoaiThietBi,TenLoaiThietBi FROM dbo.LoaiThietBi ORDER BY TenLoaiThietBi");
             ResultSet r = s.executeQuery()) {
            while (r.next()) result.add(new TypeOption(r.getString(1), r.getString(2)));
            return List.copyOf(result);
        } catch (SQLException e) { throw error("tải loại thiết bị", e); }
    }

    public void save(DeviceDraft d) {
        String existsSql = "SELECT 1 FROM dbo.ThietBi WHERE MaThietBi=?";
        String insert = "INSERT INTO dbo.ThietBi(MaThietBi,TenThietBi,MaLoaiThietBi,Model,SoSerial,HangSanXuat,NgayMua,NgayDuaVaoSuDung,ViTri,TinhTrang) VALUES(?,?,?,?,?,?,?,?,?,?)";
        String update = "UPDATE dbo.ThietBi SET TenThietBi=?,MaLoaiThietBi=?,Model=?,SoSerial=?,HangSanXuat=?,NgayMua=?,NgayDuaVaoSuDung=?,ViTri=?,TinhTrang=? WHERE MaThietBi=?";
        try (Connection c = DatabaseConnection.getConnection()) {
            boolean exists;
            try (PreparedStatement q = c.prepareStatement(existsSql)) { q.setString(1, d.id()); try (ResultSet r = q.executeQuery()) { exists = r.next(); } }
            try (PreparedStatement s = c.prepareStatement(exists ? update : insert)) {
                int i = 1; if (!exists) s.setString(i++, d.id());
                s.setNString(i++, d.name()); s.setString(i++, nullIfBlank(d.typeId()));
                s.setString(i++, nullIfBlank(d.model())); s.setString(i++, nullIfBlank(d.serial()));
                s.setNString(i++, nullIfBlank(d.manufacturer())); setDate(s, i++, d.purchaseDate());
                setDate(s, i++, d.inUseDate()); s.setNString(i++, nullIfBlank(d.location()));
                s.setString(i++, d.status()); if (exists) s.setString(i, d.id()); s.executeUpdate();
            }
        } catch (SQLException e) { throw error("lưu thiết bị", e); }
    }

    public void updateStatus(String id, String status) {
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement("UPDATE dbo.ThietBi SET TinhTrang=? WHERE MaThietBi=?")) {
            s.setString(1, status); s.setString(2, id);
            if (s.executeUpdate() != 1) throw new DeviceDAOException("Không tìm thấy thiết bị cần cập nhật.", null);
        } catch (SQLException e) { throw error("cập nhật tình trạng thiết bị", e); }
    }

    private static void setDate(PreparedStatement s, int i, java.time.LocalDate value) throws SQLException {
        if (value == null) s.setNull(i, Types.DATE); else s.setDate(i, Date.valueOf(value));
    }
    private static java.time.LocalDate date(Date value) { return value == null ? null : value.toLocalDate(); }
    private static String nullIfBlank(String value) { return value == null || value.isBlank() ? null : value; }
    private static DeviceDAOException error(String operation, Exception e) { return new DeviceDAOException("Không thể " + operation + ". Chi tiết hệ thống: " + e.getMessage(), e); }
}
