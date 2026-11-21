package dao.insurance;

import config.DatabaseConnection;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import model.insurance.InsuranceModels.Card;
import model.insurance.InsuranceModels.CardDraft;
import model.insurance.InsuranceModels.PatientOption;

public final class InsuranceDAO {
    public List<Card> find(String q,String status){String sql="SELECT t.MaTheBHYT,t.MaBenhNhan,b.HoTen,t.SoThe,t.HoTenTrenThe,t.NgaySinhTrenThe,t.NoiDangKyKCB,t.TuNgay,t.DenNgay,t.MucHuong,t.TrangThai FROM dbo.TheBHYT t JOIN dbo.BenhNhan b ON b.MaBenhNhan=t.MaBenhNhan WHERE (?='' OR t.SoThe LIKE ? OR t.MaBenhNhan LIKE ? OR b.HoTen LIKE ?) AND (?='' OR t.TrangThai=?) ORDER BY t.DenNgay";List<Card>x=new ArrayList<>();try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){String k=q==null?"":q.trim(),st=status==null?"":status;s.setString(1,k);s.setString(2,"%"+k+"%");s.setString(3,"%"+k+"%");s.setString(4,"%"+k+"%");s.setString(5,st);s.setString(6,st);try(ResultSet r=s.executeQuery()){while(r.next())x.add(new Card(r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),date(r.getDate(6)),r.getString(7),date(r.getDate(8)),date(r.getDate(9)),r.getBigDecimal(10),r.getString(11)));}return List.copyOf(x);}catch(SQLException e){throw err("tải danh sách thẻ BHYT",e);}}
    public List<PatientOption> patients(){List<PatientOption>x=new ArrayList<>();try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement("SELECT MaBenhNhan,HoTen FROM dbo.BenhNhan WHERE TrangThai=N'HoatDong' ORDER BY HoTen");ResultSet r=s.executeQuery()){while(r.next())x.add(new PatientOption(r.getString(1),r.getString(2)));return List.copyOf(x);}catch(SQLException e){throw err("tải danh sách bệnh nhân",e);}}
    public void save(CardDraft d){String insert="INSERT INTO dbo.TheBHYT(MaBenhNhan,SoThe,HoTenTrenThe,NgaySinhTrenThe,NoiDangKyKCB,TuNgay,DenNgay,MucHuong,TrangThai) VALUES(?,?,?,?,?,?,?,?,?)";String update="UPDATE dbo.TheBHYT SET MaBenhNhan=?,SoThe=?,HoTenTrenThe=?,NgaySinhTrenThe=?,NoiDangKyKCB=?,TuNgay=?,DenNgay=?,MucHuong=?,TrangThai=? WHERE MaTheBHYT=?";try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(d.id()==0?insert:update)){int i=1;s.setString(i++,d.patientId());s.setString(i++,d.cardNumber());s.setNString(i++,d.nameOnCard());setDate(s,i++,d.birthDate());s.setNString(i++,d.facility());setDate(s,i++,d.fromDate());setDate(s,i++,d.toDate());s.setBigDecimal(i++,d.coverage());s.setNString(i++,d.status());if(d.id()!=0)s.setLong(i,d.id());s.executeUpdate();}catch(SQLException e){throw err("lưu thẻ BHYT",e);}}
    public void updateStatus(long id,String status){try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement("UPDATE dbo.TheBHYT SET TrangThai=? WHERE MaTheBHYT=?")){s.setNString(1,status);s.setLong(2,id);s.executeUpdate();}catch(SQLException e){throw err("cập nhật trạng thái thẻ BHYT",e);}}
    private static void setDate(PreparedStatement s,int i,LocalDate d)throws SQLException{if(d==null)s.setNull(i,Types.DATE);else s.setDate(i,Date.valueOf(d));}
    private static LocalDate date(Date d){return d==null?null:d.toLocalDate();}
    private static InsuranceDAOException err(String op,Exception e){return new InsuranceDAOException("Không thể "+op+". Chi tiết hệ thống: "+e.getMessage(),e);}
}
