package bus.warehouse;

import bus.auth.AuthorizationBUS;
import dao.warehouse.MedicineWarehouseDAO;
import dao.warehouse.WarehouseDAOException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import model.auth.AuthenticatedSession;
import model.warehouse.WarehouseModels.BatchDetail;
import model.warehouse.WarehouseModels.BatchFilter;
import model.warehouse.WarehouseModels.CreatedReceipt;
import model.warehouse.WarehouseModels.ExportCandidate;
import model.warehouse.WarehouseModels.ExportFilter;
import model.warehouse.WarehouseModels.ExportPreview;
import model.warehouse.WarehouseModels.ImportDraft;
import model.warehouse.WarehouseModels.ImportLineDraft;
import model.warehouse.WarehouseModels.InventoryFilter;
import model.warehouse.WarehouseModels.Medicine;
import model.warehouse.WarehouseModels.MedicineDraft;
import model.warehouse.WarehouseModels.MovementFilter;
import model.warehouse.WarehouseModels.Page;
import model.warehouse.WarehouseModels.Receipt;
import model.warehouse.WarehouseModels.ReceiptDetail;
import model.warehouse.WarehouseModels.ReceiptFilter;
import model.warehouse.WarehouseModels.Stats;
import model.warehouse.WarehouseModels.StockMovement;

public final class WarehouseBUS {
    private static final int PAGE_SIZE_MAX = 100;
    private static final int SEARCH_LENGTH_MAX = 150;
    private static final int MAX_LINES = 100;
    private static final BigDecimal MAX_STOCK =
            new BigDecimal("999999999999.99");
    private static final BigDecimal MAX_RECEIPT =
            new BigDecimal("99999999999999.99");

    private final MedicineWarehouseDAO warehouseDAO;
    private final AuthorizationBUS authorizationBUS;

    public WarehouseBUS() {
        this(new MedicineWarehouseDAO(), new AuthorizationBUS());
    }

    WarehouseBUS(
            MedicineWarehouseDAO warehouseDAO, AuthorizationBUS authorizationBUS) {
        this.warehouseDAO = warehouseDAO;
        this.authorizationBUS = authorizationBUS;
    }

    public int getExpiryWarningDays(AuthenticatedSession session)
            throws WarehouseException {
        requireReadAccess(session);
        return query(warehouseDAO::getExpiryWarningDays);
    }

    public Stats getStats(AuthenticatedSession session)
            throws WarehouseException {
        requireReadAccess(session);
        int warningDays = query(warehouseDAO::getExpiryWarningDays);
        return query(() -> warehouseDAO.getStats(warningDays));
    }

    public Page<Medicine> searchMedicines(
            AuthenticatedSession session,
            String search,
            String status,
            int page,
            int pageSize) throws WarehouseException {
        requireReadAccess(session);
        validatePage(page, pageSize);
        validateSearch(search);
        validateChoice(status, Set.of("", "DangSuDung", "NgungSuDung"),
                "Trạng thái thuốc không hợp lệ.");
        return query(() -> warehouseDAO.findMedicines(
                normalize(search), normalize(status), page, pageSize));
    }

    public Medicine getMedicine(AuthenticatedSession session, String medicineId)
            throws WarehouseException {
        requireReadAccess(session);
        if (medicineId == null || medicineId.isBlank() || medicineId.length() > 20) {
            throw new WarehouseException("Mã thuốc không hợp lệ.");
        }
        return query(() -> warehouseDAO.findMedicine(medicineId.trim()));
    }

    public void saveMedicine(
            AuthenticatedSession session, MedicineDraft draft, boolean creating)
            throws WarehouseException {
        requirePermission(session, "KHO_NHAP");
        validateMedicine(draft);
        execute(() -> warehouseDAO.saveMedicine(
                normalize(draft), creating, session.getAccountId()));
    }

