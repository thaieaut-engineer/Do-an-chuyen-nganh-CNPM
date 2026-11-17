package dao.fee;

import config.DatabaseConnection;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import model.fee.FeeModels.*;

public final class FeeDAO {
    public Stats stats(){
        String sql="""
                SELECT (SELECT COUNT_BIG(*) FROM dbo.LuotKham WHERE TrangThai='HoanTat'
                    AND NOT EXISTS (SELECT 1 FROM dbo.PhieuTinhPhi p WHERE p.MaLuotKham=LuotKham.MaLuotKham AND p.TrangThai<>'Huy')),
                       (SELECT COUNT_BIG(*) FROM dbo.PhieuTinhPhi WHERE TrangThai<>'Huy'),
                       COALESCE((SELECT SUM(SoTienPhaiTra) FROM dbo.HoaDon WHERE CAST(NgayLap AS date)=CAST(SYSDATETIME() AS date)),0)
                """;
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql);ResultSet r=s.executeQuery()){r.next();return new Stats(r.getLong(1),r.getLong(2),r.getBigDecimal(3));}catch(SQLException e){throw err("đọc thống kê",e);}
    }
    public Page<Visit> visits(String q,int page,int size){
        String where=" WHERE l.TrangThai='HoanTat' ";List<Object> p=new ArrayList<>();
        if(q!=null&&!q.isBlank()){where+=" AND (b.MaBenhNhan LIKE ? OR b.HoTen LIKE ? OR CAST(l.MaLuotKham AS varchar(30)) LIKE ?)";String x="%"+q.trim()+"%";p.add(x);p.add(x);p.add(x);}
        String from=" FROM dbo.LuotKham l JOIN dbo.BenhNhan b ON b.MaBenhNhan=l.MaBenhNhan LEFT JOIN dbo.BacSi bs ON bs.MaNhanVien=l.MaNhanVien LEFT JOIN dbo.NhanVien n ON n.MaNhanVien=bs.MaNhanVien OUTER APPLY (SELECT TOP(1) p.TrangThai,p.BenhNhanPhaiTra,p.MaPhieuTinhPhi FROM dbo.PhieuTinhPhi p WHERE p.MaLuotKham=l.MaLuotKham ORDER BY p.LanTinh DESC) f";
        long total=count("SELECT COUNT_BIG(*)"+from+where,p);String sql="SELECT l.MaLuotKham,b.MaBenhNhan,b.HoTen,COALESCE(n.HoTen,'Chưa phân công'),l.ThoiGianTiepNhan,l.TrangThai,COALESCE(f.TrangThai,'Chưa tính'),COALESCE(f.BenhNhanPhaiTra,0),COALESCE((SELECT COUNT_BIG(*) FROM dbo.ChiTietPhieuTinhPhi x WHERE x.MaPhieuTinhPhi=f.MaPhieuTinhPhi),0)"+from+where+" ORDER BY l.ThoiGianTiepNhan DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";p.add(page*size);p.add(size);List<Visit> rows=new ArrayList<>();try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){bind(s,p);try(ResultSet r=s.executeQuery()){while(r.next())rows.add(new Visit(r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getTimestamp(5).toLocalDateTime(),r.getString(6),r.getString(7),r.getBigDecimal(8),r.getLong(9)));}return new Page<>(rows,total,page,size);}catch(SQLException e){throw err("tải lượt khám",e);}
    }
    public Detail detail(long feeId){
        String h="SELECT p.MaPhieuTinhPhi,p.MaLuotKham,b.MaBenhNhan,b.HoTen,p.ThoiDiemTinh,p.LanTinh,p.TongTruocGiam,p.TongGiamTru,p.BHYTChiTra,p.BenhNhanPhaiTra,p.TrangThai FROM dbo.PhieuTinhPhi p JOIN dbo.LuotKham l ON l.MaLuotKham=p.MaLuotKham JOIN dbo.BenhNhan b ON b.MaBenhNhan=l.MaBenhNhan WHERE p.MaPhieuTinhPhi=?";
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(h)){s.setLong(1,feeId);try(ResultSet r=s.executeQuery()){if(!r.next())return null;List<Line> lines=new ArrayList<>();try(PreparedStatement x=c.prepareStatement("SELECT LoaiKhoan,MoTa,SoLuong,DonGia,ThanhTien,DuocBHYTChiTra,TyLeBHYT FROM dbo.ChiTietPhieuTinhPhi WHERE MaPhieuTinhPhi=? ORDER BY MaChiTietPhi")){x.setLong(1,feeId);try(ResultSet z=x.executeQuery()){while(z.next())lines.add(new Line(z.getString(1),z.getString(2),z.getBigDecimal(3),z.getBigDecimal(4),z.getBigDecimal(5),z.getBoolean(6),z.getBigDecimal(7)));}}return new Detail(r.getLong(1),r.getLong(2),r.getString(3),r.getString(4),r.getTimestamp(5).toLocalDateTime(),r.getInt(6),r.getBigDecimal(7),r.getBigDecimal(8),r.getBigDecimal(9),r.getBigDecimal(10),r.getString(11),lines);}}catch(SQLException e){throw err("đọc chi tiết phiếu",e);}
    }
    public Detail latestDetailForVisit(long visitId){
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement("SELECT TOP(1) MaPhieuTinhPhi FROM dbo.PhieuTinhPhi WHERE MaLuotKham=? AND TrangThai<>'Huy' ORDER BY LanTinh DESC")){s.setLong(1,visitId);try(ResultSet r=s.executeQuery()){return r.next()?detail(r.getLong(1)):null;}}catch(SQLException e){throw err("đọc phiếu theo lượt khám",e);}
    }
    public Detail calculate(long visitId,String employeeId){
        try(Connection c=DatabaseConnection.getConnection()){c.setAutoCommit(false);try{
            String patient=null;try(PreparedStatement s=c.prepareStatement("SELECT MaBenhNhan FROM dbo.LuotKham WHERE MaLuotKham=? AND TrangThai='HoanTat'")){s.setLong(1,visitId);try(ResultSet r=s.executeQuery()){if(!r.next())throw new FeeDAOException("Lượt khám chưa hoàn tất hoặc không tồn tại.");patient=r.getString(1);}}
            int no=1;try(PreparedStatement s=c.prepareStatement("SELECT COALESCE(MAX(LanTinh),0)+1 FROM dbo.PhieuTinhPhi WHERE MaLuotKham=?")){s.setLong(1,visitId);try(ResultSet r=s.executeQuery()){r.next();no=r.getInt(1);}}
            List<Object[]> lines=new ArrayList<>();String service="""
                    SELECT N'DichVu',d.TenDichVu,CAST(1 AS decimal(12,2)),bg.DonGia,
                           CASE WHEN bh.MaTheBHYT IS NULL THEN 0 ELSE 1 END,COALESCE(bh.MucHuong,0)
                    FROM dbo.LuotKham l JOIN dbo.PhieuKham pk ON pk.MaLuotKham=l.MaLuotKham
                    JOIN dbo.ChiDinhDichVu cd ON cd.MaPhieuKham=pk.MaPhieuKham
                    JOIN dbo.DichVu d ON d.MaDichVu=cd.MaDichVu
                    OUTER APPLY (SELECT TOP(1) ct.DonGia FROM dbo.BangGiaDichVu b JOIN dbo.ChiTietBangGia ct ON ct.MaBangGia=b.MaBangGia WHERE ct.MaDichVu=d.MaDichVu AND b.TrangThai='DangApDung' AND b.TuNgay<=CAST(SYSDATETIME() AS date) AND (b.DenNgay IS NULL OR b.DenNgay>=CAST(SYSDATETIME() AS date)) ORDER BY b.TuNgay DESC) bg
                    LEFT JOIN dbo.TheBHYT bh ON bh.MaBenhNhan=l.MaBenhNhan AND bh.TrangThai='HopLe'
                    WHERE l.MaLuotKham=? AND bg.DonGia IS NOT NULL
                    """;try(PreparedStatement s=c.prepareStatement(service)){s.setLong(1,visitId);try(ResultSet r=s.executeQuery()){while(r.next())lines.add(new Object[]{r.getString(1),r.getString(2),r.getBigDecimal(3),r.getBigDecimal(4),r.getBoolean(5),r.getBigDecimal(6)});}}
            try(PreparedStatement s=c.prepareStatement("SELECT N'Thuoc',t.TenThuoc,c.SoLuong,CASE WHEN c.MaThuoc='THUOC-PARA' THEN 500 ELSE 1000 END,CASE WHEN bh.MaTheBHYT IS NULL THEN 0 ELSE 1 END,COALESCE(bh.MucHuong,0) FROM dbo.DonThuoc d JOIN dbo.ChiTietDonThuoc c ON c.MaDonThuoc=d.MaDonThuoc JOIN dbo.Thuoc t ON t.MaThuoc=c.MaThuoc LEFT JOIN dbo.TheBHYT bh ON bh.MaBenhNhan=(SELECT MaBenhNhan FROM dbo.LuotKham WHERE MaLuotKham=?) AND bh.TrangThai='HopLe' WHERE d.MaLuotKham=?")){s.setLong(1,visitId);s.setLong(2,visitId);try(ResultSet r=s.executeQuery()){while(r.next())lines.add(new Object[]{r.getString(1),r.getString(2),r.getBigDecimal(3),r.getBigDecimal(4),r.getBoolean(5),r.getBigDecimal(6)});}}
            if(lines.isEmpty())throw new FeeDAOException("Lượt khám chưa có dịch vụ hoặc thuốc để tính phí.");BigDecimal gross=BigDecimal.ZERO,insurance=BigDecimal.ZERO;for(Object[] x:lines){BigDecimal amount=((BigDecimal)x[2]).multiply((BigDecimal)x[3]);gross=gross.add(amount);if((Boolean)x[4])insurance=insurance.add(amount.multiply((BigDecimal)x[5]).divide(BigDecimal.valueOf(100)));}
            long fee=insert(c,"INSERT INTO dbo.PhieuTinhPhi(MaLuotKham,LanTinh,TongTruocGiam,TongGiamTru,BHYTChiTra,BenhNhanPhaiTra,TrangThai,MaNhanVienTinh) VALUES(?,?,?,?,?,?, 'DaXacNhan',?)",visitId,no,gross,BigDecimal.ZERO,insurance,gross.subtract(insurance),employeeId);
            for(Object[] x:lines)insert(c,"INSERT INTO dbo.ChiTietPhieuTinhPhi(MaPhieuTinhPhi,LoaiKhoan,MoTa,SoLuong,DonGia,ThanhTien,DuocBHYTChiTra,TyLeBHYT) VALUES(?,?,?,?,?,?,?,?,?)",fee,x[0],x[1],x[2],x[3],((BigDecimal)x[2]).multiply((BigDecimal)x[3]),x[4],x[5]);c.commit();return detail(fee);
        }catch(Exception e){try{c.rollback();}catch(SQLException ignored){}if(e instanceof FeeDAOException x)throw x;throw err("tính viện phí",e);}finally{try{c.setAutoCommit(true);}catch(SQLException ignored){}}}catch(SQLException e){throw err("tính viện phí",e);}
    }
    private long insert(Connection c,String sql,Object...v)throws SQLException{try(PreparedStatement s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){bind(s,List.of(v));s.executeUpdate();try(ResultSet r=s.getGeneratedKeys()){r.next();return r.getLong(1);}}}
    private long count(String sql,List<Object> p){try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){bind(s,p);try(ResultSet r=s.executeQuery()){r.next();return r.getLong(1);}}catch(SQLException e){throw err("đếm dữ liệu",e);}}
    private void bind(PreparedStatement s,List<?> p)throws SQLException{for(int i=0;i<p.size();i++){Object v=p.get(i);if(v instanceof BigDecimal n)s.setBigDecimal(i+1,n);else s.setObject(i+1,v);}}
    private static FeeDAOException err(String op,Exception e){return new FeeDAOException("Lỗi "+op+": "+e.getMessage(),e);}
}
