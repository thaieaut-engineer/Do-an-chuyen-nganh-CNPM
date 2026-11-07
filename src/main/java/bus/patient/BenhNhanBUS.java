package bus.patient;

import bus.auth.AuthorizationBUS;
import dao.patient.BenhNhanDAO;
import dao.patient.PatientDAOException;
import java.time.LocalDate;
import java.util.Set;
import java.util.regex.Pattern;
import model.auth.AuthenticatedSession;
import model.patient.BenhNhan;
import model.patient.PatientFilter;
import model.patient.PatientPage;

public final class BenhNhanBUS {
    private static final Set<String> GENDERS = Set.of("Nam", "Nữ", "Khác");
    private static final Set<String> STATUSES = Set.of("HoatDong", "TamKhoa");
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[0-9+().\\-\\s]{7,20}$");

    private final BenhNhanDAO patientDAO;
    private final AuthorizationBUS authorizationBUS;

    public BenhNhanBUS() {
        this(new BenhNhanDAO(), new AuthorizationBUS());
    }

    BenhNhanBUS(BenhNhanDAO patientDAO, AuthorizationBUS authorizationBUS) {
        this.patientDAO = patientDAO;
        this.authorizationBUS = authorizationBUS;
    }

    public PatientPage search(AuthenticatedSession session, PatientFilter filter)
            throws PatientException {
        requireViewPermission(session);
        if (filter == null) {
            throw new PatientException("Điều kiện tìm kiếm không hợp lệ.");
        }
        String gender = normalize(filter.gender());
        String status = normalize(filter.status());
        if (!gender.isEmpty() && !GENDERS.contains(gender)) {
            throw new PatientException("Bộ lọc giới tính không hợp lệ.");
        }
        if (!status.isEmpty() && !STATUSES.contains(status)) {
            throw new PatientException("Bộ lọc trạng thái hồ sơ không hợp lệ.");
        }
        if (filter.page() < 0 || filter.pageSize() < 1
                || filter.pageSize() > 100) {
            throw new PatientException("Thông tin phân trang không hợp lệ.");
        }
        PatientFilter normalized = new PatientFilter(
                normalize(filter.searchText()), gender, status,
                filter.page(), filter.pageSize());
        try {
            return patientDAO.search(normalized);
        } catch (PatientDAOException exception) {
            throw toPatientException(exception);
        }
    }

    public BenhNhan getById(AuthenticatedSession session, String patientId)
            throws PatientException {
        requireViewPermission(session);
        String normalizedId = normalize(patientId);
        if (normalizedId.isEmpty() || normalizedId.length() > 20) {
            throw new PatientException("Mã bệnh nhân không hợp lệ.");
        }
        try {
            BenhNhan patient = patientDAO.findById(normalizedId);
            if (patient == null) {
                throw new PatientException("Không tìm thấy hồ sơ bệnh nhân.");
            }
            return patient;
        } catch (PatientDAOException exception) {
            throw toPatientException(exception);
        }
    }

    public String create(AuthenticatedSession session, BenhNhan patient)
            throws PatientException {
        requireEditPermission(session);
        BenhNhan normalized = validateAndNormalize(patient);
        try {
            return patientDAO.insert(normalized);
        } catch (PatientDAOException exception) {
            throw toPatientException(exception);
        }
    }

    public void update(AuthenticatedSession session, BenhNhan patient)
            throws PatientException {
        requireEditPermission(session);
        if (patient == null || normalize(patient.maBenhNhan()).isEmpty()
                || normalize(patient.maBenhNhan()).length() > 20) {
            throw new PatientException("Mã bệnh nhân không hợp lệ.");
        }
        BenhNhan normalized = validateAndNormalize(patient);
        try {
            patientDAO.update(normalized);
        } catch (PatientDAOException exception) {
            throw toPatientException(exception);
        }
    }

    public void updateStatus(
            AuthenticatedSession session, String patientId, String status)
            throws PatientException {
        requireEditPermission(session);
        String normalizedId = normalize(patientId);
        String normalizedStatus = normalize(status);
        if (normalizedId.isEmpty() || normalizedId.length() > 20
                || !STATUSES.contains(normalizedStatus)) {
            throw new PatientException("Thông tin trạng thái bệnh nhân không hợp lệ.");
        }
        try {
            patientDAO.updateStatus(normalizedId, normalizedStatus);
        } catch (PatientDAOException exception) {
            throw toPatientException(exception);
        }
    }

    private BenhNhan validateAndNormalize(BenhNhan patient) throws PatientException {
        if (patient == null) {
            throw new PatientException("Thông tin bệnh nhân không hợp lệ.");
        }
        String name = normalize(patient.hoTen());
        String gender = normalize(patient.gioiTinh());
        String document = normalize(patient.soGiayTo());
        String phone = normalize(patient.soDienThoai());
        String email = normalize(patient.email());
        String address = normalize(patient.diaChi());

        requireMaxLength(name, 150, "Họ tên");
        if (name.isEmpty()) {
            throw new PatientException("Họ tên bệnh nhân không được để trống.");
        }
        if (patient.ngaySinh() != null && patient.ngaySinh().isAfter(LocalDate.now())) {
            throw new PatientException("Ngày sinh không được nằm trong tương lai.");
        }
        if (!gender.isEmpty() && !GENDERS.contains(gender)) {
            throw new PatientException("Giới tính chỉ được là Nam, Nữ hoặc Khác.");
        }
        requireMaxLength(document, 40, "Số giấy tờ");
        requireMaxLength(phone, 20, "Số điện thoại");
        requireMaxLength(email, 120, "Email");
        requireMaxLength(address, 255, "Địa chỉ");
        if (!phone.isEmpty()
                && (!PHONE_PATTERN.matcher(phone).matches()
                || phone.chars().filter(Character::isDigit).count() < 7)) {
            throw new PatientException("Số điện thoại không hợp lệ.");
        }
        if (!email.isEmpty() && !EMAIL_PATTERN.matcher(email).matches()) {
            throw new PatientException("Địa chỉ email không hợp lệ.");
        }

        return new BenhNhan(
                normalize(patient.maBenhNhan()),
                name,
                patient.ngaySinh(),
                gender.isEmpty() ? null : gender,
                document.isEmpty() ? null : document,
                phone.isEmpty() ? null : phone,
                email.isEmpty() ? null : email,
                address.isEmpty() ? null : address,
                patient.ngayTaoHoSo(),
                patient.trangThai());
    }

    private void requireViewPermission(AuthenticatedSession session)
            throws PatientException {
        try {
            authorizationBUS.requirePageAccess(session, "Bệnh nhân");
        } catch (SecurityException exception) {
            throw new PatientException(exception.getMessage(), exception);
        }
    }

    private void requireEditPermission(AuthenticatedSession session)
            throws PatientException {
        try {
            authorizationBUS.requirePageAccess(session, "Bệnh nhân");
            authorizationBUS.requirePermission(session, "BN_SUA");
        } catch (SecurityException exception) {
            throw new PatientException(exception.getMessage(), exception);
        }
    }

    private PatientException toPatientException(PatientDAOException exception) {
        return new PatientException(exception.getMessage(), exception);
    }

    private void requireMaxLength(String value, int maximum, String field)
            throws PatientException {
        if (value.length() > maximum) {
            throw new PatientException(field + " không được vượt quá " + maximum + " ký tự.");
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
