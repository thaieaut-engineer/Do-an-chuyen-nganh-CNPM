package bus.serviceorder;

import bus.auth.AuthorizationBUS;
import dao.serviceorder.ServiceOrderDAO;
import dao.serviceorder.ServiceOrderDAOException;
import java.util.List;
import model.auth.AuthenticatedSession;
import model.serviceorder.EligibleOrderVisit;
import model.serviceorder.ServiceCatalogItem;
import model.serviceorder.ServiceOrderDetail;
import model.serviceorder.ServiceOrderDraft;
import model.serviceorder.ServiceOrderFilter;
import model.serviceorder.ServiceOrderListItem;
import model.serviceorder.ServiceOrderPage;
import model.serviceorder.ServiceOrderStats;

public final class ServiceOrderBUS {
    private static final int PAGE_SIZE_MAX = 100;
    private static final int NOTE_LENGTH_MAX = 255;
    private static final int SEARCH_LENGTH_MAX = 150;

    private final ServiceOrderDAO serviceOrderDAO;
    private final AuthorizationBUS authorizationBUS;

    public ServiceOrderBUS() {
        this(new ServiceOrderDAO(), new AuthorizationBUS());
    }

    ServiceOrderBUS(ServiceOrderDAO serviceOrderDAO, AuthorizationBUS authorizationBUS) {
        this.serviceOrderDAO = serviceOrderDAO;
        this.authorizationBUS = authorizationBUS;
    }

    public ServiceOrderStats getStats(AuthenticatedSession session)
            throws ServiceOrderException {
        requireAccess(session);
        try {
            return serviceOrderDAO.getStats(
                    session.getEmployeeId(), canManageAnyDoctor(session));
        } catch (ServiceOrderDAOException exception) {
            throw databaseError(exception);
        }
    }

    public ServiceOrderPage search(
            AuthenticatedSession session, ServiceOrderFilter filter)
            throws ServiceOrderException {
        requireAccess(session);
        if (filter == null || filter.page() < 0 || filter.pageSize() < 1
                || filter.pageSize() > PAGE_SIZE_MAX
                || (filter.fromDate() != null && filter.toDate() != null
                && filter.fromDate().isAfter(filter.toDate()))
                || (filter.searchText() != null
                && filter.searchText().length() > SEARCH_LENGTH_MAX)) {
            throw new ServiceOrderException("Điều kiện lọc chỉ định dịch vụ không hợp lệ.");
        }
        try {
            return serviceOrderDAO.findPage(
                    normalize(filter),
                    session.getEmployeeId(),
                    canManageAnyDoctor(session));
        } catch (ServiceOrderDAOException exception) {
            throw databaseError(exception);
        }
    }

    public List<ServiceCatalogItem> getServices(
            AuthenticatedSession session, String searchText, String serviceType)
            throws ServiceOrderException {
        requireAccess(session);
        if (searchText != null && searchText.length() > SEARCH_LENGTH_MAX) {
            throw new ServiceOrderException("Từ khóa tìm dịch vụ quá dài.");
        }
        try {
            return serviceOrderDAO.findServices(
                    normalize(searchText), normalize(serviceType));
        } catch (ServiceOrderDAOException exception) {
            throw databaseError(exception);
        }
    }

    public List<String> getServiceTypes(AuthenticatedSession session)
            throws ServiceOrderException {
        requireAccess(session);
        try {
            return serviceOrderDAO.findServiceTypes();
        } catch (ServiceOrderDAOException exception) {
            throw databaseError(exception);
        }
    }

    public List<String> getOrderStatuses(AuthenticatedSession session)
            throws ServiceOrderException {
        requireAccess(session);
        return serviceOrderDAO.findOrderStatuses();
    }

    public List<EligibleOrderVisit> getEligibleVisits(AuthenticatedSession session)
            throws ServiceOrderException {
        requireAccess(session);
        try {
            return serviceOrderDAO.findEligibleVisits(
                    session.getEmployeeId(), canManageAnyDoctor(session));
        } catch (ServiceOrderDAOException exception) {
            throw databaseError(exception);
        }
    }

    public ServiceOrderDetail getById(AuthenticatedSession session, long orderId)
            throws ServiceOrderException {
        requireAccess(session);
        if (orderId <= 0) {
            throw new ServiceOrderException("Mã chỉ định không hợp lệ.");
        }
        try {
            ServiceOrderDetail order = serviceOrderDAO.findById(
                    orderId, session.getEmployeeId(), canManageAnyDoctor(session));
            if (order == null) {
                throw new ServiceOrderException(
                        "Không tìm thấy chỉ định hoặc bạn không có quyền xem.");
            }
            return order;
        } catch (ServiceOrderDAOException exception) {
            throw databaseError(exception);
        }
    }

