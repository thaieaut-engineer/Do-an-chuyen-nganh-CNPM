package bus.reception;

import bus.auth.AuthorizationBUS;
import dao.reception.TiepNhanDAO;
import java.time.LocalDate;
import java.util.List;
import model.appointment.AppointmentDoctor;
import model.auth.AuthenticatedSession;
import model.reception.ReceptionAppointment;
import model.reception.ReceptionStats;
import model.reception.ReceptionVisit;

public final class TiepNhanBUS {
    private final TiepNhanDAO dao;
    private final AuthorizationBUS authorizationBUS;

    public TiepNhanBUS() {
        this(new TiepNhanDAO(), new AuthorizationBUS());
    }

    TiepNhanBUS(TiepNhanDAO dao, AuthorizationBUS authorizationBUS) {
        this.dao = dao;
        this.authorizationBUS = authorizationBUS;
    }

    public ReceptionStats getStats(AuthenticatedSession session) throws TiepNhanException {
        requireAccess(session);
        try {
            return dao.getStats();
        } catch (RuntimeException exception) {
            throw new TiepNhanException("Không thể tải thống kê tiếp nhận bệnh nhân.", exception);
        }
    }

    public List<ReceptionAppointment> getPendingAppointments(
            AuthenticatedSession session,
            String searchText,
            String status,
            LocalDate date,
            String doctorId) throws TiepNhanException {
        requireAccess(session);
        try {
            return dao.listAppointments(false, normalize(searchText), normalize(status), date, normalize(doctorId));
        } catch (RuntimeException exception) {
            throw new TiepNhanException("Không thể tải danh sách chờ tiếp nhận.", exception);
        }
    }

    public List<ReceptionVisit> getReceivedVisits(
            AuthenticatedSession session,
            String searchText,
            LocalDate date) throws TiepNhanException {
        requireAccess(session);
        try {
            return dao.listReceivedVisits(normalize(searchText), date);
        } catch (RuntimeException exception) {
            throw new TiepNhanException("Không thể tải danh sách đã tiếp nhận.", exception);
        }
    }

    public LocalDate getLatestAppointmentDate(AuthenticatedSession session) throws TiepNhanException {
        requireAccess(session);
        try {
            return dao.findLatestAppointmentDate();
        } catch (RuntimeException exception) {
            throw new TiepNhanException("Không thể tải ngày lịch hẹn gần nhất.", exception);
        }
    }

    public List<String> getStatuses(AuthenticatedSession session) throws TiepNhanException {
        requireAccess(session);
        try {
            return dao.findStatuses();
        } catch (RuntimeException exception) {
            throw new TiepNhanException("Không thể tải trạng thái lịch hẹn.", exception);
        }
    }

    public List<AppointmentDoctor> getDoctors(AuthenticatedSession session) throws TiepNhanException {
        requireAccess(session);
        try {
            return dao.findDoctors();
        } catch (RuntimeException exception) {
            throw new TiepNhanException("Không thể tải danh sách bác sĩ.", exception);
        }
    }

    public ReceptionAppointment getAppointment(AuthenticatedSession session, long appointmentId)
            throws TiepNhanException {
        requireAccess(session);
        if (appointmentId <= 0) {
            throw new TiepNhanException("Mã lịch hẹn không hợp lệ.");
        }
        try {
            ReceptionAppointment appointment = dao.getAppointmentDetail(appointmentId);
            if (appointment == null) {
                throw new TiepNhanException("Không tìm thấy lịch hẹn cần tiếp nhận.");
            }
            return appointment;
        } catch (RuntimeException exception) {
            throw new TiepNhanException("Không thể tải chi tiết lịch hẹn.", exception);
        }
    }

    public long receivePatient(AuthenticatedSession session, long appointmentId) throws TiepNhanException {
        requireAccess(session);
        requirePermission(session, "BN_SUA");
        if (appointmentId <= 0) {
            throw new TiepNhanException("Mã lịch hẹn không hợp lệ.");
        }
        try {
            return dao.createVisit(appointmentId, session.getEmployeeId());
        } catch (RuntimeException exception) {
            throw new TiepNhanException(exception.getMessage() == null
                    ? "Không thể tiếp nhận bệnh nhân." : exception.getMessage(), exception);
        }
    }

    private void requireAccess(AuthenticatedSession session) throws TiepNhanException {
        if (session == null || !session.isActive()) {
            throw new TiepNhanException("Bạn chưa đăng nhập hoặc phiên làm việc đã hết hạn.");
        }
        try {
            authorizationBUS.requirePageAccess(session, "Tiếp nhận bệnh nhân");
        } catch (SecurityException exception) {
            throw new TiepNhanException(exception.getMessage(), exception);
        }
    }

    private void requirePermission(AuthenticatedSession session, String code) throws TiepNhanException {
        try {
            authorizationBUS.requirePermission(session, code);
        } catch (SecurityException exception) {
            throw new TiepNhanException(exception.getMessage(), exception);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
