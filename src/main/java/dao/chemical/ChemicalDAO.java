package dao.chemical;

import config.DatabaseConnection;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import model.chemical.ChemicalModels.*;

public final class ChemicalDAO {
    public Stats stats(int warningDays) {
        String sql = """
                SELECT (SELECT COUNT_BIG(*) FROM dbo.HoaChat),
                       (SELECT COUNT_BIG(*) FROM dbo.LoHoaChat),
                       COALESCE((SELECT SUM(SoLuongTon) FROM dbo.LoHoaChat),0),
                       (SELECT COUNT_BIG(*) FROM dbo.LoHoaChat WHERE HanSuDung IS NOT NULL
                         AND HanSuDung >= CAST(SYSDATETIME() AS date)
                         AND HanSuDung <= DATEADD(DAY, ?, CAST(SYSDATETIME() AS date))),
                       (SELECT COUNT_BIG(*) FROM dbo.LoHoaChat WHERE HanSuDung IS NOT NULL
                         AND HanSuDung < CAST(SYSDATETIME() AS date)),
                       (SELECT COUNT_BIG(*) FROM dbo.LoHoaChat WHERE SoLuongTon <= 0)
                """;
        try (Connection c=DatabaseConnection.getConnection(); PreparedStatement s=c.prepareStatement(sql)) {
            s.setInt(1, warningDays); try (ResultSet r=s.executeQuery()) { r.next();
                return new Stats(r.getLong(1),r.getLong(2),r.getBigDecimal(3),r.getLong(4),r.getLong(5),r.getLong(6)); }
        } catch(SQLException e) { throw error("stats",e); }
    }

