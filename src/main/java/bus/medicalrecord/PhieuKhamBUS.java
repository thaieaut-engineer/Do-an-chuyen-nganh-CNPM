package bus.medicalrecord;

import bus.auth.AuthorizationBUS;
import dao.medicalrecord.MedicalRecordDAOException;
import dao.medicalrecord.PhieuKhamDAO;
import java.time.LocalDate;
import java.util.List;
import model.appointment.AppointmentDoctor;
import model.appointment.AppointmentSpecialty;
import model.auth.AuthenticatedSession;
import model.medicalrecord.EligibleVisit;
import model.medicalrecord.MedicalRecordDetail;
import model.medicalrecord.MedicalRecordDraft;
import model.medicalrecord.MedicalRecordFilter;
import model.medicalrecord.MedicalRecordListItem;
import model.medicalrecord.MedicalRecordPage;
import model.medicalrecord.MedicalRecordStats;

public final class PhieuKhamBUS {
    private static final int PAGE_SIZE_MAX = 100;
    private static final int TEXT_LENGTH_MAX = 100_000;
    private final PhieuKhamDAO recordDAO;
    private final AuthorizationBUS authorizationBUS;

    public PhieuKhamBUS() {
        this(new PhieuKhamDAO(), new AuthorizationBUS());
    }

    PhieuKhamBUS(PhieuKhamDAO recordDAO, AuthorizationBUS authorizationBUS) {
        this.recordDAO = recordDAO;
        this.authorizationBUS = authorizationBUS;
    }

    public MedicalRecordStats getStats(AuthenticatedSession session)
            throws MedicalRecordException {
        requireAccess(session);
        try {
            return recordDAO.getStats(
                    session.getEmployeeId(), canManageAnyDoctor(session));
        } catch (MedicalRecordDAOException exception) {
            throw new MedicalRecordException(exception.getMessage(), exception);
        }
    }

    public MedicalRecordPage search(
            AuthenticatedSession session, MedicalRecordFilter filter)
            throws MedicalRecordException {
        requireAccess(session);
        if (filter == null || filter.page() < 0 || filter.pageSize() < 1
                || filter.pageSize() > PAGE_SIZE_MAX
                || (filter.fromDate() != null && filter.toDate() != null
                && filter.fromDate().isAfter(filter.toDate()))
                || (filter.searchText() != null && filter.searchText().length() > 150)) {
            throw new MedicalRecordException("Điều kiện tìm kiếm phiếu khám không hợp lệ.");
        }
        try {
            return recordDAO.findPage(
                    normalizeFilter(filter),
                    session.getEmployeeId(),
                    canManageAnyDoctor(session));
        } catch (MedicalRecordDAOException exception) {
            throw new MedicalRecordException(exception.getMessage(), exception);
        }
    }

    public List<String> getStatuses(AuthenticatedSession session)
            throws MedicalRecordException {
        requireAccess(session);
        try {
            return recordDAO.findStatuses();
        } catch (MedicalRecordDAOException exception) {
            throw new MedicalRecordException(exception.getMessage(), exception);
        }
    }

    public List<AppointmentDoctor> getDoctors(AuthenticatedSession session)
            throws MedicalRecordException {
        requireAccess(session);
        try {
            List<AppointmentDoctor> doctors = recordDAO.findDoctors();
            if (canManageAnyDoctor(session)) {
                return doctors;
            }
            return doctors.stream()
                    .filter(doctor -> session.getEmployeeId().equals(doctor.maNhanVien()))
                    .toList();
        } catch (MedicalRecordDAOException exception) {
            throw new MedicalRecordException(exception.getMessage(), exception);
        }
    }

    public List<AppointmentSpecialty> getSpecialties(AuthenticatedSession session)
            throws MedicalRecordException {
        requireAccess(session);
        try {
            return recordDAO.findSpecialties();
        } catch (MedicalRecordDAOException exception) {
            throw new MedicalRecordException(exception.getMessage(), exception);
        }
    }

    public List<EligibleVisit> getEligibleVisits(AuthenticatedSession session)
            throws MedicalRecordException {
        requireAccess(session);
        try {
            return recordDAO.findEligibleVisits(
                    session.getEmployeeId(), canManageAnyDoctor(session));
        } catch (MedicalRecordDAOException exception) {
            throw new MedicalRecordException(exception.getMessage(), exception);
        }
    }

    public MedicalRecordDetail getById(AuthenticatedSession session, long recordId)
            throws MedicalRecordException {
        requireAccess(session);
        if (recordId <= 0) {
            throw new MedicalRecordException("Mã phiếu khám không hợp lệ.");
        }
        try {
            MedicalRecordDetail record = recordDAO.findById(recordId);
            if (record == null) {
                throw new MedicalRecordException("Không tìm thấy phiếu khám.");
            }
            requireAssignedDoctorOrAdministrator(session, record.maNhanVien());
            return record;
        } catch (MedicalRecordDAOException exception) {
            throw new MedicalRecordException(exception.getMessage(), exception);
        }
    }

