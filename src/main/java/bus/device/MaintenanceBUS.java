package bus.device;

import bus.auth.AuthorizationBUS;
import dao.device.MaintenanceDAO;
import java.util.List;
import model.auth.AuthenticatedSession;
import model.device.MaintenanceModels.*;

public final class MaintenanceBUS {
    private final MaintenanceDAO dao = new MaintenanceDAO();
    private final AuthorizationBUS auth = new AuthorizationBUS();

    public List<Schedule> schedules(AuthenticatedSession session, String keyword) {
        access(session); return dao.schedules(normalize(keyword));
    }
    public List<Repair> repairs(AuthenticatedSession session, String keyword) {
        access(session); return dao.repairs(normalize(keyword));
    }
    public void save(AuthenticatedSession session, ScheduleDraft draft) {
        access(session);
        if (draft == null || blank(draft.deviceId()) || draft.expectedDate() == null || blank(draft.status())) throw new DeviceException("Thiết bị, ngày dự kiến và trạng thái là bắt buộc.");
        if (draft.completedDate() != null && draft.completedDate().isBefore(draft.expectedDate())) throw new DeviceException("Ngày thực hiện không được trước ngày dự kiến.");
        if (draft.cost() != null && draft.cost().signum() < 0) throw new DeviceException("Chi phí không được âm.");
        dao.save(new Schedule(draft.id(), draft.deviceId(), null, draft.expectedDate(), draft.completedDate(), draft.content(), draft.vendor(), draft.employeeId(), draft.cost(), draft.status()));
    }
    public void save(AuthenticatedSession session, RepairDraft draft) {
        access(session);
        if (draft == null || blank(draft.deviceId()) || blank(draft.description()) || blank(draft.status())) throw new DeviceException("Thiết bị, mô tả sự cố và trạng thái là bắt buộc.");
        if (draft.cost() != null && draft.cost().signum() < 0) throw new DeviceException("Chi phí không được âm.");
        dao.save(new Repair(draft.id(), draft.deviceId(), null, null, draft.description(), draft.repairedDate(), draft.vendor(), draft.cost(), draft.status(), draft.employeeId(), draft.note()));
    }
    public void delete(AuthenticatedSession session, long id, boolean schedule) {
        access(session); if (id <= 0) throw new DeviceException("Mã bảo trì không hợp lệ."); if (schedule) dao.deleteSchedule(id); else dao.deleteRepair(id);
    }
    private void access(AuthenticatedSession session) { try { auth.requirePageAccess(session, "Thiết bị"); auth.requirePermission(session, "THIETBI_QUANLY"); } catch (SecurityException e) { throw new DeviceException(e.getMessage(), e); } }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String normalize(String value) { return value == null ? "" : value.trim(); }
}