    public Page<Chemical> search(String text, String status, int page, int size) {
        String where=" WHERE 1=1 "; List<Object> p=new ArrayList<>();
        if(text!=null&&!text.isBlank()){ where += " AND (h.MaHoaChat LIKE ? OR h.TenHoaChat LIKE ?)"; String q="%"+text.trim()+"%"; p.add(q);p.add(q); }
        if(status!=null&&!status.isBlank()){ where += " AND h.TrangThai=?"; p.add(status); }
        long total=count("SELECT COUNT_BIG(*) FROM dbo.HoaChat h"+where,p);
        String sql="SELECT h.MaHoaChat,h.TenHoaChat,h.NongDo,h.DonViTinh,h.HangSanXuat,h.MoTa,h.TrangThai FROM dbo.HoaChat h"+where+" ORDER BY h.TenHoaChat OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        p.add(page*size);p.add(size); List<Chemical> rows=new ArrayList<>();
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){ bind(s,p);try(ResultSet r=s.executeQuery()){while(r.next())rows.add(new Chemical(r.getString(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6),r.getString(7)));}return new Page<>(rows,total,page,size); }catch(SQLException e){throw error("search chemicals",e);}
    }

    public Page<Batch> batches(String text, String status, int page, int size) {
        String where=" WHERE 1=1 "; List<Object> p=new ArrayList<>();
        if(text!=null&&!text.isBlank()){where+=" AND (l.MaHoaChat LIKE ? OR h.TenHoaChat LIKE ? OR l.SoLo LIKE ?)";String q="%"+text.trim()+"%";p.add(q);p.add(q);p.add(q);}
        if(status!=null&&!status.isBlank()){where+=" AND l.TrangThai=?";p.add(status);}
        long total=count("SELECT COUNT_BIG(*) FROM dbo.LoHoaChat l JOIN dbo.HoaChat h ON h.MaHoaChat=l.MaHoaChat"+where,p);
        String sql="SELECT l.MaLoHoaChat,l.MaHoaChat,h.TenHoaChat,l.SoLo,l.NgaySanXuat,l.HanSuDung,l.SoLuongTon,l.ViTriLuuTru,CASE WHEN l.HanSuDung IS NOT NULL AND l.HanSuDung<CAST(SYSDATETIME() AS date) THEN 'HetHan' WHEN l.SoLuongTon<=0 THEN 'HetHang' ELSE l.TrangThai END FROM dbo.LoHoaChat l JOIN dbo.HoaChat h ON h.MaHoaChat=l.MaHoaChat"+where+" ORDER BY l.HanSuDung,l.MaLoHoaChat OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";p.add(page*size);p.add(size);List<Batch> rows=new ArrayList<>();
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){bind(s,p);try(ResultSet r=s.executeQuery()){while(r.next())rows.add(new Batch(r.getLong(1),r.getString(2),r.getString(3),r.getString(4),date(r,5),date(r,6),r.getBigDecimal(7),r.getString(8),r.getString(9)));}return new Page<>(rows,total,page,size);}catch(SQLException e){throw error("search batches",e);}
    }

    public void save(ChemicalDraft d, boolean creating, long accountId) {
        String sql=creating?"INSERT INTO dbo.HoaChat(MaHoaChat,TenHoaChat,NongDo,DonViTinh,HangSanXuat,MoTa,TrangThai) VALUES(?,?,?,?,?,?,?)":"UPDATE dbo.HoaChat SET TenHoaChat=?,NongDo=?,DonViTinh=?,HangSanXuat=?,MoTa=?,TrangThai=? WHERE MaHoaChat=?";
        try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){if(creating){s.setString(1,d.id());bindDraft(s,d,2);}else{bindDraft(s,d,1);s.setString(7,d.id());}if(s.executeUpdate()!=1)throw new ChemicalDAOException("Không tìm thấy hóa chất để cập nhật.");}catch(SQLException e){throw error("save chemical",e);}
    }

    public CreatedReceipt createImport(ImportDraft d,String employeeId){
        String number="PN-HC-"+System.currentTimeMillis();
        try(Connection c=DatabaseConnection.getConnection()){c.setAutoCommit(false);try{
            long id=insertReceipt(c,"INSERT INTO dbo.PhieuNhap(SoPhieu,NhaCungCap,MaNhanVienNhap,TongTien,TrangThai,GhiChu) VALUES(?,?,?,?,'DaXacNhan',?)",number,d.supplier(),employeeId,BigDecimal.ZERO,d.note());
            BigDecimal total=BigDecimal.ZERO;for(ImportLine l:d.lines()){long batch=findOrCreateBatch(c,l);BigDecimal line=l.quantity().multiply(l.unitCost());insert(c,"INSERT INTO dbo.ChiTietNhapHoaChat(MaPhieuNhap,MaLoHoaChat,SoLuong,DonGia,ThanhTien) VALUES(?,?,?,?,?)",id,batch,l.quantity(),l.unitCost(),line);update(c,"UPDATE dbo.LoHoaChat SET SoLuongTon=SoLuongTon+?,TrangThai='DangTon' WHERE MaLoHoaChat=?",l.quantity(),batch);total=total.add(line);}update(c,"UPDATE dbo.PhieuNhap SET TongTien=? WHERE MaPhieuNhap=?",total,id);c.commit();return new CreatedReceipt(id,number);
        }catch(Exception e){try{c.rollback();}catch(SQLException ignored){}if(e instanceof ChemicalDAOException x)throw x;throw error("create import",e);}finally{try{c.setAutoCommit(true);}catch(SQLException ignored){}}}catch(SQLException e){throw error("create import",e);}
    }

    public CreatedReceipt createExport(ExportDraft d,String employeeId){
        String number="PX-HC-"+System.currentTimeMillis();
        try(Connection c=DatabaseConnection.getConnection()){c.setAutoCommit(false);try{
            long batch=lockBatch(c,d.chemicalId(),d.batchNumber());if(batch<=0)throw new ChemicalDAOException("Không tìm thấy lô hóa chất.");
            BigDecimal stock=scalarDecimal(c,"SELECT SoLuongTon FROM dbo.LoHoaChat WHERE MaLoHoaChat=?",batch);if(stock.compareTo(d.quantity())<0)throw new ChemicalDAOException("Số lượng tồn không đủ.");
            long id=insertReceipt(c,"INSERT INTO dbo.PhieuXuat(SoPhieu,LoaiXuat,MaNhanVienXuat,NguoiNhan,TrangThai,GhiChu) VALUES(?,?,?,?, 'DaXacNhan',?)",number,"Khac",employeeId,d.recipient(),d.note());
            insert(c,"INSERT INTO dbo.ChiTietXuatHoaChat(MaPhieuXuat,MaLoHoaChat,SoLuong) VALUES(?,?,?)",id,batch,d.quantity());update(c,"UPDATE dbo.LoHoaChat SET SoLuongTon=SoLuongTon-?,TrangThai=CASE WHEN SoLuongTon-?<=0 THEN 'HetHang' ELSE 'DangTon' END WHERE MaLoHoaChat=?",d.quantity(),d.quantity(),batch);c.commit();return new CreatedReceipt(id,number);
        }catch(Exception e){try{c.rollback();}catch(SQLException ignored){}if(e instanceof ChemicalDAOException x)throw x;throw error("create export",e);}finally{try{c.setAutoCommit(true);}catch(SQLException ignored){}}}catch(SQLException e){throw error("create export",e);}
    }

    public Page<Movement> movements(String text,int page,int size){
        String where=" WHERE 1=1 ";List<Object> p=new ArrayList<>();if(text!=null&&!text.isBlank()){where+=" AND (h.MaHoaChat LIKE ? OR h.TenHoaChat LIKE ? OR l.SoLo LIKE ?)";String q="%"+text.trim()+"%";p.add(q);p.add(q);p.add(q);}
        String base=" FROM (SELECT n.MaPhieuNhap id,n.NgayNhap date,n.SoPhieu number,l.MaHoaChat,l.MaLoHoaChat,l.SoLo,x.SoLuong,x.DonGia,n.MaNhanVienNhap employee,n.TrangThai,n.GhiChu FROM dbo.ChiTietNhapHoaChat x JOIN dbo.PhieuNhap n ON n.MaPhieuNhap=x.MaPhieuNhap JOIN dbo.LoHoaChat l ON l.MaLoHoaChat=x.MaLoHoaChat UNION ALL SELECT x.MaPhieuXuat,n.NgayXuat,n.SoPhieu,l.MaHoaChat,l.MaLoHoaChat,l.SoLo,-x.SoLuong,0,n.MaNhanVienXuat,n.TrangThai,n.GhiChu FROM dbo.ChiTietXuatHoaChat x JOIN dbo.PhieuXuat n ON n.MaPhieuXuat=x.MaPhieuXuat JOIN dbo.LoHoaChat l ON l.MaLoHoaChat=x.MaLoHoaChat) m JOIN dbo.HoaChat h ON h.MaHoaChat=m.MaHoaChat JOIN dbo.NhanVien e ON e.MaNhanVien=m.employee";
        long total=count("SELECT COUNT_BIG(*)"+base.replace(" WHERE 1=1 ","")+where,p);String sql="SELECT m.date,CASE WHEN m.quantity<0 THEN 'Xuat' ELSE 'Nhap' END,m.number,m.MaHoaChat,h.TenHoaChat,m.SoLo,ABS(m.quantity),m.DonGia,e.HoTen,m.TrangThai,m.GhiChu"+base+where+" ORDER BY m.date DESC OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";p.add(page*size);p.add(size);List<Movement> rows=new ArrayList<>();try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){bind(s,p);try(ResultSet r=s.executeQuery()){while(r.next())rows.add(new Movement(r.getTimestamp(1).toLocalDateTime(),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getString(6),r.getBigDecimal(7),r.getBigDecimal(8),r.getString(9),r.getString(10),r.getString(11)));}return new Page<>(rows,total,page,size);}catch(SQLException e){throw error("movements",e);}
    }

    private long findOrCreateBatch(Connection c,ImportLine l)throws SQLException{try(PreparedStatement s=c.prepareStatement("SELECT MaLoHoaChat FROM dbo.LoHoaChat WHERE MaHoaChat=? AND SoLo=?")){s.setString(1,l.chemicalId());s.setString(2,l.batchNumber());try(ResultSet r=s.executeQuery()){if(r.next()){long id=r.getLong(1);update(c,"UPDATE dbo.LoHoaChat SET HanSuDung=?,NgaySanXuat=?,ViTriLuuTru=? WHERE MaLoHoaChat=?",l.expiryDate(),l.manufactureDate(),l.location(),id);return id;}}}return insert(c,"INSERT INTO dbo.LoHoaChat(MaHoaChat,SoLo,NgaySanXuat,HanSuDung,SoLuongTon,ViTriLuuTru,TrangThai) VALUES(?,?,?,?,0,?,'DangTon')",l.chemicalId(),l.batchNumber(),l.manufactureDate(),l.expiryDate(),l.location());}
    private long lockBatch(Connection c,String chemical,String lot)throws SQLException{try(PreparedStatement s=c.prepareStatement("SELECT MaLoHoaChat FROM dbo.LoHoaChat WITH (UPDLOCK,HOLDLOCK) WHERE MaHoaChat=? AND SoLo=? AND TrangThai<>'BiKhoa'")){s.setString(1,chemical);s.setString(2,lot);try(ResultSet r=s.executeQuery()){return r.next()?r.getLong(1):0;}}}
    private long insertReceipt(Connection c,String sql,Object...v)throws SQLException{return insert(c,sql,v);}
    private long insert(Connection c,String sql,Object...v)throws SQLException{try(PreparedStatement s=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)){bind(s,List.of(v));s.executeUpdate();try(ResultSet r=s.getGeneratedKeys()){if(!r.next())throw new SQLException("Không lấy được mã bản ghi.");return r.getLong(1);}}}
    private void update(Connection c,String sql,Object...v)throws SQLException{try(PreparedStatement s=c.prepareStatement(sql)){bind(s,List.of(v));s.executeUpdate();}}
    private BigDecimal scalarDecimal(Connection c,String sql,Object...v)throws SQLException{try(PreparedStatement s=c.prepareStatement(sql)){bind(s,List.of(v));try(ResultSet r=s.executeQuery()){r.next();return r.getBigDecimal(1);}}}
    private long count(String sql,List<Object> p){try(Connection c=DatabaseConnection.getConnection();PreparedStatement s=c.prepareStatement(sql)){bind(s,p);try(ResultSet r=s.executeQuery()){r.next();return r.getLong(1);}}catch(SQLException e){throw error("count",e);}}
    private void bind(PreparedStatement s,List<?> p)throws SQLException{for(int i=0;i<p.size();i++){Object v=p.get(i);if(v instanceof LocalDate d)s.setDate(i+1,Date.valueOf(d));else if(v instanceof BigDecimal n)s.setBigDecimal(i+1,n);else s.setObject(i+1,v);}}
    private void bindDraft(PreparedStatement s,ChemicalDraft d,int i)throws SQLException{s.setString(i++,d.name());s.setString(i++,d.concentration());s.setString(i++,d.unit());s.setString(i++,d.manufacturer());s.setString(i++,d.description());s.setString(i,d.status());}
    private static LocalDate date(ResultSet r,int i)throws SQLException{Date d=r.getDate(i);return d==null?null:d.toLocalDate();}
    private static ChemicalDAOException error(String op,Exception e){return new ChemicalDAOException("Lỗi " + op + ": "+e.getMessage(),e);}
}
