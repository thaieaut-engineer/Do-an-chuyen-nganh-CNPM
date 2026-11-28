package bus.admin;

import bus.auth.AuthorizationBUS;
import dao.admin.AdminCrudDAO;
import java.util.List;
import java.util.Set;
import model.admin.AdminCrudModels.*;
import model.auth.AuthenticatedSession;

public final class AdminCrudBUS {
    private static final Set<String> NOTIFICATION_STATUSES = Set.of("Nhap", "DaGui", "ThatBai", "DaDoc");
    private final AdminCrudDAO dao = new AdminCrudDAO();
    private final AuthorizationBUS authorization = new AuthorizationBUS();

    public List<Evaluation> evaluations(AuthenticatedSession s, String q) { access(s, "Đánh giá nhân viên", "NHANSU_XEM"); return dao.evaluations(normalize(q)); }
    public void saveEvaluation(AuthenticatedSession s, EvaluationDraft d) {
        access(s, "Đánh giá nhân viên", "NHANSU_SUA");
        if (d == null || blank(d.employeeId()) || blank(d.period()) || d.date() == null) throw new AdminException("Vui lòng nhập đủ nhân viên, kỳ và ngày đánh giá.");
        if (d.score() != null && (d.score().signum() < 0 || d.score().compareTo(java.math.BigDecimal.TEN) > 0)) throw new AdminException("Điểm đánh giá phải từ 0 đến 10.");
        check(d.employeeId(), 20, "Mã nhân viên"); check(d.period(), 30, "Kỳ đánh giá"); check(d.comment(), 255, "Nhận xét");
        dao.saveEvaluation(new EvaluationDraft(d.id(), d.employeeId().trim(), d.period().trim(), d.date(), d.score(), d.comment()), s.getEmployeeId());
    }
    public void deleteEvaluation(AuthenticatedSession s, long id) { access(s, "Đánh giá nhân viên", "NHANSU_SUA"); if (id <= 0) throw new AdminException("Mã đánh giá không hợp lệ."); dao.deleteEvaluation(id); }
    public List<Config> configs(AuthenticatedSession s, String q) { access(s, "Cấu hình hệ thống", "CAUHINH_QUANLY"); return dao.configs(normalize(q)); }
    public void saveConfig(AuthenticatedSession s, Config d) { access(s, "Cấu hình hệ thống", "CAUHINH_QUANLY"); if (d == null || blank(d.id())) throw new AdminException("Mã cấu hình là bắt buộc."); check(d.id(), 80, "Mã cấu hình"); check(d.type(), 30, "Kiểu dữ liệu"); check(d.description(), 255, "Mô tả"); dao.saveConfig(new Config(d.id().trim(), d.value(), d.type(), d.description()), s.getAccountId()); }
    public void deleteConfig(AuthenticatedSession s, String id) { access(s, "Cấu hình hệ thống", "CAUHINH_QUANLY"); if (blank(id)) throw new AdminException("Mã cấu hình không hợp lệ."); dao.deleteConfig(id.trim()); }
    public List<Notification> notifications(AuthenticatedSession s, String q) { access(s, "Thông báo", "TAIKHOAN_QUANLY"); return dao.notifications(normalize(q)); }
    public void saveNotification(AuthenticatedSession s, NotificationDraft d) {
        access(s, "Thông báo", "TAIKHOAN_QUANLY");
        if (d == null || blank(d.title()) || blank(d.content())) throw new AdminException("Tiêu đề và nội dung thông báo là bắt buộc.");
        if (d.status() != null && !NOTIFICATION_STATUSES.contains(d.status())) throw new AdminException("Trạng thái thông báo không hợp lệ.");
        check(d.title(), 180, "Tiêu đề"); check(d.type(), 50, "Loại thông báo"); check(d.channel(), 40, "Kênh gửi"); check(d.employeeId(), 20, "Mã nhân viên"); check(d.patientId(), 20, "Mã bệnh nhân");
        dao.saveNotification(new NotificationDraft(d.id(), empty(d.employeeId()), empty(d.patientId()), d.title().trim(), d.content().trim(), d.type(), d.status() == null ? "Nhap" : d.status(), d.channel()));
    }
    public void deleteNotification(AuthenticatedSession s, long id) { access(s, "Thông báo", "TAIKHOAN_QUANLY"); if (id <= 0) throw new AdminException("Mã thông báo không hợp lệ."); dao.deleteNotification(id); }
    private void access(AuthenticatedSession s, String page, String permission) { try { authorization.requirePageAccess(s, page); authorization.requirePermission(s, permission); } catch (SecurityException e) { throw new AdminException(e.getMessage(), e); } }
    private static void check(String value, int max, String label) { if (value != null && value.length() > max) throw new AdminException(label + " vượt quá giới hạn " + max + " ký tự."); }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static String empty(String value) { return blank(value) ? null : value.trim(); }
    private static String normalize(String value) { return value == null ? "" : value.trim(); }
}
