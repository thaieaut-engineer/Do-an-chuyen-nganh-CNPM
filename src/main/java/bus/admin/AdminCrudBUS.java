package bus.admin;

import bus.auth.AuthorizationBUS;
import dao.admin.AdminCrudDAO;
import java.util.List;
import model.admin.AdminCrudModels.*;
import model.auth.AuthenticatedSession;

public final class AdminCrudBUS {
    private final AdminCrudDAO dao = new AdminCrudDAO();
    private final AuthorizationBUS authorization = new AuthorizationBUS();
    public List<Evaluation> evaluations(AuthenticatedSession s,String q){access(s,"Đánh giá nhân viên","NHANSU_XEM");return dao.evaluations(normalize(q));}
    public void saveEvaluation(AuthenticatedSession s,EvaluationDraft d){access(s,"Đánh giá nhân viên","NHANSU_SUA");if(d==null||blank(d.employeeId())||blank(d.period())||d.date()==null)throw new AdminException("Vui lòng nhập đủ nhân viên, kỳ và ngày đánh giá.");if(d.score()!=null&&(d.score().signum()<0||d.score().doubleValue()>10))throw new AdminException("Điểm đánh giá phải từ 0 đến 10.");dao.saveEvaluation(d,s.getEmployeeId());}
    public void deleteEvaluation(AuthenticatedSession s,long id){access(s,"Đánh giá nhân viên","NHANSU_SUA");dao.deleteEvaluation(id);}
    public List<Config> configs(AuthenticatedSession s,String q){access(s,"Cấu hình hệ thống","CAUHINH_QUANLY");return dao.configs(normalize(q));}
    public void saveConfig(AuthenticatedSession s,Config d){access(s,"Cấu hình hệ thống","CAUHINH_QUANLY");if(d==null||blank(d.id()))throw new AdminException("Mã cấu hình là bắt buộc.");if(d.id().length()>80||d.type()!=null&&d.type().length()>30||d.description()!=null&&d.description().length()>255)throw new AdminException("Thông tin cấu hình vượt quá giới hạn.");dao.saveConfig(d,s.getAccountId());}
    public void deleteConfig(AuthenticatedSession s,String id){access(s,"Cấu hình hệ thống","CAUHINH_QUANLY");dao.deleteConfig(id);}
    public List<Notification> notifications(AuthenticatedSession s,String q){access(s,"Thông báo","TAIKHOAN_QUANLY");return dao.notifications(normalize(q));}
    public void saveNotification(AuthenticatedSession s,NotificationDraft d){access(s,"Thông báo","TAIKHOAN_QUANLY");if(d==null||blank(d.title())||blank(d.content()))throw new AdminException("Tiêu đề và nội dung thông báo là bắt buộc.");if(d.title().length()>180)throw new AdminException("Tiêu đề tối đa 180 ký tự.");dao.saveNotification(d);}
    public void deleteNotification(AuthenticatedSession s,long id){access(s,"Thông báo","TAIKHOAN_QUANLY");dao.deleteNotification(id);}
    private void access(AuthenticatedSession s,String page,String permission){try{authorization.requirePageAccess(s,page);authorization.requirePermission(s,permission);}catch(SecurityException e){throw new AdminException(e.getMessage(),e);}}
    private boolean blank(String v){return v==null||v.isBlank();} private String normalize(String v){return v==null?"":v.trim();}
}