    public Page<model.warehouse.WarehouseModels.Batch> searchBatches(
            AuthenticatedSession session, BatchFilter filter)
            throws WarehouseException {
        requireReadAccess(session);
        validatePageFilter(filter == null ? -1 : filter.page(),
                filter == null ? 0 : filter.pageSize(),
                filter == null ? null : filter.searchText());
        int warningDays = query(warehouseDAO::getExpiryWarningDays);
        BatchFilter normalized = new BatchFilter(
                normalize(filter.searchText()),
                normalize(filter.medicineId()),
                normalize(filter.status()),
                filter.expiryFrom(),
                filter.expiryTo(),
                filter.page(),
                filter.pageSize());
        validateChoice(normalized.status(),
                Set.of("", "DangTon", "SapHetHan", "HetHang", "HetHan", "BiKhoa"),
                "Trạng thái lô không hợp lệ.");
        if (normalized.expiryFrom() != null && normalized.expiryTo() != null
                && normalized.expiryFrom().isAfter(normalized.expiryTo())) {
            throw new WarehouseException("Khoảng hạn sử dụng không hợp lệ.");
        }
        return query(() -> warehouseDAO.findBatches(normalized, warningDays));
    }

    public BatchDetail getBatchDetail(AuthenticatedSession session, long batchId)
            throws WarehouseException {
        requireReadAccess(session);
        requireId(batchId, "Mã lô");
        return query(() -> warehouseDAO.findBatchDetail(batchId));
    }

    public Page<model.warehouse.WarehouseModels.InventoryItem> searchInventory(
            AuthenticatedSession session, InventoryFilter filter)
            throws WarehouseException {
        requireReadAccess(session);
        validatePageFilter(filter == null ? -1 : filter.page(),
                filter == null ? 0 : filter.pageSize(),
                filter == null ? null : filter.searchText());
        validateChoice(filter.stockStatus(),
                Set.of("", "ConHang", "HetHang", "SapHetHan", "HetHan"),
                "Trạng thái tồn kho không hợp lệ.");
        int warningDays = query(warehouseDAO::getExpiryWarningDays);
        return query(() -> warehouseDAO.findInventory(
                new InventoryFilter(normalize(filter.searchText()),
                        normalize(filter.stockStatus()), filter.page(), filter.pageSize()),
                warningDays));
    }

    public Page<Receipt> searchReceipts(
            AuthenticatedSession session, boolean inbound, ReceiptFilter filter)
            throws WarehouseException {
        requireReadAccess(session);
        if (filter == null) {
            throw new WarehouseException("Bộ lọc phiếu kho không hợp lệ.");
        }
        validatePageFilter(filter.page(), filter.pageSize(), filter.searchText());
        if (filter.fromDate() != null && filter.toDate() != null
                && filter.fromDate().isAfter(filter.toDate())) {
            throw new WarehouseException("Khoảng ngày phiếu kho không hợp lệ.");
        }
        return query(() -> warehouseDAO.findReceipts(inbound, new ReceiptFilter(
                normalize(filter.searchText()),
                normalize(filter.status()),
                filter.fromDate(),
                filter.toDate(),
                filter.page(),
                filter.pageSize())));
    }

    public ReceiptDetail getReceiptDetail(
            AuthenticatedSession session, long receiptId, boolean inbound)
            throws WarehouseException {
        requireReadAccess(session);
        requireId(receiptId, "Mã phiếu");
        return query(() -> warehouseDAO.findReceiptDetail(receiptId, inbound));
    }

    public Page<StockMovement> searchMovements(
            AuthenticatedSession session, MovementFilter filter)
            throws WarehouseException {
        requireReadAccess(session);
        if (filter == null) {
            throw new WarehouseException("Bộ lọc lịch sử kho không hợp lệ.");
        }
        validatePageFilter(filter.page(), filter.pageSize(), filter.searchText());
        if (filter.fromDate() != null && filter.toDate() != null
                && filter.fromDate().isAfter(filter.toDate())) {
            throw new WarehouseException("Khoảng ngày lịch sử không hợp lệ.");
        }
        validateChoice(filter.movementType(), Set.of("", "Nhap", "Xuat"),
                "Loại giao dịch kho không hợp lệ.");
        return query(() -> warehouseDAO.findMovements(new MovementFilter(
                normalize(filter.searchText()),
                normalize(filter.movementType()),
                normalize(filter.status()),
                filter.fromDate(),
                filter.toDate(),
                filter.page(),
                filter.pageSize())));
    }

    public Page<ExportCandidate> searchExportCandidates(
            AuthenticatedSession session, ExportFilter filter)
            throws WarehouseException {
        requirePermission(session, "KHO_XUAT");
        if (filter == null) {
            throw new WarehouseException("Bộ lọc đơn thuốc không hợp lệ.");
        }
        validatePageFilter(filter.page(), filter.pageSize(), filter.searchText());
        return query(() -> warehouseDAO.findExportCandidates(new ExportFilter(
                normalize(filter.searchText()), filter.page(), filter.pageSize())));
    }

