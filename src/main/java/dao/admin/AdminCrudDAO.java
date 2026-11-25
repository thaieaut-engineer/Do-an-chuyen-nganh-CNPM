package dao.admin;

import config.DatabaseConnection;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import model.admin.AdminCrudModels.Config;
import model.admin.AdminCrudModels.Evaluation;
import model.admin.AdminCrudModels.EvaluationDraft;
import model.admin.AdminCrudModels.Notification;
import model.admin.AdminCrudModels.NotificationDraft;

public final class AdminCrudDAO {
    public List<Evaluation> evaluations(String keyword) {
        String sql = "SELECT d.MaDanhGia,d.MaNhanVien,n.HoTen,d.NguoiDanhGia,e.HoTen,d.KyDanhGia,d.NgayDanhGia,d.Diem,d.NhanXet FROM dbo.DanhGiaNhanVien d JOIN dbo.NhanVien n ON n.MaNhanVien=d.MaNhanVien JOIN dbo.NhanVien e ON e.MaNhanVien=d.NguoiDanhGia WHERE (?='' OR n.HoTen LIKE ? OR n.MaNhanVien LIKE ? OR d.KyDanhGia LIKE ?) ORDER BY d.NgayDanhGia DESC";
        List<Evaluation> rows = new ArrayList<>();
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            bindSearch(s, keyword, 1, 2, 3, 4);
            try (ResultSet r = s.executeQuery()) { while (r.next()) rows.add(new Evaluation(r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6),date(r.getDate(7)),r.getBigDecimal(8),r.getString(9))); }
            return List.copyOf(rows);
        } catch (SQLException e) { throw error("tải đánh giá nhân viên", e); }
    }

    public void saveEvaluation(EvaluationDraft d, String reviewerId) {
        String sql = d.id() == null ? "INSERT INTO dbo.DanhGiaNhanVien(MaNhanVien,NguoiDanhGia,KyDanhGia,NgayDanhGia,Diem,NhanXet) VALUES(?,?,?,?,?,?)" : "UPDATE dbo.DanhGiaNhanVien SET MaNhanVien=?,KyDanhGia=?,NgayDanhGia=?,Diem=?,NhanXet=? WHERE MaDanhGia=?";
        try (Connection c = DatabaseConnection.getConnection(); PreparedStatement s = c.prepareStatement(sql)) {
            int i=1; s.setString(i++, d.employeeId()); if (d.id() == null) s.setString(i++, reviewerId);
            s.setNString(i++, d.period()); setDate(s,i++,d.date()); if(d.score()==null)s.setNull(i++,Types.DECIMAL);else s.setBigDecimal(i++,d.score()); s.setNString(i++,d.comment()); if(d.id()!=null)s.setLong(i,d.id()); s.executeUpdate();
        } catch (SQLException e) { throw error("lưu đánh giá nhân viên", e); }
    }
    public void deleteEvaluation(long id) { execute("DELETE FROM dbo.DanhGiaNhanVien WHERE MaDanhGia=?", id, "xóa đánh giá nhân viên"); }

    public List<Config> configs(String keyword) {
        String sql="SELECT MaCauHinh,GiaTri,KieuDuLieu,MoTa FROM dbo.CauHinhHeThong WHERE (?='' OR MaCauHinh LIKE ? OR MoTa LIKE ?) ORDER BY MaCauHinh"; List<Config> rows=new ArrayList<>();
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){bindSearch(s,keyword,1,2,3);try(ResultSet r=s.executeQuery()){while(r.next())rows.add(new Config(r.getString(1),r.getString(2),r.getString(3),r.getString(4)));}return List.copyOf(rows);}catch(SQLException e){throw error("tải cấu hình hệ thống",e);}
    }
    public void saveConfig(Config d, long accountId) {
        String sql="MERGE dbo.CauHinhHeThong AS t USING (SELECT ? AS MaCauHinh) AS s ON t.MaCauHinh=s.MaCauHinh WHEN MATCHED THEN UPDATE SET GiaTri=?,KieuDuLieu=?,MoTa=?,CapNhatLuc=SYSDATETIME(),CapNhatBoi=? WHEN NOT MATCHED THEN INSERT(MaCauHinh,GiaTri,KieuDuLieu,MoTa,CapNhatBoi) VALUES(?,?,?,?,?);";
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){s.setString(1,d.id());s.setNString(2,d.value());s.setString(3,d.type());s.setNString(4,d.description());s.setLong(5,accountId);s.setString(6,d.id());s.setNString(7,d.value());s.setString(8,d.type());s.setNString(9,d.description());s.setLong(10,accountId);s.executeUpdate();}catch(SQLException e){throw error("lưu cấu hình hệ thống",e);}
    }
    public void deleteConfig(String id) { execute("DELETE FROM dbo.CauHinhHeThong WHERE MaCauHinh=?", id, "xóa cấu hình hệ thống"); }

    public List<Notification> notifications(String keyword) {
        String sql="SELECT t.MaThongBao,t.MaNhanVien,t.MaBenhNhan,t.TieuDe,t.NoiDung,t.LoaiThongBao,t.TrangThai,t.KenhGui FROM dbo.ThongBao t WHERE (?='' OR t.TieuDe LIKE ? OR t.NoiDung LIKE ?) ORDER BY t.MaThongBao DESC";List<Notification> rows=new ArrayList<>();
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){bindSearch(s,keyword,1,2,3);try(ResultSet r=s.executeQuery()){while(r.next())rows.add(new Notification(r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6),r.getString(7),r.getString(8)));}return List.copyOf(rows);}catch(SQLException e){throw error("tải thông báo",e);}
    }
    public void saveNotification(NotificationDraft d) {
        String sql=d.id()==null?"INSERT INTO dbo.ThongBao(MaNhanVien,MaBenhNhan,TieuDe,NoiDung,LoaiThongBao,TrangThai,KenhGui) VALUES(?,?,?,?,?,?,?)":"UPDATE dbo.ThongBao SET MaNhanVien=?,MaBenhNhan=?,TieuDe=?,NoiDung=?,LoaiThongBao=?,TrangThai=?,KenhGui=? WHERE MaThongBao=?";
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){int i=1;s.setString(i++,blankToNull(d.employeeId()));s.setString(i++,blankToNull(d.patientId()));s.setNString(i++,d.title());s.setNString(i++,d.content());s.setNString(i++,d.type());s.setNString(i++,d.status());s.setNString(i++,d.channel());if(d.id()!=null)s.setLong(i,d.id());s.executeUpdate();}catch(SQLException e){throw error("lưu thông báo",e);}
    }
    public void deleteNotification(long id) { execute("DELETE FROM dbo.ThongBao WHERE MaThongBao=?", id, "xóa thông báo"); }

    private static void bindSearch(PreparedStatement s,String q,int... indexes)throws SQLException{String k=q==null?"":q.trim();s.setString(indexes[0],k);for(int i=1;i<indexes.length;i++)s.setString(indexes[i],"%"+k+"%");}
    private static void setDate(PreparedStatement s,int i,java.time.LocalDate d)throws SQLException{if(d==null)s.setNull(i,Types.DATE);else s.setDate(i,Date.valueOf(d));}
    private static java.time.LocalDate date(Date d){return d==null?null:d.toLocalDate();}
    private static String blankToNull(String v){return v==null||v.isBlank()?null:v.trim();}
    private static void execute(String sql,Object value,String operation){try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){if(value instanceof Long)s.setLong(1,(Long)value);else s.setString(1,String.valueOf(value));s.executeUpdate();}catch(SQLException e){throw error(operation,e);}}
    private static AdminDAOException error(String operation,Exception e){return new AdminDAOException("Không thể "+operation+". Chi tiết hệ thống: "+e.getMessage(),e);}
}
