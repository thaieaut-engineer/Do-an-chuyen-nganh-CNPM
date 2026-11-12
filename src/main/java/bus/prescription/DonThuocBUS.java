package bus.prescription;

import bus.auth.AuthorizationBUS;
import dao.prescription.DonThuocDAO;
import dao.prescription.PrescriptionDAOException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import model.appointment.AppointmentDoctor;
import model.appointment.AppointmentSpecialty;
import model.auth.AuthenticatedSession;
import model.prescription.EligiblePrescriptionVisit;
import model.prescription.MedicationCatalogItem;
import model.prescription.PrescriptionDetail;
import model.prescription.PrescriptionDraft;
import model.prescription.PrescriptionFilter;
import model.prescription.PrescriptionLineDraft;
import model.prescription.PrescriptionListItem;
import model.prescription.PrescriptionPage;
import model.prescription.PrescriptionStats;

public final class DonThuocBUS {
    private static final int PAGE_SIZE_MAX = 100;
    private static final int SEARCH_LENGTH_MAX = 150;
    private static final int MAX_LINES = 100;
    private static final int MAX_TEXT_LENGTH = 100_000;
    private static final BigDecimal MAX_QUANTITY =
            new BigDecimal("9999999999.99");

    private final DonThuocDAO prescriptionDAO;
    private final AuthorizationBUS authorizationBUS;

    public DonThuocBUS() {
        this(new DonThuocDAO(), new AuthorizationBUS());
    }

    DonThuocBUS(DonThuocDAO prescriptionDAO, AuthorizationBUS authorizationBUS) {
        this.prescriptionDAO = prescriptionDAO;
        this.authorizationBUS = authorizationBUS;
    }

    public PrescriptionStats getStats(AuthenticatedSession session)
            throws PrescriptionException {
        requireAccess(session);
        try {
            return prescriptionDAO.getStats(
                    session.getEmployeeId(), canManageAnyDoctor(session));
        } catch (PrescriptionDAOException exception) {
            throw databaseError(exception);
        }
    }

    public PrescriptionPage search(
            AuthenticatedSession session, PrescriptionFilter filter)
            throws PrescriptionException {
        requireAccess(session);
        if (filter == null || filter.page() < 0 || filter.pageSize() < 1
                || filter.pageSize() > PAGE_SIZE_MAX
                || (filter.fromDate() != null && filter.toDate() != null
                && filter.fromDate().isAfter(filter.toDate()))
                || (filter.searchText() != null
                && filter.searchText().length() > SEARCH_LENGTH_MAX)) {
            throw new PrescriptionException("Điều kiện tìm kiếm đơn thuốc không hợp lệ.");
        }
        try {
            return prescriptionDAO.findPage(
                    normalize(filter),
                    session.getEmployeeId(),
                    canManageAnyDoctor(session));
        } catch (PrescriptionDAOException exception) {
            throw databaseError(exception);
        }
    }

    public List<String> getStatuses(AuthenticatedSession session)
            throws PrescriptionException {
        requireAccess(session);
        try {
            return prescriptionDAO.findStatuses();
        } catch (PrescriptionDAOException exception) {
            throw databaseError(exception);
        }
    }

    public List<AppointmentDoctor> getDoctors(AuthenticatedSession session)
            throws PrescriptionException {
        requireAccess(session);
        try {
            List<AppointmentDoctor> doctors = prescriptionDAO.findDoctors();
            if (canManageAnyDoctor(session)) {
                return doctors;
            }
            return doctors.stream()
                    .filter(doctor -> session.getEmployeeId()
                            .equals(doctor.maNhanVien()))
                    .toList();
        } catch (PrescriptionDAOException exception) {
            throw databaseError(exception);
        }
    }

    public List<AppointmentSpecialty> getSpecialties(AuthenticatedSession session)
            throws PrescriptionException {
        requireAccess(session);
        try {
            return prescriptionDAO.findSpecialties();
        } catch (PrescriptionDAOException exception) {
            throw databaseError(exception);
        }
    }