    public List<ServiceOrderListItem> getByVisit(
            AuthenticatedSession session, long visitId) throws ServiceOrderException {
        requireAccess(session);
        if (visitId <= 0) {
            throw new ServiceOrderException("Mã lượt khám không hợp lệ.");
        }
        try {
            return serviceOrderDAO.findByVisit(
                    visitId, session.getEmployeeId(), canManageAnyDoctor(session));
        } catch (ServiceOrderDAOException exception) {
            throw databaseError(exception);
        }
    }

    public long create(AuthenticatedSession session, ServiceOrderDraft draft)
            throws ServiceOrderException {
        requireAccess(session);
        validateDraft(draft);
        try {
            return serviceOrderDAO.create(
                    draft,
                    session.getEmployeeId(),
                    session.getAccountId(),
                    canManageAnyDoctor(session));
        } catch (ServiceOrderDAOException exception) {
            throw databaseError(exception);
        }
    }

    public void update(
            AuthenticatedSession session, long orderId, ServiceOrderDraft draft)
            throws ServiceOrderException {
        requireAccess(session);
        if (orderId <= 0) {
            throw new ServiceOrderException("Mã chỉ định không hợp lệ.");
        }
        validateDraft(draft);
        try {
            serviceOrderDAO.update(
                    orderId,
                    draft,
                    session.getEmployeeId(),
                    session.getAccountId(),
                    canManageAnyDoctor(session));
        } catch (ServiceOrderDAOException exception) {
            throw databaseError(exception);
        }
    }

    public void cancel(
            AuthenticatedSession session, long orderId, String reason)
            throws ServiceOrderException {
        requireAccess(session);
        if (orderId <= 0) {
            throw new ServiceOrderException("Mã chỉ định không hợp lệ.");
        }
        String normalizedReason = normalize(reason);
        if (normalizedReason.isEmpty() || normalizedReason.length() > NOTE_LENGTH_MAX) {
            throw new ServiceOrderException(
                    "Lý do hủy là bắt buộc và tối đa 255 ký tự.");
        }
        try {
            serviceOrderDAO.cancel(
                    orderId,
                    normalizedReason,
                    session.getEmployeeId(),
                    session.getAccountId(),
                    canManageAnyDoctor(session));
        } catch (ServiceOrderDAOException exception) {
            throw databaseError(exception);
        }
    }

    public boolean canEdit(AuthenticatedSession session, ServiceOrderListItem order) {
        return canWrite(session) && order != null
                && "ChoThucHien".equals(order.trangThai())
                && ("DangKham".equals(order.trangThaiLuotKham())
                || "ChoKetQua".equals(order.trangThaiLuotKham()))
                && !order.daTinhPhi()
                && (canManageAnyDoctor(session)
                || session.getEmployeeId().equals(order.maBacSi()));
    }

    public boolean canCancel(AuthenticatedSession session, ServiceOrderListItem order) {
        return canEdit(session, order);
    }

    private void validateDraft(ServiceOrderDraft draft) throws ServiceOrderException {
        if (draft == null || draft.maLuotKham() <= 0) {
            throw new ServiceOrderException("Hãy chọn lượt khám hợp lệ.");
        }
        if (draft.maDichVu() == null || draft.maDichVu().isBlank()
                || draft.maDichVu().length() > 20) {
            throw new ServiceOrderException("Hãy chọn dịch vụ hợp lệ.");
        }
        if (draft.ghiChu() != null && draft.ghiChu().length() > NOTE_LENGTH_MAX) {
            throw new ServiceOrderException("Ghi chú tối đa 255 ký tự.");
        }
    }

    private void requireAccess(AuthenticatedSession session) throws ServiceOrderException {
        if (session == null || !session.isActive()) {
            throw new ServiceOrderException(
                    "Bạn chưa đăng nhập hoặc phiên làm việc đã hết hạn.");
        }
        try {
            authorizationBUS.requirePageAccess(session, "Chỉ định dịch vụ");
            authorizationBUS.requirePermission(session, "KHAM_CHIDINH");
        } catch (SecurityException exception) {
            throw new ServiceOrderException(exception.getMessage(), exception);
        }
    }

    private boolean canWrite(AuthenticatedSession session) {
        return session != null && session.isActive()
                && session.hasPermission("KHAM_CHIDINH");
    }

    private boolean canManageAnyDoctor(AuthenticatedSession session) {
        return session.hasPermission("NHANSU_SUA");
    }

    private ServiceOrderFilter normalize(ServiceOrderFilter filter) {
        return new ServiceOrderFilter(
                normalize(filter.searchText()),
                filter.fromDate(),
                filter.toDate(),
                normalize(filter.serviceType()),
                normalize(filter.status()),
                filter.page(),
                filter.pageSize());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private ServiceOrderException databaseError(ServiceOrderDAOException cause) {
        return new ServiceOrderException(cause.getMessage(), cause);
    }
}
