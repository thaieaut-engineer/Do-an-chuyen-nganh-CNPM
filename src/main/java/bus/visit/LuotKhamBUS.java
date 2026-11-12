package bus.visit;

import bus.auth.AuthorizationBUS;
import dao.visit.LuotKhamDAO;
import dao.visit.VisitDAOException;
import java.time.LocalDate;
import java.util.List;
import model.appointment.AppointmentDoctor;
import model.appointment.AppointmentSpecialty;
import model.auth.AuthenticatedSession;
import model.visit.VisitDetail;
import model.visit.VisitFilter;
import model.visit.VisitPage;
import model.visit.VisitStats;

public final class LuotKhamBUS {
    private static final int PAGE_SIZE_MAX = 100;
    private final LuotKhamDAO visitDAO;
    private final AuthorizationBUS authorizationBUS;

    public LuotKhamBUS() {
        this(new LuotKhamDAO(), new AuthorizationBUS());
    }

    LuotKhamBUS(LuotKhamDAO visitDAO, AuthorizationBUS authorizationBUS) {
        this.visitDAO = visitDAO;
        this.authorizationBUS = authorizationBUS;
    }

    public VisitStats getTodayStats(AuthenticatedSession session) throws VisitException {
        requireAccess(session);
        try {
            return visitDAO.getTodayStats();
        } catch (VisitDAOException exception) {
            throw new VisitException(exception.getMessage(), exception);
        }
    }

    public VisitPage search(AuthenticatedSession session, VisitFilter filter)
            throws VisitException {
        requireAccess(session);
        if (filter == null || filter.page() < 0 || filter.pageSize() < 1
                || filter.pageSize() > PAGE_SIZE_MAX
                || (filter.fromDate() != null && filter.toDate() != null
                && filter.fromDate().isAfter(filter.toDate()))
                || (filter.searchText() != null && filter.searchText().length() > 150)) {
            throw new VisitException("Điều kiện tìm kiếm lượt khám không hợp lệ.");
        }
        try {
            return visitDAO.findPage(new VisitFilter(
                    normalize(filter.searchText()),
                    filter.fromDate(),
                    filter.toDate(),
                    normalize(filter.status()),
                    normalize(filter.doctorId()),
                    normalize(filter.specialtyId()),
                    filter.page(),
                    filter.pageSize()));
        } catch (VisitDAOException exception) {
            throw new VisitException(exception.getMessage(), exception);
        }
    }

    public List<String> getStatuses(AuthenticatedSession session) throws VisitException {
        requireAccess(session);
        try {
            return visitDAO.findStatuses();
        } catch (VisitDAOException exception) {
            throw new VisitException(exception.getMessage(), exception);
        }
    }

    public List<AppointmentDoctor> getDoctors(AuthenticatedSession session)
            throws VisitException {
        requireAccess(session);
        try {
            return visitDAO.findDoctors();
        } catch (VisitDAOException exception) {
            throw new VisitException(exception.getMessage(), exception);
        }
    }

    public List<AppointmentSpecialty> getSpecialties(AuthenticatedSession session)
            throws VisitException {
        requireAccess(session);
        try {
            return visitDAO.findSpecialties();
        } catch (VisitDAOException exception) {
            throw new VisitException(exception.getMessage(), exception);
        }
    }

    public VisitDetail getById(AuthenticatedSession session, long visitId)
            throws VisitException {
        requireAccess(session);
        if (visitId <= 0) {
            throw new VisitException("Mã lượt khám không hợp lệ.");
        }
        try {
            VisitDetail visit = visitDAO.findById(visitId);
            if (visit == null) {
                throw new VisitException("Không tìm thấy lượt khám.");
            }
            return visit;
        } catch (VisitDAOException exception) {
            throw new VisitException(exception.getMessage(), exception);
        }
    }

    public void advanceStatus(
            AuthenticatedSession session, long visitId, String targetStatus)
            throws VisitException {
        requireAccess(session);
        requirePermission(session, "KHAM_HOSO");
        if (visitId <= 0 || targetStatus == null
                || !List.of("DangKham", "ChoKetQua", "HoanTat")
                .contains(targetStatus)) {
            throw new VisitException("Trạng thái đích của lượt khám không hợp lệ.");
        }
        boolean administratorOverride = session.hasPermission("NHANSU_SUA");
        try {
            visitDAO.updateStatus(
                    visitId,
                    targetStatus,
                    session.getEmployeeId(),
                    session.getAccountId(),
                    administratorOverride);
        } catch (VisitDAOException exception) {
            throw new VisitException(exception.getMessage(), exception);
        }
    }

    public boolean canUpdateVisit(
            AuthenticatedSession session, String assignedDoctorId) {
        return session != null && session.isActive()
                && session.hasPermission("KHAM_HOSO")
                && (session.hasPermission("NHANSU_SUA")
                || session.getEmployeeId().equals(assignedDoctorId));
    }

    private void requireAccess(AuthenticatedSession session) throws VisitException {
        if (session == null || !session.isActive()) {
            throw new VisitException("Bạn chưa đăng nhập hoặc phiên làm việc đã hết hạn.");
        }
        try {
            authorizationBUS.requirePageAccess(session, "Lượt khám");
        } catch (SecurityException exception) {
            throw new VisitException(exception.getMessage(), exception);
        }
    }

    private void requirePermission(AuthenticatedSession session, String code)
            throws VisitException {
        try {
            authorizationBUS.requirePermission(session, code);
        } catch (SecurityException exception) {
            throw new VisitException(exception.getMessage(), exception);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
