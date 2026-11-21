package bus.billing;

import bus.auth.AuthorizationBUS;
import dao.billing.BillingDAO;
import dao.billing.BillingDAOException;
import java.math.BigDecimal;
import java.util.List;
import model.auth.AuthenticatedSession;
import model.billing.BillingModels.*;

public final class BillingBUS {
    private final BillingDAO dao=new BillingDAO(); private final AuthorizationBUS auth=new AuthorizationBUS();
    public List<InvoiceRow> invoices(AuthenticatedSession s,String q,String status){access(s,"Hóa đơn","HOADON_QUANLY");return run(()->dao.find(q,status));}
    public List<InvoiceRow> paymentQueue(AuthenticatedSession s,String q,String status){access(s,"Thanh toán","THANHTOAN_THU");return run(()->dao.find(q,status));}
    public Summary summary(AuthenticatedSession s){access(s,"Hóa đơn","HOADON_QUANLY");return run(dao::summary);}
    public List<PaymentMethod> methods(AuthenticatedSession s){access(s,"Thanh toán","THANHTOAN_THU");return run(dao::methods);}
    public void pay(AuthenticatedSession s,PaymentDraft d){access(s,"Thanh toán","THANHTOAN_THU");if(d==null||d.invoiceId()<=0||d.methodId()==null||d.amount()==null||d.amount().compareTo(BigDecimal.ZERO)<=0)throw new BillingException("Thông tin thanh toán chưa hợp lệ.");run(()->{dao.pay(d,s.getEmployeeId());return null;});}
    private void access(AuthenticatedSession s,String page,String permission){if(s==null||!s.isActive())throw new BillingException("Phiên làm việc không hợp lệ.");try{auth.requirePageAccess(s,page);auth.requirePermission(s,permission);}catch(SecurityException e){throw new BillingException(e.getMessage(),e);}}
    private <T>T run(java.util.concurrent.Callable<T> x){try{return x.call();}catch(BillingDAOException e){throw new BillingException(e.getMessage(),e);}catch(Exception e){throw new BillingException(e.getMessage(),e);}}
}