    public List<MedicationCatalogItem> getMedications(
            AuthenticatedSession session, String searchText)
            throws PrescriptionException {
        requireAccess(session);
        if (searchText != null && searchText.length() > SEARCH_LENGTH_MAX) {
            throw new PrescriptionException("Từ khóa tìm thuốc quá dài.");
        }
        try {
            return prescriptionDAO.findMedications(normalize(searchText));
        } catch (PrescriptionDAOException exception) {
            throw databaseError(exception);
        }
    }

    public List<EligiblePrescriptionVisit> getEligibleVisits(
            AuthenticatedSession session) throws PrescriptionException {
        requireAccess(session);
        try {
            return prescriptionDAO.findEligibleVisits(
                    session.getEmployeeId(), canManageAnyDoctor(session));
        } catch (PrescriptionDAOException exception) {
            throw databaseError(exception);
        }
    }

    public PrescriptionDetail getById(AuthenticatedSession session, long prescriptionId)
            throws PrescriptionException {
        requireAccess(session);
        if (prescriptionId <= 0) {
            throw new PrescriptionException("Mã đơn thuốc không hợp lệ.");
        }
        try {
            PrescriptionDetail detail = prescriptionDAO.findById(
                    prescriptionId,
                    session.getEmployeeId(),
                    canManageAnyDoctor(session));
            if (detail == null) {
                throw new PrescriptionException(
                        "Không tìm thấy đơn thuốc hoặc bạn không có quyền xem.");
            }
            return detail;
        } catch (PrescriptionDAOException exception) {
            throw databaseError(exception);
        }
    }

    public long create(AuthenticatedSession session, PrescriptionDraft draft)
            throws PrescriptionException {
        requireAccess(session);
        validateDraft(draft, true);
        try {
            return prescriptionDAO.create(
                    draft,
                    session.getEmployeeId(),
                    session.getAccountId(),
                    canManageAnyDoctor(session));
        } catch (PrescriptionDAOException exception) {
            throw databaseError(exception);
        }
    }

    public void update(
            AuthenticatedSession session,
            long prescriptionId,
            PrescriptionDraft draft) throws PrescriptionException {
        requireAccess(session);
        if (prescriptionId <= 0) {
            throw new PrescriptionException("Mã đơn thuốc không hợp lệ.");
        }
        validateDraft(draft, false);
        try {
            prescriptionDAO.update(
                    prescriptionId,
                    draft,
                    session.getEmployeeId(),
                    session.getAccountId(),
                    canManageAnyDoctor(session));
        } catch (PrescriptionDAOException exception) {
            throw databaseError(exception);
        }
    }

    public void cancel(AuthenticatedSession session, long prescriptionId)
            throws PrescriptionException {
        requireAccess(session);
        if (prescriptionId <= 0) {
            throw new PrescriptionException("Mã đơn thuốc không hợp lệ.");
        }
        try {
            prescriptionDAO.cancel(
                    prescriptionId,
                    session.getEmployeeId(),
                    session.getAccountId(),
                    canManageAnyDoctor(session));
        } catch (PrescriptionDAOException exception) {
            throw databaseError(exception);
        }
    }

    public boolean canEdit(AuthenticatedSession session, PrescriptionListItem item) {
        return canWrite(session) && item != null
                && "Nhap".equals(item.trangThai())
                && !item.daXuatThuoc()
                && (canManageAnyDoctor(session)
                || session.getEmployeeId().equals(item.maBacSi()));
    }

    public boolean canCancel(AuthenticatedSession session, PrescriptionListItem item) {
        return canEdit(session, item);
    }

    public boolean canEdit(AuthenticatedSession session, PrescriptionDetail detail) {
        return canWrite(session) && detail != null
                && "Nhap".equals(detail.trangThai())
                && ("DangKham".equals(detail.trangThaiLuotKham())
                || "ChoKetQua".equals(detail.trangThaiLuotKham()))
                && !detail.daXuatThuoc()
                && (canManageAnyDoctor(session)
                || session.getEmployeeId().equals(detail.maBacSi()));
    }