    public ExportPreview getExportPreview(
            AuthenticatedSession session, long prescriptionId)
            throws WarehouseException {
        requirePermission(session, "KHO_XUAT");
        requireId(prescriptionId, "Mã đơn thuốc");
        ExportPreview preview = query(
                () -> warehouseDAO.findExportPreview(prescriptionId));
        if (preview == null) {
            throw new WarehouseException(
                    "Đơn thuốc không còn đủ điều kiện cấp phát hoặc đã được xuất.");
        }
        return preview;
    }

    public CreatedReceipt createImport(
            AuthenticatedSession session, ImportDraft draft)
            throws WarehouseException {
        requirePermission(session, "KHO_NHAP");
        validateImport(draft);
        return query(() -> warehouseDAO.createImport(
                normalize(draft), session.getEmployeeId(), session.getAccountId()));
    }

    public CreatedReceipt exportPrescription(
            AuthenticatedSession session, long prescriptionId)
            throws WarehouseException {
        requirePermission(session, "KHO_XUAT");
        requireId(prescriptionId, "Mã đơn thuốc");
        return query(() -> warehouseDAO.exportPrescription(
                prescriptionId, session.getEmployeeId(), session.getAccountId()));
    }

    private void validateMedicine(MedicineDraft draft) throws WarehouseException {
        if (draft == null || draft.id() == null || draft.id().isBlank()
                || draft.id().length() > 20) {
            throw new WarehouseException("Mã thuốc bắt buộc và tối đa 20 ký tự.");
        }
        requireText("Tên thuốc", draft.name(), 180, true);
        requireText("Hoạt chất", draft.activeIngredient(), 180, false);
        requireText("Hàm lượng", draft.strength(), 80, false);
        requireText("Dạng bào chế", draft.dosageForm(), 80, false);
        requireText("Đơn vị tính", draft.unit(), 30, false);
        requireText("Đường dùng mặc định", draft.defaultRoute(), 80, false);
        requireText("Hãng sản xuất", draft.manufacturer(), 150, false);
        requireText("Nước sản xuất", draft.country(), 100, false);
        requireText("Mô tả", draft.description(), 255, false);
        validateChoice(draft.status(), Set.of("DangSuDung", "NgungSuDung"),
                "Trạng thái thuốc không hợp lệ.");
    }

    private void validateImport(ImportDraft draft) throws WarehouseException {
        if (draft == null || draft.lines().isEmpty() || draft.lines().size() > MAX_LINES) {
            throw new WarehouseException(
                    "Phiếu nhập phải có từ 1 đến " + MAX_LINES + " dòng thuốc.");
        }
        requireText("Nhà cung cấp", draft.supplier(), 180, false);
        requireText("Ghi chú", draft.note(), 255, false);
        Set<String> batchKeys = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (ImportLineDraft line : draft.lines()) {
            if (line == null || line.medicineId() == null
                    || line.medicineId().isBlank() || line.medicineId().length() > 20) {
                throw new WarehouseException("Dòng nhập có mã thuốc không hợp lệ.");
            }
            requireText("Số lô", line.batchNumber(), 80, true);
            requireText("Vị trí lưu trữ", line.location(), 100, false);
            if (line.expiryDate() == null
                    || line.expiryDate().isBefore(java.time.LocalDate.now())
                    || (line.manufactureDate() != null
                    && line.manufactureDate().isAfter(line.expiryDate()))) {
                throw new WarehouseException(
                        "Ngày sản xuất và hạn sử dụng của lô không hợp lệ.");
            }
            validateDecimal("Số lượng nhập", line.quantity(), MAX_STOCK, true);
            validateDecimal("Đơn giá nhập", line.unitCost(), MAX_STOCK, false);
            String key = line.medicineId().trim().toUpperCase(Locale.ROOT)
                    + "\u0000" + line.batchNumber().trim().toUpperCase(Locale.ROOT);
            if (!batchKeys.add(key)) {
                throw new WarehouseException(
                        "Một lô thuốc không được lặp lại trong cùng phiếu nhập.");
            }
            total = total.add(line.quantity().multiply(line.unitCost()));
        }
        if (total.compareTo(MAX_RECEIPT) > 0) {
            throw new WarehouseException("Tổng tiền phiếu nhập vượt giới hạn lưu trữ.");
        }
    }