    public long create(AuthenticatedSession session, MedicalRecordDraft draft)
            throws MedicalRecordException {
        requireAccess(session);
        validateDraft(draft);
        try {
            return recordDAO.create(
                    draft,
                    session.getEmployeeId(),
                    session.getAccountId(),
                    canManageAnyDoctor(session));
        } catch (MedicalRecordDAOException exception) {
            throw new MedicalRecordException(exception.getMessage(), exception);
        }
    }

    public void update(
            AuthenticatedSession session, long recordId, MedicalRecordDraft draft)
            throws MedicalRecordException {
        requireAccess(session);
        if (recordId <= 0) {
            throw new MedicalRecordException("Mã phiếu khám không hợp lệ.");
        }
        validateDraft(draft);
        try {
            recordDAO.update(
                    recordId,
                    draft,
                    session.getEmployeeId(),
                    session.getAccountId(),
                    canManageAnyDoctor(session));
        } catch (MedicalRecordDAOException exception) {
            throw new MedicalRecordException(exception.getMessage(), exception);
        }
    }

    public boolean canEdit(AuthenticatedSession session, MedicalRecordListItem record) {
        return session != null && session.isActive()
                && session.hasPermission("KHAM_HOSO")
                && record != null && record.canEdit()
                && (canManageAnyDoctor(session)
                || session.getEmployeeId().equals(record.maNhanVien()));
    }

    public boolean canEdit(AuthenticatedSession session, MedicalRecordDetail record) {
        if (session == null || !session.isActive()
                || !session.hasPermission("KHAM_HOSO")
                || record == null || !"Nhap".equals(record.trangThaiPhieu())
                || (!"DangKham".equals(record.trangThaiLuotKham())
                && !"ChoKetQua".equals(record.trangThaiLuotKham()))) {
            return false;
        }
        return canManageAnyDoctor(session)
                || session.getEmployeeId().equals(record.maNhanVien());
    }

    private MedicalRecordFilter normalizeFilter(MedicalRecordFilter filter) {
        return new MedicalRecordFilter(
                normalize(filter.searchText()),
                filter.fromDate(),
                filter.toDate(),
                normalize(filter.status()),
                normalize(filter.doctorId()),
                normalize(filter.specialtyId()),
                filter.page(),
                filter.pageSize());
    }

    private void validateDraft(MedicalRecordDraft draft) throws MedicalRecordException {
        if (draft == null || draft.maLuotKham() <= 0) {
            throw new MedicalRecordException("Cần chọn lượt khám hợp lệ để lập phiếu.");
        }
        validateLength("Triệu chứng", draft.trieuChung());
        validateLength("Dấu hiệu sinh tồn", draft.dauHieuSinhTon());
        validateLength("Lý do khám", draft.lyDoKham());
        validateLength("Kết quả lâm sàng", draft.ketQuaLamSang());
        validateLength("Chẩn đoán", draft.chanDoan());
        validateLength("Hướng điều trị", draft.huongDieuTri());
        if (isBlank(draft.trieuChung()) || isBlank(draft.ketQuaLamSang())
                || isBlank(draft.chanDoan())) {
            throw new MedicalRecordException(
                    "Vui lòng nhập Triệu chứng, Kết quả lâm sàng và Chẩn đoán.");
        }
    }

    private void validateLength(String field, String value) throws MedicalRecordException {
        if (value != null && value.length() > TEXT_LENGTH_MAX) {
            throw new MedicalRecordException(field + " vượt quá giới hạn nhập liệu.");
        }
    }

    private void requireAccess(AuthenticatedSession session)
            throws MedicalRecordException {
        if (session == null || !session.isActive()) {
            throw new MedicalRecordException(
                    "Bạn chưa đăng nhập hoặc phiên làm việc đã hết hạn.");
        }
        try {
            authorizationBUS.requirePageAccess(session, "Phiếu khám");
            authorizationBUS.requirePermission(session, "KHAM_HOSO");
        } catch (SecurityException exception) {
            throw new MedicalRecordException(exception.getMessage(), exception);
        }
    }

    private void requireAssignedDoctorOrAdministrator(
            AuthenticatedSession session, String assignedDoctorId)
            throws MedicalRecordException {
        if (!canManageAnyDoctor(session)
                && !session.getEmployeeId().equals(assignedDoctorId)) {
            throw new MedicalRecordException(
                    "Bác sĩ chỉ được xem phiếu khám của lượt do mình phụ trách.");
        }
    }

    private boolean canManageAnyDoctor(AuthenticatedSession session) {
        return session.hasPermission("NHANSU_SUA");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
