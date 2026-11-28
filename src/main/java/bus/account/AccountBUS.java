package bus.account;

import bus.auth.AuthorizationBUS;
import bus.auth.PasswordHasher;
import dao.account.AccountDAO;
import java.util.List;
import model.account.AccountModels.AccountDraft;
import model.account.AccountModels.AccountRow;
import model.account.AccountModels.EmployeeOption;
import model.account.AccountModels.RoleOption;
import model.auth.AuthenticatedSession;

public final class AccountBUS {
    private final AccountDAO dao = new AccountDAO();
    private final AuthorizationBUS authorization = new AuthorizationBUS();
    public List<AccountRow> search(AuthenticatedSession s,String keyword,String status){ access(s); return dao.find(keyword,status); }
    public List<RoleOption> roles(AuthenticatedSession s){ access(s); return dao.findRoles(); }
    public List<EmployeeOption> employees(AuthenticatedSession s){ return employees(s, 0); }
    public List<EmployeeOption> employees(AuthenticatedSession s, long accountId){ access(s); return dao.findEmployees(accountId); }
    public long create(AuthenticatedSession s,AccountDraft d){ access(s); validate(d,true); return dao.insert(d,PasswordHasher.hash(d.password()),s.getAccountId()); }
    public void update(AuthenticatedSession s,AccountDraft d){ access(s); validate(d,false); dao.update(d,d.password()==null||d.password().length==0?null:PasswordHasher.hash(d.password()),s.getAccountId()); }
    public void changeStatus(AuthenticatedSession s,long id,String status){ access(s); if(id==s.getAccountId()) throw new AccountException("Không thể tự khóa tài khoản đang đăng nhập."); if(!List.of("HoatDong","Khoa","ChoKichHoat","ThuHoi").contains(status)) throw new AccountException("Trạng thái không hợp lệ."); dao.changeStatus(id,status,s.getAccountId()); }
    private void access(AuthenticatedSession s){ authorization.requirePageAccess(s,"Tài khoản"); }
    private void validate(AccountDraft d,boolean create){
        if(d==null||d.username()==null||!d.username().matches("[A-Za-z0-9._-]{4,80}")) throw new AccountException("Tên đăng nhập 4-80 ký tự, chỉ gồm chữ không dấu, số, dấu chấm, gạch ngang hoặc gạch dưới.");
        if(d.employeeId()==null||d.employeeId().isBlank()) throw new AccountException("Vui lòng chọn nhân viên gắn với tài khoản.");
        if(d.roleIds()==null||d.roleIds().isEmpty()) throw new AccountException("Vui lòng chọn ít nhất một vai trò.");
        if(create&&(d.password()==null||d.password().length<2)) throw new AccountException("Mật khẩu phải có ít nhất 2 ký tự.");
        if(d.password()!=null&&d.password().length>0&&d.password().length<2) throw new AccountException("Mật khẩu phải có ít nhất 2 ký tự.");
    }
}