    private void validateDecimal(
            String name, BigDecimal value, BigDecimal max, boolean positive)
            throws WarehouseException {
        if (value == null || (positive ? value.signum() <= 0 : value.signum() < 0)
                || value.compareTo(max) > 0
                || value.stripTrailingZeros().scale() > 2) {
            throw new WarehouseException(name + " không hợp lệ (tối đa 2 chữ số thập phân).");
        }
    }

    private void validatePageFilter(int page, int pageSize, String search)
            throws WarehouseException {
        validatePage(page, pageSize);
        validateSearch(search);
    }

    private void validatePage(int page, int pageSize) throws WarehouseException {
        if (page < 0 || pageSize < 1 || pageSize > PAGE_SIZE_MAX) {
            throw new WarehouseException("Thông tin phân trang không hợp lệ.");
        }
    }

    private void validateSearch(String value) throws WarehouseException {
        if (value != null && value.length() > SEARCH_LENGTH_MAX) {
            throw new WarehouseException("Từ khóa tìm kiếm quá dài.");
        }
    }

    private void validateChoice(String value, Set<String> choices, String message)
            throws WarehouseException {
        if (value != null && !choices.contains(value)) {
            throw new WarehouseException(message);
        }
    }

    private void requireText(String name, String value, int maxLength, boolean required)
            throws WarehouseException {
        if (required && (value == null || value.isBlank())) {
            throw new WarehouseException(name + " là bắt buộc.");
        }
        if (value != null && value.length() > maxLength) {
            throw new WarehouseException(
                    name + " vượt quá giới hạn " + maxLength + " ký tự.");
        }
    }

    private void requireId(long id, String label) throws WarehouseException {
        if (id <= 0) {
            throw new WarehouseException(label + " không hợp lệ.");
        }
    }

    private void requireReadAccess(AuthenticatedSession session)
            throws WarehouseException {
        requireAccess(session, "KHO_XEM");
    }

    private void requirePermission(AuthenticatedSession session, String permission)
            throws WarehouseException {
        requireAccess(session, permission);
    }

    private void requireAccess(AuthenticatedSession session, String permission)
            throws WarehouseException {
        if (session == null || !session.isActive()) {
            throw new WarehouseException(
                    "Bạn chưa đăng nhập hoặc phiên làm việc đã hết hạn.");
        }
        try {
            authorizationBUS.requirePageAccess(session, "Kho thuốc");
            authorizationBUS.requirePermission(session, permission);
        } catch (SecurityException exception) {
            throw new WarehouseException(exception.getMessage(), exception);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private MedicineDraft normalize(MedicineDraft draft) {
        return new MedicineDraft(
                draft.id().trim(), draft.name().trim(),
                normalizeOptional(draft.activeIngredient()), normalizeOptional(draft.strength()),
                normalizeOptional(draft.dosageForm()), normalizeOptional(draft.unit()),
                normalizeOptional(draft.defaultRoute()), normalizeOptional(draft.manufacturer()),
                normalizeOptional(draft.country()), normalizeOptional(draft.description()),
                draft.status());
    }

    private ImportDraft normalize(ImportDraft draft) {
        return new ImportDraft(
                normalizeOptional(draft.supplier()),
                normalizeOptional(draft.note()),
                draft.lines().stream().map(line -> new ImportLineDraft(
                        line.medicineId().trim(), line.batchNumber().trim(),
                        line.manufactureDate(), line.expiryDate(), line.quantity(),
                        line.unitCost(), normalizeOptional(line.location()))).toList());
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private <T> T query(DAOCall<T> call) throws WarehouseException {
        try {
            return call.run();
        } catch (WarehouseDAOException exception) {
            throw new WarehouseException(exception.getMessage(), exception);
        }
    }

    private void execute(DAOAction action) throws WarehouseException {
        try {
            action.run();
        } catch (WarehouseDAOException exception) {
            throw new WarehouseException(exception.getMessage(), exception);
        }
    }

    @FunctionalInterface
    private interface DAOCall<T> {
        T run();
    }

    @FunctionalInterface
    private interface DAOAction {
        void run();
    }
}