    private void validateDraft(PrescriptionDraft draft, boolean creating)
            throws PrescriptionException {
        if (draft == null || draft.maLuotKham() <= 0) {
            throw new PrescriptionException("Hãy chọn lượt khám hợp lệ.");
        }
        if (draft.loiDan() != null && draft.loiDan().length() > MAX_TEXT_LENGTH) {
            throw new PrescriptionException("Lời dặn vượt quá giới hạn nhập liệu.");
        }
        if (draft.lines().isEmpty()) {
            throw new PrescriptionException("Đơn thuốc phải có ít nhất một thuốc.");
        }
        if (draft.lines().size() > MAX_LINES) {
            throw new PrescriptionException("Đơn thuốc không được vượt quá 100 dòng thuốc.");
        }
        Set<String> medicationIds = new HashSet<>();
        for (PrescriptionLineDraft line : draft.lines()) {
            if (line == null) {
                throw new PrescriptionException("Dòng thuốc không hợp lệ.");
            }
            if (creating && line.maChiTietDon() != null) {
                throw new PrescriptionException(
                        "Dòng thuốc mới không được gửi kèm mã chi tiết đã lưu.");
            }
            if (line.maThuoc() == null || line.maThuoc().isBlank()
                    || line.maThuoc().length() > 20) {
                throw new PrescriptionException("Hãy chọn thuốc hợp lệ.");
            }
            if (!medicationIds.add(line.maThuoc())) {
                throw new PrescriptionException(
                        "Một thuốc chỉ được kê một dòng trong cùng đơn.");
            }
            requireText("Liều dùng", line.lieuDung(), 150, true);
            requireText("Đường dùng", line.duongDung(), 80, false);
            requireText("Tần suất", line.tanSuat(), 100, false);
            requireText("Thời gian dùng", line.thoiGianDung(), 100, false);
            requireText("Đơn vị tính", line.donViTinh(), 30, false);
            requireText("Hướng dẫn", line.huongDan(), MAX_TEXT_LENGTH, false);
            BigDecimal quantity = line.soLuong();
            if (quantity == null || quantity.signum() <= 0
                    || quantity.compareTo(MAX_QUANTITY) > 0
                    || quantity.stripTrailingZeros().scale() > 2) {
                throw new PrescriptionException(
                        "Số lượng phải lớn hơn 0 và tối đa 2 chữ số thập phân.");
            }
        }
    }

    private void requireText(
            String name, String value, int maxLength, boolean required)
            throws PrescriptionException {
        if (required && (value == null || value.isBlank())) {
            throw new PrescriptionException(name + " là bắt buộc.");
        }
        if (value != null && value.length() > maxLength) {
            throw new PrescriptionException(
                    name + " vượt quá giới hạn " + maxLength + " ký tự.");
        }
    }

    private void requireAccess(AuthenticatedSession session)
            throws PrescriptionException {
        if (session == null || !session.isActive()) {
            throw new PrescriptionException(
                    "Bạn chưa đăng nhập hoặc phiên làm việc đã hết hạn.");
        }
        try {
            authorizationBUS.requirePageAccess(session, "Kê đơn thuốc");
            authorizationBUS.requirePermission(session, "DONTHUOC_KE");
        } catch (SecurityException exception) {
            throw new PrescriptionException(exception.getMessage(), exception);
        }
    }

    private boolean canWrite(AuthenticatedSession session) {
        return session != null && session.isActive()
                && session.hasPermission("DONTHUOC_KE");
    }

    private boolean canManageAnyDoctor(AuthenticatedSession session) {
        return session.hasPermission("NHANSU_SUA");
    }

    private PrescriptionFilter normalize(PrescriptionFilter filter) {
        return new PrescriptionFilter(
                normalize(filter.searchText()),
                filter.fromDate(),
                filter.toDate(),
                normalize(filter.status()),
                normalize(filter.doctorId()),
                normalize(filter.specialtyId()),
                filter.page(),
                filter.pageSize());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private PrescriptionException databaseError(PrescriptionDAOException cause) {
        return new PrescriptionException(cause.getMessage(), cause);
    }
}
