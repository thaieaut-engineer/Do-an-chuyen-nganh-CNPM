package bus.chemical;

import bus.auth.AuthorizationBUS;
import dao.chemical.ChemicalDAO;
import dao.chemical.ChemicalDAOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import model.auth.AuthenticatedSession;
import model.chemical.ChemicalModels.*;

public final class ChemicalBUS {
    private final ChemicalDAO dao = new ChemicalDAO();
    private final AuthorizationBUS authorization = new AuthorizationBUS();

    public Stats stats(AuthenticatedSession s) throws ChemicalException { read(s); return query(() -> dao.stats(90)); }
    public Page<Chemical> search(AuthenticatedSession s,String q,String status,int page,int size)throws ChemicalException{read(s);validatePage(page,size);return query(()->dao.search(q,status,page,size));}
    public Page<Batch> batches(AuthenticatedSession s,String q,String status,int page,int size)throws ChemicalException{read(s);validatePage(page,size);return query(()->dao.batches(q,status,page,size));}
    public Page<Movement> movements(AuthenticatedSession s,String q,int page,int size)throws ChemicalException{read(s);validatePage(page,size);return query(()->dao.movements(q,page,size));}
    public void save(AuthenticatedSession s,ChemicalDraft d,boolean creating)throws ChemicalException{write(s,"KHO_NHAP");if(d==null||d.id()==null||d.id().isBlank()||d.id().length()>20)throw new ChemicalException("Mã hóa chất bắt buộc, tối đa 20 ký tự.");text("Tên hóa chất",d.name(),180,true);text("Nồng độ",d.concentration(),80,false);text("Đơn vị tính",d.unit(),30,false);text("Hãng sản xuất",d.manufacturer(),150,false);text("Mô tả",d.description(),255,false);choice(d.status(),Set.of("DangSuDung","NgungSuDung"));query(()->{dao.save(d,creating,s.getAccountId());return null;});}
    public CreatedReceipt createImport(AuthenticatedSession s,ImportDraft d)throws ChemicalException{write(s,"KHO_NHAP");if(d==null||d.lines().isEmpty())throw new ChemicalException("Phiếu nhập phải có ít nhất một dòng.");for(ImportLine l:d.lines()){if(l==null||l.chemicalId()==null||l.chemicalId().isBlank()||l.batchNumber()==null||l.batchNumber().isBlank()||l.expiryDate()==null||l.expiryDate().isBefore(LocalDate.now())||l.quantity()==null||l.quantity().signum()<=0||l.unitCost()==null||l.unitCost().signum()<0)throw new ChemicalException("Thông tin dòng nhập không hợp lệ.");}return query(()->dao.createImport(d,s.getEmployeeId()));}
    public CreatedReceipt createExport(AuthenticatedSession s,ExportDraft d)throws ChemicalException{write(s,"KHO_XUAT");if(d==null||d.chemicalId()==null||d.batchNumber()==null||d.quantity()==null||d.quantity().signum()<=0)throw new ChemicalException("Thông tin xuất hóa chất không hợp lệ.");return query(()->dao.createExport(d,s.getEmployeeId()));}
    private void read(AuthenticatedSession s)throws ChemicalException{access(s,"KHO_XEM");}
    private void write(AuthenticatedSession s,String permission)throws ChemicalException{access(s,permission);}
    private void access(AuthenticatedSession s,String p)throws ChemicalException{if(s==null||!s.isActive())throw new ChemicalException("Phiên làm việc không hợp lệ.");try{authorization.requirePageAccess(s,"Kho hóa chất");authorization.requirePermission(s,p);}catch(SecurityException e){throw new ChemicalException(e.getMessage(),e);}}
    private void validatePage(int p,int size)throws ChemicalException{if(p<0||size<1||size>100)throw new ChemicalException("Thông tin phân trang không hợp lệ.");}
    private void text(String n,String v,int max,boolean req)throws ChemicalException{if(req&&(v==null||v.isBlank()))throw new ChemicalException(n+" là bắt buộc.");if(v!=null&&v.length()>max)throw new ChemicalException(n+" vượt quá giới hạn "+max+" ký tự.");}
    private void choice(String v,Set<String> choices)throws ChemicalException{if(v==null||!choices.contains(v))throw new ChemicalException("Trạng thái không hợp lệ.");}
    private <T>T query(java.util.concurrent.Callable<T> c)throws ChemicalException{try{return c.call();}catch(ChemicalDAOException e){throw new ChemicalException(e.getMessage(),e);}catch(Exception e){throw new ChemicalException(e.getMessage(),e);}}
}
