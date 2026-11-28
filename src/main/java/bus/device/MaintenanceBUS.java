package bus.device;

import bus.auth.AuthorizationBUS;
import dao.device.MaintenanceDAO;
import java.util.List;
import java.util.Set;
import model.auth.AuthenticatedSession;
import model.device.MaintenanceModels.*;

public final class MaintenanceBUS {
    private static final Set<String> SCHEDULE_STATUSES = Set.of("LenLich", "DaThucHien", "QuaHan", "Huy");
    private static final Set<String> REPAIR_STATUSES = Set.of("MoiBao", "DangXuLy", "DaSua", "KhongTheSua", "Huy");
    private final MaintenanceDAO dao = new MaintenanceDAO();
    private final AuthorizationBUS auth = new AuthorizationBUS();

    public List<Schedule> schedules(AuthenticatedSession session, String keyword) { access(session); return dao.schedules(normalize(keyword)); }
    public List<Repair> repairs(AuthenticatedSession session, String keyword) { access(session); return dao.repairs(normalize(keyword)); }

    public void save(AuthenticatedSession session, ScheduleDraft draft) {
        access(session);
        if (draft == null || blank(draft.deviceId()) || draft.expectedDate() == null || blank(draft.status())) throw new DeviceException("Thiết bị, ngày dự kiến và trạng thái là bắt buộc.");
        if (!SCHEDULE_STATUSES.contains(draft.status())) throw new DeviceException("Trạng thái lịch bảo dưỡng không hợp lệ.");
        if (draft.completedDate() != null && draft.completedDate().isBefore(draft.expectedDate())) throw new DeviceException("Ngày thực hiện không được trước ngày dự kiến.");
        if ("DaThucHien".equals(draft.status()) && draft.completedDate() == null) throw new DeviceException("Lịch đã thực hiện phải có ngày thực hiện.");
        if (draft.cost() != null && draft.cost().signum() < 0) throw new DeviceException("Chi phí không được âm.");
        checkLength(draft.deviceId(), 20, "Mã thiết bị"); checkLength(draft.vendor(), 150, "Đơn vị thực hiện"); checkLength(draft.content(), 255, "Nội dung"); checkLength(draft.employeeId(), 20, "Mã nhân viên phụ trách");
        dao.save(new Schedule(draft.id(), draft.deviceId().trim(), null, draft.expectedDate(), draft.completedDate(), draft.content(), draft.vendor(), empty(draft.employeeId()), draft.cost(), draft.status()));
    }

    public void save(AuthenticatedSession session, RepairDraft draft) {
        access(session);
        if (draft == null || blank(draft.deviceId()) || blank(draft.description()) || blank(draft.status())) throw new DeviceException("Thiết bị, mô tả sự cố và trạng thái là bắt buộc.");
        if (!REPAIR_STATUSES.contains(draft.status())) throw new DeviceException("Trạng thái phiếu sửa chữa không hợp lệ.");
        if ("DaSua".equals(draft.status()) && draft.repairedDate() == null) throw new DeviceException("Phiếu đã sửa phải có ngày sửa.");
        if (draft.cost() != null && draft.cost().signum() < 0) throw new DeviceException("Chi phí không được âm.");
        checkLength(draft.deviceId(), 20, "Mã thiết bị"); checkLength(draft.vendor(), 150, "Đơn vị sửa chữa"); checkLength(draft.employeeId(), 20, "Mã nhân viên tiếp nhận"); checkLength(draft.note(), 255, "Ghi chú");
        dao.save(new Repair(draft.id(), draft.deviceId().trim(), null, null, draft.description().trim(), draft.repairedDate(), draft.vendor(), draft.cost(), draft.status(), empty(draft.employeeId()), draft.note()));
    }

    public void delete(AuthenticatedSession session, long id, boolean schedule) {
        access(session); if (id <= 0) throw new DeviceException("Mã bảo trì không hợp lệ.");
        if (schedule) dao.cancelSchedule(id); else dao.cancelRepair(id);
    }

    private void access(AuthenticatedSession session) { try { auth.requirePageAccess(session, "Thiết bị"); auth.requirePermission(session, "THIETBI_QUANLY"); } catch (SecurityException e) { throw new DeviceException(e.getMessage(), e); } }
    private static void checkLength(String value, int max, String label) { if (value != null && value.length() > max) throw new DeviceException(label + " vượt quá giới hạn " + max + " ký tự."); }
    private static boolean blank(String value) { return value == null || value.isBlank(); }
    private static String empty(String value) { return blank(value) ? null : value.trim(); }
    private static String normalize(String value) { return value == null ? "" : value.trim(); }
}
