package dao.billing;

import config.DatabaseConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import model.billing.BillingModels.InvoiceRow;
import model.billing.BillingModels.PaymentDraft;
import model.billing.BillingModels.PaymentMethod;
import model.billing.BillingModels.Summary;

public final class BillingDAO {
    private static final String BASE = " FROM dbo.HoaDon h JOIN dbo.PhieuTinhPhi f ON f.MaPhieuTinhPhi=h.MaPhieuTinhPhi "
            + "JOIN dbo.LuotKham l ON l.MaLuotKham=f.MaLuotKham JOIN dbo.BenhNhan b ON b.MaBenhNhan=l.MaBenhNhan "
            + "OUTER APPLY (SELECT COALESCE(SUM(g.SoTien),0) paid FROM dbo.GiaoDichThanhToan g "
            + "WHERE g.MaHoaDon=h.MaHoaDon AND g.TrangThai=N'ThanhCong') p ";

    public List<InvoiceRow> find(String keyword, String status) {
        String where = " WHERE (?='' OR h.SoHoaDon LIKE ? OR b.MaBenhNhan LIKE ? OR b.HoTen LIKE ?)"
                + " AND (?='' OR h.TrangThai=?)";
        String sql = "SELECT h.MaHoaDon,h.SoHoaDon,h.MaPhieuTinhPhi,b.MaBenhNhan,b.HoTen,h.NgayLap,h.TongTien,h.SoTienBHYT,h.SoTienPhaiTra,p.paid,"
                + "CASE WHEN h.SoTienPhaiTra-p.paid<0 THEN 0 ELSE h.SoTienPhaiTra-p.paid END,h.TrangThai" + BASE + where
                + " ORDER BY h.NgayLap DESC";
        List<InvoiceRow> rows = new ArrayList<>();
        try (Connection c=DatabaseConnection.getConnection(); PreparedStatement s=c.prepareStatement(sql)) {
            String q=keyword==null?"":keyword.trim(); String st=status==null?"":status;
            s.setString(1,q); s.setString(2,"%"+q+"%"); s.setString(3,"%"+q+"%"); s.setString(4,"%"+q+"%"); s.setString(5,st); s.setString(6,st);
            try(ResultSet r=s.executeQuery()){while(r.next()) rows.add(new InvoiceRow(r.getLong(1),r.getString(2),r.getLong(3),r.getString(4),r.getString(5),r.getTimestamp(6).toLocalDateTime(),r.getBigDecimal(7),r.getBigDecimal(8),r.getBigDecimal(9),r.getBigDecimal(10),r.getBigDecimal(11),r.getString(12)));}
            return List.copyOf(rows);
        } catch(SQLException e){throw error("tải danh sách hóa đơn",e);}
    }
    public Summary summary(){String sql="SELECT COUNT_BIG(*),SUM(CASE WHEN TrangThai IN (N'ChuaThanhToan',N'ThanhToanMotPhan') THEN 1 ELSE 0 END),SUM(CASE WHEN TrangThai=N'DaThanhToan' THEN 1 ELSE 0 END),COALESCE((SELECT SUM(SoTien) FROM dbo.GiaoDichThanhToan WHERE TrangThai=N'ThanhCong' AND CAST(ThoiGianGiaoDich AS date)=CAST(SYSDATETIME() AS date)),0) FROM dbo.HoaDon";try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql);ResultSet r=s.executeQuery()){r.next();return new Summary(r.getLong(1),r.getLong(2),r.getLong(3),r.getBigDecimal(4));}catch(SQLException e){throw error("tải thống kê thanh toán",e);}}
    public List<PaymentMethod> methods(){List<PaymentMethod> x=new ArrayList<>();try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement("SELECT MaPhuongThuc,TenPhuongThuc FROM dbo.PhuongThucThanhToan WHERE TrangThai=N'HoatDong' ORDER BY TenPhuongThuc");ResultSet r=s.executeQuery()){while(r.next())x.add(new PaymentMethod(r.getString(1),r.getString(2)));return List.copyOf(x);}catch(SQLException e){throw error("tải phương thức thanh toán",e);}}
    public void pay(PaymentDraft d,String employeeId){
        try(Connection c=DatabaseConnection.getConnection()){c.setAutoCommit(false);try{
            BigDecimal due;
            try(PreparedStatement s=c.prepareStatement("SELECT h.SoTienPhaiTra-COALESCE((SELECT SUM(g.SoTien) FROM dbo.GiaoDichThanhToan g WHERE g.MaHoaDon=h.MaHoaDon AND g.TrangThai=N'ThanhCong'),0) FROM dbo.HoaDon h WITH (UPDLOCK) WHERE h.MaHoaDon=? AND h.TrangThai NOT IN (N'DaHuy',N'DaHoanTien')")){s.setLong(1,d.invoiceId());try(ResultSet r=s.executeQuery()){if(!r.next())throw new SQLException("Hóa đơn không tồn tại hoặc không thể thu tiền.");due=r.getBigDecimal(1);}}
            if(d.amount().compareTo(due)>0)throw new SQLException("Số tiền thu vượt số tiền còn phải thu: "+due.toPlainString());
            try(PreparedStatement s=c.prepareStatement("INSERT INTO dbo.GiaoDichThanhToan(MaHoaDon,MaPhuongThuc,MaThamChieu,SoTien,TrangThai,NguoiThucHien,GhiChu) VALUES(?,?,?,?,N'ThanhCong',?,?)")){s.setLong(1,d.invoiceId());s.setString(2,d.methodId());s.setString(3,d.reference()==null||d.reference().isBlank()?null:d.reference().trim());s.setBigDecimal(4,d.amount());s.setString(5,employeeId);s.setNString(6,d.note());s.executeUpdate();}
            String next=d.amount().compareTo(due)==0?"DaThanhToan":"ThanhToanMotPhan";try(PreparedStatement s=c.prepareStatement("UPDATE dbo.HoaDon SET TrangThai=? WHERE MaHoaDon=?")){s.setNString(1,next);s.setLong(2,d.invoiceId());s.executeUpdate();}c.commit();
        }catch(Exception e){try{c.rollback();}catch(SQLException x){e.addSuppressed(x);}if(e instanceof BillingDAOException x)throw x;throw error("ghi nhận thanh toán",e);}finally{try{c.setAutoCommit(true);}catch(SQLException ignored){}}}catch(SQLException e){throw error("ghi nhận thanh toán",e);}
    }
    private BillingDAOException error(String op,Exception e){return new BillingDAOException("Không thể "+op+". Chi tiết hệ thống: "+e.getMessage(),e);}
}
