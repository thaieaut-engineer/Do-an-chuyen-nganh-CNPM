package bus.fee;

import bus.auth.AuthorizationBUS;
import dao.fee.FeeDAO;
import dao.fee.FeeDAOException;
import model.auth.AuthenticatedSession;
import model.fee.FeeModels.*;

public final class FeeBUS {
    private final FeeDAO dao=new FeeDAO(); private final AuthorizationBUS authorization=new AuthorizationBUS();
    public Stats stats(AuthenticatedSession s)throws FeeException{read(s);return query(dao::stats);}
    public Page<Visit> visits(AuthenticatedSession s,String q,int page,int size)throws FeeException{read(s);page(page,size);return query(()->dao.visits(q,page,size));}
    public Detail detail(AuthenticatedSession s,long id)throws FeeException{read(s);if(id<=0)throw new FeeException("Mã phiếu không hợp lệ.");return query(()->dao.detail(id));}
    public Detail latestDetail(AuthenticatedSession s,long visitId)throws FeeException{read(s);if(visitId<=0)throw new FeeException("Mã lượt khám không hợp lệ.");return query(()->dao.latestDetailForVisit(visitId));}
    public Detail calculate(AuthenticatedSession s,long visitId)throws FeeException{access(s,"VIENPHI_TINH");if(visitId<=0)throw new FeeException("Mã lượt khám không hợp lệ.");return query(()->dao.calculate(visitId,s.getEmployeeId()));}
    private void read(AuthenticatedSession s)throws FeeException{access(s,"VIENPHI_TINH");}
    private void access(AuthenticatedSession s,String permission)throws FeeException{if(s==null||!s.isActive())throw new FeeException("Phiên làm việc không hợp lệ.");try{authorization.requirePageAccess(s,"Viện phí");authorization.requirePermission(s,permission);}catch(SecurityException e){throw new FeeException(e.getMessage(),e);}}
    private void page(int p,int size)throws FeeException{if(p<0||size<1||size>100)throw new FeeException("Thông tin phân trang không hợp lệ.");}
    private <T>T query(java.util.concurrent.Callable<T> call)throws FeeException{try{return call.call();}catch(FeeDAOException e){throw new FeeException(e.getMessage(),e);}catch(Exception e){throw new FeeException(e.getMessage(),e);}}
}
