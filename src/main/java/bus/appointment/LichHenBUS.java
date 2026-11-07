package bus.appointment;

import bus.auth.AuthorizationBUS;
import dao.appointment.AppointmentDAOException;
import dao.appointment.LichHenDAO;
import java.time.LocalDate;
import java.util.List;
import model.appointment.AppointmentDoctor;
import model.appointment.AppointmentDraft;
import model.appointment.AppointmentFilter;
import model.appointment.AppointmentHistoryEntry;
import model.appointment.AppointmentPage;
import model.appointment.AppointmentPatient;
import model.appointment.AppointmentSlot;
import model.appointment.AppointmentSpecialty;
import model.appointment.AppointmentStats;
import model.appointment.LichHen;
import model.auth.AuthenticatedSession;

public final class LichHenBUS {
    private static final int PAGE_SIZE_MAX = 100;
    private final LichHenDAO appointmentDAO;
    private final AuthorizationBUS authorizationBUS;

    public LichHenBUS() {
        this(new LichHenDAO(), new AuthorizationBUS());
    }

    LichHenBUS(LichHenDAO appointmentDAO, AuthorizationBUS authorizationBUS) {
        this.appointmentDAO = appointmentDAO;
        this.authorizationBUS = authorizationBUS;
    }

    public AppointmentPage search(
            AuthenticatedSession session, AppointmentFilter filter)
            throws AppointmentException {
        requireAccess(session);
        if (filter == null || filter.page() < 0
                || filter.pageSize() < 1 || filter.pageSize() > PAGE_SIZE_MAX
                || (filter.fromDate() != null && filter.toDate() != null
                && filter.fromDate().isAfter(filter.toDate()))) {
            throw new AppointmentException("Điều kiện tìm kiếm lịch hẹn không hợp lệ.");
        }
        AppointmentFilter normalized = new AppointmentFilter(
                normalize(filter.searchText()),
                filter.fromDate(),
                filter.toDate(),
                normalize(filter.status()),
                normalize(filter.doctorId()),
                normalize(filter.specialtyId()),
                filter.page(),
                filter.pageSize());
        try {
            return appointmentDAO.search(normalized);
        } catch (AppointmentDAOException exception) {
            throw toAppointmentException(exception);
        }
    }

    public AppointmentStats getTodayStats(AuthenticatedSession session)
            throws AppointmentException {
        requireAccess(session);
        try {
            return appointmentDAO.getTodayStats();
        } catch (AppointmentDAOException exception) {
            throw toAppointmentException(exception);
        }
    }

    public LocalDate getDatabaseDate(AuthenticatedSession session)
            throws AppointmentException {
        requireAccess(session);
        try {
            return appointmentDAO.getDatabaseDate();
        } catch (AppointmentDAOException exception) {
            throw toAppointmentException(exception);
        }
    }

    public List<String> getStatuses(AuthenticatedSession session)
            throws AppointmentException {
        requireAccess(session);
        try {
            return appointmentDAO.findStatuses();
        } catch (AppointmentDAOException exception) {
            throw toAppointmentException(exception);
        }
    }

    public List<AppointmentPatient> searchPatients(
            AuthenticatedSession session, String searchText)
            throws AppointmentException {
        requireAccess(session);
        if (searchText != null && searchText.length() > 150) {
            throw new AppointmentException("Từ khóa tìm bệnh nhân quá dài.");
        }
        try {
            return appointmentDAO.searchPatients(normalize(searchText));
        } catch (AppointmentDAOException exception) {
            throw toAppointmentException(exception);
        }
    }

    public List<AppointmentSpecialty> getSpecialties(AuthenticatedSession session)
            throws AppointmentException {
        requireAccess(session);
        try {
            return appointmentDAO.findSpecialties();
        } catch (AppointmentDAOException exception) {
            throw toAppointmentException(exception);
        }
    }

    public List<AppointmentDoctor> getDoctors(
            AuthenticatedSession session, String specialtyId, LocalDate date)
            throws AppointmentException {
        requireAccess(session);
        String normalizedSpecialty = normalize(specialtyId);
        if (normalizedSpecialty.isEmpty() || normalizedSpecialty.length() > 20 || date == null) {
            throw new AppointmentException("Chuyên khoa hoặc ngày khám không hợp lệ.");
        }
        try {
            return appointmentDAO.findDoctors(normalizedSpecialty, date);
        } catch (AppointmentDAOException exception) {
            throw toAppointmentException(exception);
        }
    }

    public List<AppointmentDoctor> getDoctors(AuthenticatedSession session)
            throws AppointmentException {
        requireAccess(session);
        try {
            return appointmentDAO.findDoctors();
        } catch (AppointmentDAOException exception) {
            throw toAppointmentException(exception);
        }
    }

    public List<AppointmentSlot> getAvailableSlots(
            AuthenticatedSession session,
            LocalDate date,
            String doctorId,
            String specialtyId,
            String patientId,
            Long excludeAppointmentId) throws AppointmentException {
        requireAccess(session);
        String normalizedDoctor = normalize(doctorId);
        String normalizedSpecialty = normalize(specialtyId);
        String normalizedPatient = normalize(patientId);
        if (date == null || normalizedDoctor.isEmpty()
                || normalizedDoctor.length() > 20
                || normalizedSpecialty.isEmpty() || normalizedSpecialty.length() > 20
                || normalizedPatient.isEmpty() || normalizedPatient.length() > 20
                || (excludeAppointmentId != null && excludeAppointmentId <= 0)) {
            throw new AppointmentException("Thông tin tìm khung giờ không hợp lệ.");
        }
        try {
            return appointmentDAO.findAvailableSlots(
                    date, normalizedDoctor, normalizedSpecialty,
                    normalizedPatient, excludeAppointmentId);
        } catch (AppointmentDAOException exception) {
            throw toAppointmentException(exception);
        }
    }

