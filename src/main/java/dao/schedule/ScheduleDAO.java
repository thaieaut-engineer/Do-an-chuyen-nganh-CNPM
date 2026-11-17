package dao.schedule;

import config.DatabaseConnection;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import model.schedule.ScheduleModels.*;

public final class ScheduleDAO {
    public List<Doctor> doctors(){
        String sql="SELECT bs.MaNhanVien,n.HoTen,COALESCE(c.TenChuyenKhoa,N'Chưa phân chuyên khoa') FROM dbo.BacSi bs JOIN dbo.NhanVien n ON n.MaNhanVien=bs.MaNhanVien LEFT JOIN dbo.ChuyenKhoa c ON c.MaChuyenKhoa=bs.MaChuyenKhoa WHERE n.TrangThaiCongTac IN ('ThuViec','DangLam') ORDER BY n.HoTen";List<Doctor> rows=new ArrayList<>();
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql);ResultSet r=s.executeQuery()){while(r.next())rows.add(new Doctor(r.getString(1),r.getString(2),r.getString(3)));return rows;}catch(SQLException e){throw err("tải bác sĩ",e);}
    }
    public List<Shift> shifts(){
        String sql="SELECT MaCa,TenCa,GioBatDau,GioKetThuc FROM dbo.CaLamViec WHERE TrangThai='HoatDong' ORDER BY GioBatDau";List<Shift> rows=new ArrayList<>();
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql);ResultSet r=s.executeQuery()){while(r.next())rows.add(new Shift(r.getString(1),r.getString(2),r.getTime(3).toLocalTime(),r.getTime(4).toLocalTime()));return rows;}catch(SQLException e){throw err("tải ca làm việc",e);}
    }
    public Page<Assignment> assignments(String q,LocalDate from,LocalDate to,int page,int size){
        String where=" WHERE 1=1 ";List<Object> p=new ArrayList<>();if(q!=null&&!q.isBlank()){where+=" AND (lp.MaNhanVien LIKE ? OR n.HoTen LIKE ? OR lp.DiaDiem LIKE ?)";String x="%"+q.trim()+"%";p.add(x);p.add(x);p.add(x);}if(from!=null){where+=" AND lp.NgayLam>=?";p.add(Date.valueOf(from));}if(to!=null){where+=" AND lp.NgayLam<=?";p.add(Date.valueOf(to));}
        String fromSql=" FROM dbo.LichPhanCong lp JOIN dbo.NhanVien n ON n.MaNhanVien=lp.MaNhanVien JOIN dbo.BacSi bs ON bs.MaNhanVien=n.MaNhanVien LEFT JOIN dbo.ChuyenKhoa ck ON ck.MaChuyenKhoa=bs.MaChuyenKhoa JOIN dbo.CaLamViec ca ON ca.MaCa=lp.MaCa";
        long total=count("SELECT COUNT_BIG(*)"+fromSql+where,p);String sql="SELECT lp.MaPhanCong,lp.MaNhanVien,n.HoTen,COALESCE(ck.TenChuyenKhoa,N'Chưa phân chuyên khoa'),lp.MaCa,ca.TenCa,lp.NgayLam,ca.GioBatDau,ca.GioKetThuc,lp.DiaDiem,lp.TrangThai,(SELECT COUNT_BIG(*) FROM dbo.KhungGioKham kg WHERE kg.MaNhanVien=lp.MaNhanVien AND kg.NgayKham=lp.NgayLam),(SELECT COUNT_BIG(*) FROM dbo.LichHen lh JOIN dbo.KhungGioKham kg ON kg.MaKhungGio=lh.MaKhungGio WHERE kg.MaNhanVien=lp.MaNhanVien AND kg.NgayKham=lp.NgayLam AND lh.TrangThai NOT IN ('DaHuy','VangMat'))"+fromSql+where+" ORDER BY lp.NgayLam,ca.GioBatDau,n.HoTen OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";p.add(page*size);p.add(size);List<Assignment> rows=new ArrayList<>();
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){bind(s,p);try(ResultSet r=s.executeQuery()){while(r.next())rows.add(new Assignment(r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6),r.getDate(7).toLocalDate(),r.getTime(8).toLocalTime(),r.getTime(9).toLocalTime(),r.getString(10),r.getString(11),r.getLong(12),r.getLong(13)));}return new Page<>(rows,total,page,size);}catch(SQLException e){throw err("tải lịch phân công",e);}
    }
    public void assign(Draft d,String approver){
        try(Connection c=DatabaseConnection.getConnection()){c.setAutoCommit(false);try{
            Time start,end;try(PreparedStatement s=c.prepareStatement("SELECT GioBatDau,GioKetThuc FROM dbo.CaLamViec WHERE MaCa=? AND TrangThai='HoatDong'")){s.setString(1,d.shiftId());try(ResultSet r=s.executeQuery()){if(!r.next())throw new ScheduleDAOException("Ca làm việc không tồn tại hoặc đã ngừng hoạt động.");start=r.getTime(1);end=r.getTime(2);}}
            try(PreparedStatement s=c.prepareStatement("SELECT 1 FROM dbo.LichPhanCong WITH (UPDLOCK,HOLDLOCK) WHERE MaNhanVien=? AND MaCa=? AND NgayLam=? AND TrangThai<>'DaHuy'")){s.setString(1,d.doctorId());s.setString(2,d.shiftId());s.setDate(3,Date.valueOf(d.workDate()));try(ResultSet r=s.executeQuery()){if(r.next())throw new ScheduleDAOException("Bác sĩ đã được phân công ca này trong ngày.");}}
            long assignment=insert(c,"INSERT INTO dbo.LichPhanCong(MaNhanVien,MaCa,NgayLam,DiaDiem,TrangThai,MaNguoiDuyet,ThoiDiemDuyet,GhiChu) VALUES(?,?,?,?,'DaXacNhan',?,SYSDATETIME(),?)",d.doctorId(),d.shiftId(),Date.valueOf(d.workDate()),d.location(),approver,d.note());
            int duration=30;for(LocalTime time=start.toLocalTime();time.plusMinutes(duration).compareTo(end.toLocalTime())<=0;time=time.plusMinutes(duration)){try(PreparedStatement s=c.prepareStatement("IF NOT EXISTS (SELECT 1 FROM dbo.KhungGioKham WHERE MaNhanVien=? AND NgayKham=CAST(? AS date) AND GioBatDau=CAST(? AS time)) INSERT INTO dbo.KhungGioKham(MaNhanVien,NgayKham,GioBatDau,GioKetThuc,SoLuongToiDa,TrangThai) VALUES(?,CAST(? AS date),CAST(? AS time),CAST(? AS time),1,'Mo')")){s.setString(1,d.doctorId());s.setDate(2,Date.valueOf(d.workDate()));s.setTime(3,Time.valueOf(time));s.setString(4,d.doctorId());s.setDate(5,Date.valueOf(d.workDate()));s.setTime(6,Time.valueOf(time));s.setTime(7,Time.valueOf(time.plusMinutes(duration)));s.executeUpdate();}}
            c.commit();
        }catch(Exception e){try{c.rollback();}catch(SQLException ignored){}if(e instanceof ScheduleDAOException x)throw x;throw err("phân công bác sĩ",e);}finally{try{c.setAutoCommit(true);}catch(SQLException ignored){}}}catch(SQLException e){throw err("phân công bác sĩ",e);}
    }
    public void cancel(long assignmentId){
        try(Connection c=DatabaseConnection.getConnection()){c.setAutoCommit(false);try{String sql="UPDATE lp SET lp.TrangThai='DaHuy' FROM dbo.LichPhanCong lp WHERE lp.MaPhanCong=? AND lp.TrangThai NOT IN ('DaHuy','DaKhoa') AND NOT EXISTS (SELECT 1 FROM dbo.LichHen h JOIN dbo.KhungGioKham kg ON kg.MaKhungGio=h.MaKhungGio WHERE kg.MaNhanVien=lp.MaNhanVien AND kg.NgayKham=lp.NgayLam AND h.TrangThai NOT IN ('DaHuy','VangMat'))";try(PreparedStatement s=c.prepareStatement(sql)){s.setLong(1,assignmentId);if(s.executeUpdate()!=1)throw new ScheduleDAOException("Không thể hủy: lịch đã khóa hoặc đã có lịch hẹn.");}c.commit();}catch(Exception e){try{c.rollback();}catch(SQLException ignored){}if(e instanceof ScheduleDAOException x)throw x;throw err("hủy phân công",e);}finally{try{c.setAutoCommit(true);}catch(SQLException ignored){}}}catch(SQLException e){throw err("hủy phân công",e);}
    }
    private long insert(Connection c,String sql,Object...v)throws SQLException{try(PreparedStatement s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){bind(s,List.of(v));s.executeUpdate();try(ResultSet r=s.getGeneratedKeys()){r.next();return r.getLong(1);}}}
    private long count(String sql,List<Object> p){try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){bind(s,p);try(ResultSet r=s.executeQuery()){r.next();return r.getLong(1);}}catch(SQLException e){throw err("đếm phân công",e);}}
    private void bind(PreparedStatement s,List<?> p)throws SQLException{for(int i=0;i<p.size();i++)s.setObject(i+1,p.get(i));}
    private static ScheduleDAOException err(String op,Exception e){return new ScheduleDAOException("Lỗi "+op+": "+e.getMessage(),e);}
}
