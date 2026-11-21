package bus.staff;

import bus.auth.AuthorizationBUS;
import dao.staff.StaffDAO;
import dao.staff.StaffDAOException;
import java.util.List;
import model.auth.AuthenticatedSession;
import model.staff.StaffModels.Staff;
import model.staff.StaffModels.StaffDraft;
import model.staff.StaffModels.Option;

public final class StaffBUS {
    private final StaffDAO dao = new StaffDAO();
    private final AuthorizationBUS auth = new AuthorizationBUS();

    public List<Staff> find(AuthenticatedSession s, String q, boolean doctors) {
        access(s);
        return run(() -> dao.find(q, doctors));
    }

    public List<Option> departments(AuthenticatedSession s) { access(s); return run(dao::departments); }
    public List<Option> titles(AuthenticatedSession s) { access(s); return run(dao::titles); }
    public List<Option> specialties(AuthenticatedSession s) { access(s); return run(dao::specialties); }

    public void save(AuthenticatedSession s, StaffDraft d, boolean doctor) {
        access(s);
        requirePermission(s, "NHANSU_SUA");
        if (d == null || d.id() == null || d.id().isBlank()
                || d.name() == null || d.name().isBlank()) {
            throw new StaffException("Mã nhân viên và họ tên là bắt buộc.");
        }
        if (d.startDate() == null) {
            throw new StaffException("Ngày bắt đầu làm việc là bắt buộc.");
        }
        run(() -> { dao.save(d, doctor); return null; });
    }

    private void access(AuthenticatedSession s) {
        try { auth.requirePageAccess(s, "Nhân viên"); }
        catch (SecurityException e) { throw new StaffException(e.getMessage(), e); }
    }

    private void requirePermission(AuthenticatedSession s, String code) {
        try { auth.requirePermission(s, code); }
        catch (SecurityException e) { throw new StaffException(e.getMessage(), e); }
    }

    private <T> T run(java.util.concurrent.Callable<T> x) {
        try { return x.call(); }
        catch (StaffDAOException e) { throw new StaffException(e.getMessage(), e); }
        catch (Exception e) { throw new StaffException(e.getMessage(), e); }
    }
}