    public LichHen getById(AuthenticatedSession session, long appointmentId)
            throws AppointmentException {
        requireAccess(session);
        if (appointmentId <= 0) {
            throw new AppointmentException("Mã lịch hẹn không hợp lệ.");
        }
        try {
            LichHen appointment = appointmentDAO.findById(appointmentId);
            if (appointment == null) {
                throw new AppointmentException("Không tìm thấy lịch hẹn.");
            }
            return appointment;
        } catch (AppointmentDAOException exception) {
            throw toAppointmentException(exception);
        }
    }

    public List<AppointmentHistoryEntry> getHistory(
            AuthenticatedSession session, long appointmentId)
            throws AppointmentException {
        requireAccess(session);
        if (appointmentId <= 0) {
            throw new AppointmentException("Mã lịch hẹn không hợp lệ.");
        }
        try {
            return appointmentDAO.findHistory(appointmentId);
        } catch (AppointmentDAOException exception) {
            throw toAppointmentException(exception);
        }
    }

    public long create(AuthenticatedSession session, AppointmentDraft draft)
            throws AppointmentException {
        requireEdit(session);
        AppointmentDraft normalized = validateDraft(draft);
        try {
            return appointmentDAO.insert(normalized, session.getEmployeeId());
        } catch (AppointmentDAOException exception) {
            throw toAppointmentException(exception);
        }
    }

    public void update(
            AuthenticatedSession session, long appointmentId, AppointmentDraft draft)
            throws AppointmentException {
        requireEdit(session);
        if (appointmentId <= 0) {
            throw new AppointmentException("Mã lịch hẹn không hợp lệ.");
        }
        AppointmentDraft normalized = validateDraft(draft);
        try {
            appointmentDAO.update(appointmentId, normalized, session.getEmployeeId());
        } catch (AppointmentDAOException exception) {
            throw toAppointmentException(exception);
        }
    }

    public void confirm(AuthenticatedSession session, long appointmentId)
            throws AppointmentException {
        requireEdit(session);
        if (appointmentId <= 0) {
            throw new AppointmentException("Mã lịch hẹn không hợp lệ.");
        }
        try {
            appointmentDAO.confirm(appointmentId, session.getEmployeeId());
        } catch (AppointmentDAOException exception) {
            throw toAppointmentException(exception);
        }
    }

    public void cancel(
            AuthenticatedSession session, long appointmentId, String reason)
            throws AppointmentException {
        requireEdit(session);
        if (appointmentId <= 0) {
            throw new AppointmentException("Mã lịch hẹn không hợp lệ.");
        }
        String normalizedReason = normalize(reason);
        if (normalizedReason.length() > 255) {
            throw new AppointmentException("Lý do hủy không được vượt quá 255 ký tự.");
        }
        try {
            appointmentDAO.cancel(
                    appointmentId, session.getEmployeeId(), normalizedReason);
        } catch (AppointmentDAOException exception) {
            throw toAppointmentException(exception);
        }
    }

    private AppointmentDraft validateDraft(AppointmentDraft draft)
            throws AppointmentException {
        if (draft == null) {
            throw new AppointmentException("Thông tin lịch hẹn không hợp lệ.");
        }
        String patientId = normalize(draft.maBenhNhan());
        String doctorId = normalize(draft.maNhanVien());
        String specialtyId = normalize(draft.maChuyenKhoa());
        String reason = normalize(draft.lyDoKham());
        String note = normalize(draft.ghiChu());
        if (patientId.isEmpty() || patientId.length() > 20
                || doctorId.isEmpty() || doctorId.length() > 20
                || specialtyId.isEmpty() || specialtyId.length() > 20
                || draft.maKhungGio() <= 0) {
            throw new AppointmentException(
                    "Cần chọn bệnh nhân, chuyên khoa, bác sĩ và khung giờ hợp lệ.");
        }
        if (reason.length() > 500) {
            throw new AppointmentException("Lý do khám không được vượt quá 500 ký tự.");
        }
        if (note.length() > 255) {
            throw new AppointmentException("Ghi chú không được vượt quá 255 ký tự.");
        }
        return new AppointmentDraft(
                patientId, doctorId, specialtyId, draft.maKhungGio(),
                reason.isEmpty() ? null : reason, note.isEmpty() ? null : note);
    }

    private void requireAccess(AuthenticatedSession session)
            throws AppointmentException {
        try {
            authorizationBUS.requirePageAccess(session, "Lịch hẹn");
        } catch (SecurityException exception) {
            throw new AppointmentException(exception.getMessage(), exception);
        }
    }

    private void requireEdit(AuthenticatedSession session)
            throws AppointmentException {
        requireAccess(session);
        if (session.getEmployeeId() == null || session.getEmployeeId().isBlank()) {
            throw new AppointmentException(
                    "Phiên đăng nhập không liên kết với nhân viên để thực hiện thao tác.");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private AppointmentException toAppointmentException(
            AppointmentDAOException exception) {
        return new AppointmentException(exception.getMessage(), exception);
    }
}
