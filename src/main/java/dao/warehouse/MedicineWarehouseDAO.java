package dao.warehouse;

import config.DatabaseConnection;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import model.warehouse.WarehouseModels.Batch;
import model.warehouse.WarehouseModels.BatchAllocationPreview;
import model.warehouse.WarehouseModels.BatchDetail;
import model.warehouse.WarehouseModels.BatchFilter;
import model.warehouse.WarehouseModels.CreatedReceipt;
import model.warehouse.WarehouseModels.ExportCandidate;
import model.warehouse.WarehouseModels.ExportFilter;
import model.warehouse.WarehouseModels.ExportLinePreview;
import model.warehouse.WarehouseModels.ExportPreview;
import model.warehouse.WarehouseModels.ImportDraft;
import model.warehouse.WarehouseModels.ImportLineDraft;
import model.warehouse.WarehouseModels.InventoryFilter;
import model.warehouse.WarehouseModels.InventoryItem;
import model.warehouse.WarehouseModels.Medicine;
import model.warehouse.WarehouseModels.MedicineDraft;
import model.warehouse.WarehouseModels.MovementFilter;
import model.warehouse.WarehouseModels.Page;
import model.warehouse.WarehouseModels.Receipt;
import model.warehouse.WarehouseModels.ReceiptDetail;
import model.warehouse.WarehouseModels.ReceiptFilter;
import model.warehouse.WarehouseModels.ReceiptLine;
import model.warehouse.WarehouseModels.Stats;
import model.warehouse.WarehouseModels.StockMovement;

public final class MedicineWarehouseDAO {
    private static final String MEDICINE_COLUMNS = """
            MaThuoc, TenThuoc, HoatChat, HamLuong, DangBaoChe, DonViTinh,
            DuongDungMacDinh, HangSanXuat, NuocSanXuat, MoTa, TrangThai
            """;
    private static final String BATCH_SELECT = """
            SELECT l.MaLoThuoc, l.MaThuoc, t.TenThuoc, l.SoLo,
                   l.NgaySanXuat, l.HanSuDung, l.DonGiaNhap, l.SoLuongTon,
                   l.ViTriLuuTru,
                   CASE
                       WHEN l.TrangThai = N'BiKhoa' THEN N'BiKhoa'
                       WHEN l.HanSuDung < CAST(SYSDATETIME() AS date) THEN N'HetHan'
                       WHEN l.SoLuongTon <= 0 THEN N'HetHang'
                       ELSE l.TrangThai
                   END AS TrangThaiHienTai,
                   COALESCE(n.SoLuongNhap, 0) AS SoLuongNhap,
                   COALESCE(x.SoLuongXuat, 0) AS SoLuongXuat
            FROM dbo.LoThuoc l
            JOIN dbo.Thuoc t ON t.MaThuoc = l.MaThuoc
            LEFT JOIN (
                SELECT c.MaLoThuoc, SUM(c.SoLuong) AS SoLuongNhap
                FROM dbo.ChiTietPhieuNhap c
                JOIN dbo.PhieuNhap p ON p.MaPhieuNhap = c.MaPhieuNhap
                WHERE p.TrangThai = N'DaXacNhan'
                GROUP BY c.MaLoThuoc
            ) n ON n.MaLoThuoc = l.MaLoThuoc
            LEFT JOIN (
                SELECT c.MaLoThuoc, SUM(c.SoLuong) AS SoLuongXuat
                FROM dbo.ChiTietPhieuXuat c
                JOIN dbo.PhieuXuat p ON p.MaPhieuXuat = c.MaPhieuXuat
                WHERE p.TrangThai = N'DaXacNhan'
                GROUP BY c.MaLoThuoc
            ) x ON x.MaLoThuoc = l.MaLoThuoc
            """;
    private static final String MOVEMENT_SOURCE = """
            SELECT p.NgayNhap AS NgayGiaoDich, N'Nhap' AS LoaiGiaoDich,
                   p.SoPhieu, p.TrangThai, l.MaLoThuoc, l.SoLo, t.MaThuoc,
                   t.TenThuoc, c.SoLuong, c.DonGia AS DonGia,
                   nv.HoTen AS NhanVien, p.GhiChu
            FROM dbo.ChiTietPhieuNhap c
            JOIN dbo.PhieuNhap p ON p.MaPhieuNhap = c.MaPhieuNhap
            JOIN dbo.LoThuoc l ON l.MaLoThuoc = c.MaLoThuoc
            JOIN dbo.Thuoc t ON t.MaThuoc = l.MaThuoc
            JOIN dbo.NhanVien nv ON nv.MaNhanVien = p.MaNhanVienNhap
            UNION ALL
            SELECT p.NgayXuat, N'Xuat', p.SoPhieu, p.TrangThai,
                   l.MaLoThuoc, l.SoLo, t.MaThuoc, t.TenThuoc,
                   -c.SoLuong, c.DonGia, nv.HoTen, p.GhiChu
            FROM dbo.ChiTietPhieuXuat c
            JOIN dbo.PhieuXuat p ON p.MaPhieuXuat = c.MaPhieuXuat
            JOIN dbo.LoThuoc l ON l.MaLoThuoc = c.MaLoThuoc
            JOIN dbo.Thuoc t ON t.MaThuoc = l.MaThuoc
            JOIN dbo.NhanVien nv ON nv.MaNhanVien = p.MaNhanVienXuat
            """;
    private static final BigDecimal MAX_STOCK_VALUE =
            new BigDecimal("999999999999.99");
    private static final BigDecimal MAX_UNIT_COST =
            new BigDecimal("999999999999.99");
    private static final BigDecimal MAX_RECEIPT_TOTAL =
            new BigDecimal("99999999999999.99");
    private static final DateTimeFormatter RECEIPT_STAMP =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public int getExpiryWarningDays() {
        String sql = """
                SELECT TRY_CONVERT(int, GiaTri)
                FROM dbo.CauHinhHeThong
                WHERE MaCauHinh = 'KHO_CANHBAO_HAN_NGAY'
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            if (!result.next()) {
                throw new WarehouseDAOException(
                        "Thiếu cấu hình KHO_CANHBAO_HAN_NGAY trong CauHinhHeThong.");
            }
            int days = result.getInt(1);
            if (result.wasNull() || days < 0) {
                throw new WarehouseDAOException(
                        "Cấu hình KHO_CANHBAO_HAN_NGAY không phải số ngày hợp lệ.");
            }
            return days;
        } catch (SQLException exception) {
            throw databaseError("getExpiryWarningDays", exception);
        }
    }

    public Stats getStats(int expiryWarningDays) {
        String sql = """
                SELECT
                    (SELECT COUNT_BIG(*) FROM dbo.Thuoc) AS MedicineCount,
                    (SELECT COUNT_BIG(*) FROM dbo.LoThuoc) AS BatchCount,
                    (SELECT COALESCE(SUM(SoLuongTon), 0) FROM dbo.LoThuoc) AS TotalStock,
                    (SELECT COUNT_BIG(*) FROM dbo.LoThuoc
                     WHERE SoLuongTon > 0
                       AND HanSuDung >= CAST(SYSDATETIME() AS date)
                       AND HanSuDung < DATEADD(day, ?, CAST(SYSDATETIME() AS date)))
                        AS ExpiringLots,
                    (SELECT COUNT_BIG(*) FROM dbo.LoThuoc
                     WHERE SoLuongTon > 0
                       AND HanSuDung < CAST(SYSDATETIME() AS date)) AS ExpiredLots,
                    (SELECT COUNT_BIG(*) FROM dbo.LoThuoc
                     WHERE SoLuongTon <= 0) AS OutOfStockLots,
                    (SELECT COUNT_BIG(*) FROM dbo.PhieuNhap p
                     WHERE p.TrangThai = N'DaXacNhan'
                       AND EXISTS (SELECT 1 FROM dbo.ChiTietPhieuNhap c
                                   WHERE c.MaPhieuNhap = p.MaPhieuNhap))
                        AS ImportedReceipts,
                    (SELECT COUNT_BIG(*) FROM dbo.PhieuXuat p
                     WHERE p.TrangThai = N'DaXacNhan'
                       AND EXISTS (SELECT 1 FROM dbo.ChiTietPhieuXuat c
                                   WHERE c.MaPhieuXuat = p.MaPhieuXuat))
                        AS ExportedReceipts
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, expiryWarningDays);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("SQL Server không trả về thống kê kho thuốc.");
                }
                return new Stats(
                        result.getLong("MedicineCount"),
                        result.getLong("BatchCount"),
                        result.getBigDecimal("TotalStock"),
                        result.getLong("ExpiringLots"),
                        result.getLong("ExpiredLots"),
                        result.getLong("OutOfStockLots"),
                        result.getLong("ImportedReceipts"),
                        result.getLong("ExportedReceipts"));
            }
        } catch (SQLException exception) {
            throw databaseError("getStats", exception);
        }
    }

    public Page<Medicine> findMedicines(
            String search, String status, int page, int pageSize) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> parameters = new ArrayList<>();
        appendMedicineSearch(where, parameters, search);
        if (status != null && !status.isBlank()) {
            where.append(" AND m.TrangThai = ?");
            parameters.add(status);
        }
        String from = " FROM dbo.Thuoc m" + where;
        long total = count("SELECT COUNT_BIG(*)" + from, parameters, "findMedicines");
        String sql = "SELECT m." + MEDICINE_COLUMNS.replace(", ", ", m.")
                + from + " ORDER BY m.TenThuoc, m.MaThuoc OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        List<Object> queryParameters = new ArrayList<>(parameters);
        queryParameters.add((long) page * pageSize);
        queryParameters.add(pageSize);
        List<Medicine> medicines = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, queryParameters);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    medicines.add(mapMedicine(result));
                }
            }
            return new Page<>(medicines, total, page, pageSize);
        } catch (SQLException exception) {
            throw databaseError("findMedicines", exception);
        }
    }

    public Medicine findMedicine(String medicineId) {
        String sql = "SELECT " + MEDICINE_COLUMNS
                + " FROM dbo.Thuoc WHERE MaThuoc = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, medicineId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? mapMedicine(result) : null;
            }
        } catch (SQLException exception) {
            throw databaseError("findMedicine", exception);
        }
    }

    public void saveMedicine(
            MedicineDraft draft, boolean creating, long accountId) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (creating) {
                    String sql = """
                            INSERT INTO dbo.Thuoc
                                (MaThuoc, TenThuoc, HoatChat, HamLuong, DangBaoChe,
                                 DonViTinh, DuongDungMacDinh, HangSanXuat,
                                 NuocSanXuat, MoTa, TrangThai)
                            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """;
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        bindMedicine(statement, draft);
                        statement.executeUpdate();
                    }
                } else {
                    String sql = """
                            UPDATE dbo.Thuoc
                            SET TenThuoc = ?, HoatChat = ?, HamLuong = ?,
                                DangBaoChe = ?, DonViTinh = ?, DuongDungMacDinh = ?,
                                HangSanXuat = ?, NuocSanXuat = ?, MoTa = ?, TrangThai = ?
                            WHERE MaThuoc = ?
                            """;
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        bindMedicineUpdate(statement, draft);
                        if (statement.executeUpdate() != 1) {
                            throw new WarehouseDAOException(
                                    "Không tìm thấy thuốc cần cập nhật.");
                        }
                    }
                }
                recordAudit(connection, accountId,
                        creating ? "TAO_THUOC" : "CAP_NHAT_THUOC",
                        "Thuoc", draft.id());
                connection.commit();
            } catch (WarehouseDAOException exception) {
                rollback(connection, exception);
                throw exception;
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw databaseError(creating ? "createMedicine" : "updateMedicine",
                        exception);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw databaseError(creating ? "createMedicine" : "updateMedicine",
                    exception);
        }
    }

    public Page<Batch> findBatches(BatchFilter filter, int expiryWarningDays) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> parameters = new ArrayList<>();
        appendBatchFilters(where, parameters, filter, expiryWarningDays);
        long total = count("SELECT COUNT_BIG(*) " + BATCH_SELECT + where,
                parameters, "findBatches");
        String sql = BATCH_SELECT + where
                + " ORDER BY l.HanSuDung, l.MaLoThuoc OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        List<Object> queryParameters = withPaging(parameters, filter.page(), filter.pageSize());
        List<Batch> batches = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, queryParameters);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    batches.add(mapBatch(result));
                }
            }
            return new Page<>(batches, total, filter.page(), filter.pageSize());
        } catch (SQLException exception) {
            throw databaseError("findBatches", exception);
        }
    }

    public BatchDetail findBatchDetail(long batchId) {
        Batch batch = findBatchById(batchId);
        if (batch == null) {
            return null;
        }
        BigDecimal expected = batch.confirmedIn().subtract(batch.confirmedOut());
        List<StockMovement> movements = findMovementsForBatch(batchId);
        return new BatchDetail(
                batch,
                batch.confirmedIn(),
                batch.confirmedOut(),
                expected,
                expected.compareTo(batch.stock()) == 0,
                movements);
    }

    public Page<InventoryItem> findInventory(
            InventoryFilter filter, int expiryWarningDays) {
        String stockCondition = inventoryStockCondition(filter.stockStatus());
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> parameters = new ArrayList<>();
        appendInventorySearch(where, parameters, filter.searchText());
        if (!stockCondition.isBlank()) {
            where.append(" AND ").append(stockCondition);
        }
        String cte = inventoryCte(expiryWarningDays);
        String from = """
                FROM dbo.Thuoc t
                LEFT JOIN InventoryByMedicine i ON i.MaThuoc = t.MaThuoc
                """ + where;
        long total = count(cte + " SELECT COUNT_BIG(*) " + from,
                parameters, "findInventory");
        String sql = cte + """
                SELECT t.MaThuoc, t.TenThuoc, t.HoatChat, t.HamLuong, t.DonViTinh,
                       t.TrangThai,
                       COALESCE(i.TongTon, 0) AS TongTon,
                       COALESCE(i.SoLo, 0) AS SoLo,
                       COALESCE(i.LoSapHetHan, 0) AS LoSapHetHan,
                       COALESCE(i.LoHetHan, 0) AS LoHetHan,
                       COALESCE(i.LoHetHang, 0) AS LoHetHang
                """ + from + """
                 ORDER BY t.TenThuoc, t.MaThuoc
                 OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """;
        List<Object> queryParameters = withPaging(parameters, filter.page(), filter.pageSize());
        List<InventoryItem> inventory = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, queryParameters);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    inventory.add(new InventoryItem(
                            result.getString("MaThuoc"),
                            result.getString("TenThuoc"),
                            result.getString("HoatChat"),
                            result.getString("HamLuong"),
                            result.getString("DonViTinh"),
                            result.getString("TrangThai"),
                            result.getBigDecimal("TongTon"),
                            result.getLong("SoLo"),
                            result.getLong("LoSapHetHan"),
                            result.getLong("LoHetHan"),
                            result.getLong("LoHetHang")));
                }
            }
            return new Page<>(inventory, total, filter.page(), filter.pageSize());
        } catch (SQLException exception) {
            throw databaseError("findInventory", exception);
        }
    }

    public Page<Receipt> findReceipts(boolean inbound, ReceiptFilter filter) {
        String source = inbound ? importReceiptSource() : exportReceiptSource();
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> parameters = new ArrayList<>();
        appendReceiptFilters(where, parameters, filter);
        String from = " FROM (" + source + ") r" + where;
        long total = count("SELECT COUNT_BIG(*)" + from, parameters,
                inbound ? "findImportReceipts" : "findExportReceipts");
        String sql = "SELECT r.*" + from
                + " ORDER BY r.NgayGiaoDich DESC, r.MaPhieu DESC"
                + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        List<Object> queryParameters = withPaging(parameters, filter.page(), filter.pageSize());
        List<Receipt> receipts = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, queryParameters);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    receipts.add(mapReceipt(result, inbound));
                }
            }
            return new Page<>(receipts, total, filter.page(), filter.pageSize());
        } catch (SQLException exception) {
            throw databaseError(inbound ? "findImportReceipts" : "findExportReceipts",
                    exception);
        }
    }

    public ReceiptDetail findReceiptDetail(long receiptId, boolean inbound) {
        String source = inbound ? importReceiptSource() : exportReceiptSource();
        Receipt receipt;
        String headerSql = "SELECT r.* FROM (" + source
                + ") r WHERE r.MaPhieu = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(headerSql)) {
            statement.setLong(1, receiptId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                receipt = mapReceipt(result, inbound);
            }
        } catch (SQLException exception) {
            throw databaseError(inbound ? "findImportReceiptDetail"
                    : "findExportReceiptDetail", exception);
        }

        String sql = inbound ? """
                SELECT t.MaThuoc, t.TenThuoc, l.MaLoThuoc, l.SoLo,
                       l.NgaySanXuat, l.HanSuDung, c.SoLuong, c.DonGia,
                       c.ThanhTien AS ThanhTien
                FROM dbo.ChiTietPhieuNhap c
                JOIN dbo.LoThuoc l ON l.MaLoThuoc = c.MaLoThuoc
                JOIN dbo.Thuoc t ON t.MaThuoc = l.MaThuoc
                WHERE c.MaPhieuNhap = ?
                ORDER BY c.MaChiTietNhap
                """ : """
                SELECT t.MaThuoc, t.TenThuoc, l.MaLoThuoc, l.SoLo,
                       l.NgaySanXuat, l.HanSuDung, c.SoLuong, c.DonGia,
                       c.SoLuong * c.DonGia AS ThanhTien
                FROM dbo.ChiTietPhieuXuat c
                JOIN dbo.LoThuoc l ON l.MaLoThuoc = c.MaLoThuoc
                JOIN dbo.Thuoc t ON t.MaThuoc = l.MaThuoc
                WHERE c.MaPhieuXuat = ?
                ORDER BY c.MaChiTietXuat
                """;
        List<ReceiptLine> lines = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, receiptId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    lines.add(new ReceiptLine(
                            result.getString("MaThuoc"),
                            result.getString("TenThuoc"),
                            result.getLong("MaLoThuoc"),
                            result.getString("SoLo"),
                            localDate(result, "NgaySanXuat"),
                            localDate(result, "HanSuDung"),
                            result.getBigDecimal("SoLuong"),
                            result.getBigDecimal("DonGia"),
                            result.getBigDecimal("ThanhTien")));
                }
            }
            return new ReceiptDetail(receipt, lines);
        } catch (SQLException exception) {
            throw databaseError(inbound ? "findImportReceiptDetail"
                    : "findExportReceiptDetail", exception);
        }
    }

    public Page<StockMovement> findMovements(MovementFilter filter) {
        String cte = "WITH MovementData AS (" + MOVEMENT_SOURCE + ")";
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> parameters = new ArrayList<>();
        appendMovementFilters(where, parameters, filter);
        String from = " FROM MovementData m" + where;
        long total = count(cte + " SELECT COUNT_BIG(*)" + from,
                parameters, "findMovements");
        String sql = cte + " SELECT m.*" + from
                + " ORDER BY m.NgayGiaoDich DESC, m.SoPhieu DESC, m.MaLoThuoc"
                + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";
        List<Object> queryParameters = withPaging(parameters, filter.page(), filter.pageSize());
        List<StockMovement> movements = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, queryParameters);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    movements.add(mapMovement(result));
                }
            }
            return new Page<>(movements, total, filter.page(), filter.pageSize());
        } catch (SQLException exception) {
            throw databaseError("findMovements", exception);
        }
    }

    public Page<ExportCandidate> findExportCandidates(ExportFilter filter) {
        StringBuilder where = new StringBuilder("""
                 WHERE dt.TrangThai IN (N'DaKy', N'DaGuiKho')
                   AND NOT EXISTS (
                       SELECT 1 FROM dbo.PhieuXuat px
                       WHERE px.MaDonThuoc = dt.MaDonThuoc
                   )
                """);
        List<Object> parameters = new ArrayList<>();
        appendExportSearch(where, parameters, filter.searchText());
        String from = """
                FROM dbo.DonThuoc dt
                JOIN dbo.LuotKham lk ON lk.MaLuotKham = dt.MaLuotKham
                JOIN dbo.BenhNhan bn ON bn.MaBenhNhan = lk.MaBenhNhan
                JOIN dbo.BacSi bs ON bs.MaNhanVien = dt.MaNhanVien
                JOIN dbo.NhanVien nv ON nv.MaNhanVien = bs.MaNhanVien
                """ + where;
        long total = count("SELECT COUNT_BIG(*) " + from, parameters,
                "findExportCandidates");
        String sql = """
                SELECT dt.MaDonThuoc, dt.MaLuotKham, bn.MaBenhNhan,
                       bn.HoTen AS TenBenhNhan, nv.HoTen AS TenBacSi,
                       dt.NgayKe, dt.TrangThai,
                       (SELECT COUNT_BIG(*) FROM dbo.ChiTietDonThuoc c
                        WHERE c.MaDonThuoc = dt.MaDonThuoc) AS SoLoaiThuoc
                """ + from + """
                 ORDER BY dt.NgayKe, dt.MaDonThuoc
                 OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """;
        List<Object> queryParameters = withPaging(parameters, filter.page(), filter.pageSize());
        List<ExportCandidate> candidates = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, queryParameters);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    candidates.add(mapExportCandidate(result));
                }
            }
            return new Page<>(candidates, total, filter.page(), filter.pageSize());
        } catch (SQLException exception) {
            throw databaseError("findExportCandidates", exception);
        }
    }

    public ExportPreview findExportPreview(long prescriptionId) {
        ExportCandidate candidate = findExportCandidate(prescriptionId);
        if (candidate == null) {
            return null;
        }
        List<PrescriptionLine> prescriptionLines = findPrescriptionLines(prescriptionId);
        if (prescriptionLines.isEmpty()) {
            throw new WarehouseDAOException("Đơn thuốc không có chi tiết thuốc để xuất.");
        }
        Set<String> medicineIds = new LinkedHashSet<>();
        for (PrescriptionLine line : prescriptionLines) {
            medicineIds.add(line.medicineId());
        }
        Map<String, List<BatchAllocationPreview>> availableBatches =
                findAvailableBatches(medicineIds, false);
        List<ExportLinePreview> previews = new ArrayList<>();
        boolean canFulfill = true;
        for (PrescriptionLine line : prescriptionLines) {
            List<BatchAllocationPreview> batches =
                    availableBatches.getOrDefault(line.medicineId(), List.of());
            BigDecimal available = batches.stream()
                    .map(BatchAllocationPreview::availableQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (available.compareTo(line.quantity()) < 0) {
                canFulfill = false;
            }
            previews.add(new ExportLinePreview(
                    line.lineId(),
                    line.medicineId(),
                    line.medicineName(),
                    line.quantity(),
                    available,
                    batches));
        }
        return new ExportPreview(candidate, previews, canFulfill);
    }

    public CreatedReceipt createImport(
            ImportDraft draft, String employeeId, long accountId) {
        String receiptNumber = createReceiptNumber("PN");
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            connection.setAutoCommit(false);
            try {
                verifyEmployee(connection, employeeId);
                BigDecimal total = draft.lines().stream()
                        .map(line -> line.quantity().multiply(line.unitCost())
                                .setScale(2, RoundingMode.HALF_UP))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                if (total.compareTo(MAX_RECEIPT_TOTAL) > 0) {
                    throw new WarehouseDAOException(
                            "Tổng tiền phiếu nhập vượt giới hạn lưu trữ.");
                }
                long receiptId = insertImportHeader(
                        connection, receiptNumber, employeeId, draft, total);
                for (ImportLineDraft line : draft.lines()) {
                    verifyActiveMedicine(connection, line.medicineId());
                    LockedBatch batch = lockOrCreateBatch(connection, line);
                    insertImportLine(connection, receiptId, batch.id(), line);
                    updateBatchAfterImport(connection, batch, line);
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE dbo.PhieuNhap
                        SET TrangThai = N'DaXacNhan'
                        WHERE MaPhieuNhap = ? AND TrangThai = N'Nhap'
                        """)) {
                    statement.setLong(1, receiptId);
                    if (statement.executeUpdate() != 1) {
                        throw new WarehouseDAOException(
                                "Không thể xác nhận phiếu nhập vừa tạo.");
                    }
                }
                recordAudit(connection, accountId, "XAC_NHAN_PHIEU_NHAP",
                        "PhieuNhap", Long.toString(receiptId));
                connection.commit();
                return new CreatedReceipt(receiptId, receiptNumber);
            } catch (WarehouseDAOException exception) {
                rollback(connection, exception);
                throw exception;
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw databaseError("createImport", exception);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw databaseError("createImport", exception);
        }
    }

    public CreatedReceipt exportPrescription(
            long prescriptionId, String employeeId, long accountId) {
        String receiptNumber = createReceiptNumber("PX");
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            connection.setAutoCommit(false);
            try {
                LockedPrescription prescription =
                        lockExportablePrescription(connection, prescriptionId);
                verifyEmployee(connection, employeeId);
                List<PrescriptionLine> lines =
                        findPrescriptionLines(connection, prescriptionId, true);
                if (lines.isEmpty()) {
                    throw new WarehouseDAOException(
                            "Đơn thuốc không có chi tiết để cấp phát.");
                }
                Set<String> medicineIds = new LinkedHashSet<>();
                for (PrescriptionLine line : lines) {
                    medicineIds.add(line.medicineId());
                }
                Map<String, List<LockedLot>> availableLots =
                        findLockedLots(connection, medicineIds);
                List<LockedAllocation> allocations = allocateByFefo(lines, availableLots);
                long receiptId = insertExportHeader(
                        connection, receiptNumber, prescription, employeeId);
                updateStockForAllocations(connection, allocations);
                insertExportLines(connection, receiptId, allocations);
                try (PreparedStatement statement = connection.prepareStatement("""
                        UPDATE dbo.DonThuoc
                        SET TrangThai = N'DaCapPhat'
                        WHERE MaDonThuoc = ?
                          AND TrangThai IN (N'DaKy', N'DaGuiKho')
                        """)) {
                    statement.setLong(1, prescriptionId);
                    if (statement.executeUpdate() != 1) {
                        throw new WarehouseDAOException(
                                "Trạng thái đơn thuốc đã thay đổi; hãy tải lại.");
                    }
                }
                confirmExportHeader(connection, receiptId);
                recordAudit(connection, accountId, "XAC_NHAN_PHIEU_XUAT",
                        "PhieuXuat", Long.toString(receiptId));
                connection.commit();
                return new CreatedReceipt(receiptId, receiptNumber);
            } catch (WarehouseDAOException exception) {
                rollback(connection, exception);
                throw exception;
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw databaseError("exportPrescription", exception);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw databaseError("exportPrescription", exception);
        }
    }

                private String importReceiptSource() {
                    return """
                            SELECT p.MaPhieuNhap AS MaPhieu, p.SoPhieu, p.NgayNhap AS NgayGiaoDich,
                                   p.NhaCungCap AS DoiTac, p.TrangThai,
                                   COALESCE((SELECT SUM(c.ThanhTien) FROM dbo.ChiTietPhieuNhap c
                                             WHERE c.MaPhieuNhap = p.MaPhieuNhap), 0) AS TongTien,
                                   nv.MaNhanVien, nv.HoTen AS TenNhanVien, CAST(NULL AS bigint) AS MaDonThuoc,
                                   CAST(NULL AS nvarchar(200)) AS TenBenhNhan,
                                   (SELECT COUNT_BIG(*) FROM dbo.ChiTietPhieuNhap c
                                    WHERE c.MaPhieuNhap = p.MaPhieuNhap) AS SoDong,
                                   (SELECT COUNT_BIG(*) FROM dbo.ChiTietNhapHoaChat c
                                    WHERE c.MaPhieuNhap = p.MaPhieuNhap) AS SoDongHoaChat
                            FROM dbo.PhieuNhap p
                            JOIN dbo.NhanVien nv ON nv.MaNhanVien = p.MaNhanVienNhap
                            WHERE EXISTS (SELECT 1 FROM dbo.ChiTietPhieuNhap c
                                          WHERE c.MaPhieuNhap = p.MaPhieuNhap)
                            """;
                }

                private String exportReceiptSource() {
                    return """
                            SELECT p.MaPhieuXuat AS MaPhieu, p.SoPhieu, p.NgayXuat AS NgayGiaoDich,
                                   p.NguoiNhan AS DoiTac, p.TrangThai,
                                   COALESCE((SELECT SUM(c.SoLuong * c.DonGia)
                                             FROM dbo.ChiTietPhieuXuat c
                                             WHERE c.MaPhieuXuat = p.MaPhieuXuat), 0) AS TongTien,
                                   nv.MaNhanVien, nv.HoTen AS TenNhanVien, p.MaDonThuoc,
                                   bn.HoTen AS TenBenhNhan,
                                   (SELECT COUNT_BIG(*) FROM dbo.ChiTietPhieuXuat c
                                    WHERE c.MaPhieuXuat = p.MaPhieuXuat) AS SoDong,
                                   (SELECT COUNT_BIG(*) FROM dbo.ChiTietXuatHoaChat c
                                    WHERE c.MaPhieuXuat = p.MaPhieuXuat) AS SoDongHoaChat
                            FROM dbo.PhieuXuat p
                            JOIN dbo.NhanVien nv ON nv.MaNhanVien = p.MaNhanVienXuat
                            LEFT JOIN dbo.DonThuoc dt ON dt.MaDonThuoc = p.MaDonThuoc
                            LEFT JOIN dbo.LuotKham lk ON lk.MaLuotKham = dt.MaLuotKham
                            LEFT JOIN dbo.BenhNhan bn ON bn.MaBenhNhan = lk.MaBenhNhan
                            WHERE EXISTS (SELECT 1 FROM dbo.ChiTietPhieuXuat c
                                          WHERE c.MaPhieuXuat = p.MaPhieuXuat)
                            """;
                }

                private String combinedReceiptSource() {
                    return """
                            SELECT * FROM (
                            """ + importReceiptSource() + """
                            UNION ALL
                            """ + exportReceiptSource() + """
                            ) r
                            """;
                }

                private void appendReceiptFilters(
                        StringBuilder where, List<Object> parameters, ReceiptFilter filter) {
                    if (filter.fromDate() != null) {
                        where.append(" AND r.NgayGiaoDich >= ?");
                        parameters.add(Date.valueOf(filter.fromDate()));
                    }
                    if (filter.toDate() != null) {
                        where.append(" AND r.NgayGiaoDich < ?");
                        parameters.add(Date.valueOf(filter.toDate().plusDays(1)));
                    }
                    if (filter.status() != null && !filter.status().isBlank()) {
                        where.append(" AND r.TrangThai = ?");
                        parameters.add(filter.status());
                    }
                    if (filter.searchText() != null && !filter.searchText().isBlank()) {
                        where.append("""
                                 AND (r.SoPhieu LIKE ? ESCAPE '\\'
                                      OR r.MaNhanVien LIKE ? ESCAPE '\\'
                                      OR r.TenNhanVien LIKE ? ESCAPE '\\'
                                      OR r.DoiTac LIKE ? ESCAPE '\\'
                                      OR r.TenBenhNhan LIKE ? ESCAPE '\\')
                                """);
                        String pattern = "%" + escapeLike(filter.searchText().trim()) + "%";
                        parameters.add(pattern);
                        parameters.add(pattern);
                        parameters.add(pattern);
                        parameters.add(pattern);
                        parameters.add(pattern);
                    }
                }

                private void appendMovementFilters(
                        StringBuilder where, List<Object> parameters, MovementFilter filter) {
                    if (filter.fromDate() != null) {
                        where.append(" AND m.NgayGiaoDich >= ?");
                        parameters.add(Timestamp.valueOf(filter.fromDate().atStartOfDay()));
                    }
                    if (filter.toDate() != null) {
                        where.append(" AND m.NgayGiaoDich < ?");
                        parameters.add(Timestamp.valueOf(filter.toDate().plusDays(1).atStartOfDay()));
                    }
                    if (filter.movementType() != null && !filter.movementType().isBlank()) {
                        where.append(" AND m.LoaiGiaoDich = ?");
                        parameters.add(filter.movementType());
                    }
                    if (filter.status() != null && !filter.status().isBlank()) {
                        where.append(" AND m.TrangThai = ?");
                        parameters.add(filter.status());
                    }
                    if (filter.searchText() != null && !filter.searchText().isBlank()) {
                        where.append("""
                                 AND (m.SoPhieu LIKE ? ESCAPE '\\'
                                      OR m.MaThuoc LIKE ? ESCAPE '\\'
                                      OR m.TenThuoc LIKE ? ESCAPE '\\'
                                      OR m.SoLo LIKE ? ESCAPE '\\'
                                      OR m.NhanVien LIKE ? ESCAPE '\\')
                                """);
                        String pattern = "%" + escapeLike(filter.searchText().trim()) + "%";
                        for (int i = 0; i < 5; i++) {
                            parameters.add(pattern);
                        }
                    }
                }

                private void appendExportSearch(
                        StringBuilder where, List<Object> parameters, String searchText) {
                    if (searchText == null || searchText.isBlank()) {
                        return;
                    }
                    where.append("""
                             AND (CONVERT(varchar(20), dt.MaDonThuoc) LIKE ? ESCAPE '\\'
                                  OR bn.MaBenhNhan LIKE ? ESCAPE '\\'
                                  OR bn.HoTen LIKE ? ESCAPE '\\'
                                  OR nv.HoTen LIKE ? ESCAPE '\\')
                            """);
                    String pattern = "%" + escapeLike(searchText.trim()) + "%";
                    for (int i = 0; i < 4; i++) {
                        parameters.add(pattern);
                    }
                }

                private ExportCandidate findExportCandidate(long prescriptionId) {
                    String sql = """
                            SELECT dt.MaDonThuoc, dt.MaLuotKham, bn.MaBenhNhan,
                                   bn.HoTen AS TenBenhNhan, nv.HoTen AS TenBacSi,
                                   dt.NgayKe, dt.TrangThai,
                                   (SELECT COUNT_BIG(*) FROM dbo.ChiTietDonThuoc c
                                    WHERE c.MaDonThuoc = dt.MaDonThuoc) AS SoLoaiThuoc
                            FROM dbo.DonThuoc dt
                            JOIN dbo.LuotKham lk ON lk.MaLuotKham = dt.MaLuotKham
                            JOIN dbo.BenhNhan bn ON bn.MaBenhNhan = lk.MaBenhNhan
                            JOIN dbo.BacSi bs ON bs.MaNhanVien = dt.MaNhanVien
                            JOIN dbo.NhanVien nv ON nv.MaNhanVien = bs.MaNhanVien
                            WHERE dt.MaDonThuoc = ?
                              AND dt.TrangThai IN (N'DaKy', N'DaGuiKho')
                              AND NOT EXISTS (SELECT 1 FROM dbo.PhieuXuat px
                                              WHERE px.MaDonThuoc = dt.MaDonThuoc)
                            """;
                    try (Connection connection = DatabaseConnection.getConnection();
                         PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setLong(1, prescriptionId);
                        try (ResultSet result = statement.executeQuery()) {
                            return result.next() ? mapExportCandidate(result) : null;
                        }
                    } catch (SQLException exception) {
                        throw databaseError("findExportCandidate", exception);
                    }
                }

                private List<PrescriptionLine> findPrescriptionLines(long prescriptionId) {
                    try (Connection connection = DatabaseConnection.getConnection()) {
                        return findPrescriptionLines(connection, prescriptionId, false);
                    } catch (SQLException exception) {
                        throw databaseError("findPrescriptionLines", exception);
                    }
                }

                private List<PrescriptionLine> findPrescriptionLines(
                        Connection connection, long prescriptionId, boolean lockRows) throws SQLException {
                    String hint = lockRows ? " WITH (UPDLOCK, ROWLOCK, HOLDLOCK) " : " ";
                    String sql = """
                            SELECT c.MaChiTietDon, c.MaThuoc, t.TenThuoc, c.SoLuong
                            FROM dbo.ChiTietDonThuoc c""" + hint + """
                            JOIN dbo.Thuoc t ON t.MaThuoc = c.MaThuoc
                            WHERE c.MaDonThuoc = ?
                            ORDER BY c.MaChiTietDon
                            """;
                    List<PrescriptionLine> lines = new ArrayList<>();
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setLong(1, prescriptionId);
                        try (ResultSet result = statement.executeQuery()) {
                            while (result.next()) {
                                lines.add(new PrescriptionLine(
                                        result.getLong("MaChiTietDon"),
                                        result.getString("MaThuoc"),
                                        result.getString("TenThuoc"),
                                        result.getBigDecimal("SoLuong")));
                            }
                        }
                    }
                    return lines;
                }

                private Map<String, List<BatchAllocationPreview>> findAvailableBatches(
                        Set<String> medicineIds, boolean lockRows) {
                    Map<String, List<BatchAllocationPreview>> byMedicine = new LinkedHashMap<>();
                    if (medicineIds.isEmpty()) {
                        return byMedicine;
                    }
                    List<String> ids = new ArrayList<>(medicineIds);
                    String placeholders = placeholders(ids.size());
                    String hint = lockRows ? " WITH (UPDLOCK, ROWLOCK, HOLDLOCK) " : " ";
                    String sql = """
                            SELECT l.MaLoThuoc, l.MaThuoc, l.SoLo, l.HanSuDung,
                                   l.SoLuongTon, l.DonGiaNhap, l.ViTriLuuTru
                            FROM dbo.LoThuoc l""" + hint + """
                            WHERE l.MaThuoc IN (""" + placeholders + """
                            )
                              AND l.TrangThai = N'DangTon'
                              AND l.HanSuDung >= CAST(SYSDATETIME() AS date)
                              AND l.SoLuongTon > 0
                            ORDER BY l.MaThuoc, l.HanSuDung, l.MaLoThuoc
                            """;
                    try (Connection connection = DatabaseConnection.getConnection();
                         PreparedStatement statement = connection.prepareStatement(sql)) {
                        bind(statement, new ArrayList<>(ids));
                        try (ResultSet result = statement.executeQuery()) {
                            while (result.next()) {
                                BatchAllocationPreview batch = new BatchAllocationPreview(
                                        result.getLong("MaLoThuoc"),
                                        result.getString("SoLo"),
                                        result.getDate("HanSuDung").toLocalDate(),
                                        result.getBigDecimal("SoLuongTon"),
                                        result.getBigDecimal("DonGiaNhap"));
                                byMedicine.computeIfAbsent(
                                        result.getString("MaThuoc"), ignored -> new ArrayList<>())
                                        .add(batch);
                            }
                        }
                        return byMedicine;
                    } catch (SQLException exception) {
                        throw databaseError("findAvailableBatches", exception);
                    }
                }

                private Map<String, List<LockedLot>> findLockedLots(
                        Connection connection, Set<String> medicineIds) throws SQLException {
                    Map<String, List<LockedLot>> byMedicine = new LinkedHashMap<>();
                    if (medicineIds.isEmpty()) {
                        return byMedicine;
                    }
                    List<String> ids = new ArrayList<>(medicineIds);
                    String sql = """
                            SELECT l.MaLoThuoc, l.MaThuoc, l.SoLo, l.HanSuDung,
                                   l.SoLuongTon, l.DonGiaNhap, l.ViTriLuuTru
                            FROM dbo.LoThuoc l WITH (UPDLOCK, ROWLOCK, HOLDLOCK)
                            WHERE l.MaThuoc IN (""" + placeholders(ids.size()) + """
                            )
                              AND l.TrangThai = N'DangTon'
                              AND l.HanSuDung >= CAST(SYSDATETIME() AS date)
                              AND l.SoLuongTon > 0
                            ORDER BY l.MaThuoc, l.HanSuDung, l.MaLoThuoc
                            """;
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        bind(statement, new ArrayList<>(ids));
                        try (ResultSet result = statement.executeQuery()) {
                            while (result.next()) {
                                LockedLot lot = mapLockedLot(result);
                                byMedicine.computeIfAbsent(lot.medicineId(), ignored -> new ArrayList<>())
                                        .add(lot);
                            }
                        }
                    }
                    return byMedicine;
                }

                private void verifyEmployee(Connection connection, String employeeId)
                        throws SQLException {
                    try (PreparedStatement statement = connection.prepareStatement("""
                            SELECT 1 FROM dbo.NhanVien WITH (UPDLOCK, HOLDLOCK)
                            WHERE MaNhanVien = ?
                            """)) {
                        statement.setString(1, employeeId);
                        try (ResultSet result = statement.executeQuery()) {
                            if (!result.next()) {
                                throw new WarehouseDAOException(
                                        "Tài khoản nhân viên hiện tại không còn hợp lệ.");
                            }
                        }
                    }
                }

                private void verifyActiveMedicine(Connection connection, String medicineId)
                        throws SQLException {
                    try (PreparedStatement statement = connection.prepareStatement("""
                            SELECT TrangThai FROM dbo.Thuoc WITH (UPDLOCK, HOLDLOCK)
                            WHERE MaThuoc = ?
                            """)) {
                        statement.setString(1, medicineId);
                        try (ResultSet result = statement.executeQuery()) {
                            if (!result.next()) {
                                throw new WarehouseDAOException(
                                        "Không tìm thấy thuốc " + medicineId + ".");
                            }
                            if (!"DangSuDung".equals(result.getString("TrangThai"))) {
                                throw new WarehouseDAOException(
                                        "Thuốc " + medicineId + " đã ngừng sử dụng.");
                            }
                        }
                    }
                }

                private LockedBatch lockOrCreateBatch(
                        Connection connection, ImportLineDraft line) throws SQLException {
                    String sql = """
                            SELECT MaLoThuoc, NgaySanXuat, HanSuDung, DonGiaNhap,
                                   SoLuongTon, ViTriLuuTru, TrangThai
                            FROM dbo.LoThuoc WITH (UPDLOCK, ROWLOCK, HOLDLOCK)
                            WHERE MaThuoc = ? AND SoLo = ?
                            """;
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setString(1, line.medicineId());
                        statement.setString(2, line.batchNumber());
                        try (ResultSet result = statement.executeQuery()) {
                            if (result.next()) {
                                LocalDate manufactureDate = localDate(result, "NgaySanXuat");
                                LocalDate expiryDate = localDate(result, "HanSuDung");
                                if (!sameDate(manufactureDate, line.manufactureDate())
                                        || !expiryDate.equals(line.expiryDate())) {
                                    throw new WarehouseDAOException(
                                            "Thông tin ngày của lô " + line.batchNumber()
                                                    + " không khớp lô đã tồn tại.");
                                }
                                String status = result.getString("TrangThai");
                                if ("BiKhoa".equals(status)) {
                                    throw new WarehouseDAOException(
                                            "Lô " + line.batchNumber() + " đang bị khóa.");
                                }
                                if (expiryDate.isBefore(LocalDate.now())) {
                                    throw new WarehouseDAOException(
                                            "Không thể nhập bổ sung lô đã hết hạn.");
                                }
                                return new LockedBatch(
                                        result.getLong("MaLoThuoc"),
                                        zeroIfNull(result.getBigDecimal("SoLuongTon")),
                                        zeroIfNull(result.getBigDecimal("DonGiaNhap")),
                                        result.getString("ViTriLuuTru"),
                                        false);
                            }
                        }
                    }
                    String insertSql = """
                            INSERT INTO dbo.LoThuoc
                                (MaThuoc, SoLo, NgaySanXuat, HanSuDung, DonGiaNhap,
                                 SoLuongTon, ViTriLuuTru, TrangThai)
                            VALUES (?, ?, ?, ?, ?, 0, ?, N'DangTon')
                            """;
                    try (PreparedStatement statement = connection.prepareStatement(
                            insertSql, Statement.RETURN_GENERATED_KEYS)) {
                        statement.setString(1, line.medicineId());
                        statement.setString(2, line.batchNumber());
                        setDate(statement, 3, line.manufactureDate());
                        statement.setDate(4, Date.valueOf(line.expiryDate()));
                        statement.setBigDecimal(5, line.unitCost());
                        setString(statement, 6, line.location());
                        statement.executeUpdate();
                        try (ResultSet keys = statement.getGeneratedKeys()) {
                            if (!keys.next()) {
                                throw new SQLException("SQL Server không trả về mã lô thuốc mới.");
                            }
                            return new LockedBatch(
                                    keys.getLong(1), BigDecimal.ZERO, line.unitCost(),
                                    line.location(), true);
                        }
                    }
                }

                private long insertImportHeader(
                        Connection connection,
                        String receiptNumber,
                        String employeeId,
                        ImportDraft draft,
                        BigDecimal total) throws SQLException {
                    String sql = """
                            INSERT INTO dbo.PhieuNhap
                                (SoPhieu, NgayNhap, NhaCungCap, MaNhanVienNhap,
                                 TongTien, TrangThai, GhiChu)
                            VALUES (?, SYSDATETIME(), ?, ?, ?, N'Nhap', ?)
                            """;
                    try (PreparedStatement statement = connection.prepareStatement(
                            sql, Statement.RETURN_GENERATED_KEYS)) {
                        statement.setString(1, receiptNumber);
                        setString(statement, 2, draft.supplier());
                        statement.setString(3, employeeId);
                        statement.setBigDecimal(4, total.setScale(2, RoundingMode.HALF_UP));
                        setString(statement, 5, draft.note());
                        statement.executeUpdate();
                        try (ResultSet keys = statement.getGeneratedKeys()) {
                            if (!keys.next()) {
                                throw new SQLException("SQL Server không trả về mã phiếu nhập mới.");
                            }
                            return keys.getLong(1);
                        }
                    }
                }

                private void insertImportLine(
                        Connection connection, long receiptId, long batchId, ImportLineDraft line)
                        throws SQLException {
                    String sql = """
                            INSERT INTO dbo.ChiTietPhieuNhap
                                (MaPhieuNhap, MaLoThuoc, SoLuong, DonGia, ThanhTien)
                            VALUES (?, ?, ?, ?, ?)
                            """;
                    BigDecimal lineTotal = line.quantity().multiply(line.unitCost())
                            .setScale(2, RoundingMode.HALF_UP);
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setLong(1, receiptId);
                        statement.setLong(2, batchId);
                        statement.setBigDecimal(3, line.quantity());
                        statement.setBigDecimal(4, line.unitCost());
                        statement.setBigDecimal(5, lineTotal);
                        statement.executeUpdate();
                    }
                }

                private void updateBatchAfterImport(
                        Connection connection, LockedBatch batch, ImportLineDraft line)
                        throws SQLException {
                    BigDecimal nextStock = batch.stock().add(line.quantity());
                    if (nextStock.compareTo(MAX_STOCK_VALUE) > 0) {
                        throw new WarehouseDAOException("Tồn lô vượt giới hạn lưu trữ.");
                    }
                    BigDecimal nextCost = line.unitCost();
                    if (!batch.created() && batch.stock().signum() > 0) {
                        BigDecimal oldValue = batch.stock().multiply(
                                batch.unitCost() == null ? BigDecimal.ZERO : batch.unitCost());
                        nextCost = oldValue.add(line.quantity().multiply(line.unitCost()))
                                .divide(nextStock, 2, RoundingMode.HALF_UP);
                    }
                    String sql = """
                            UPDATE dbo.LoThuoc
                            SET SoLuongTon = SoLuongTon + ?,
                                DonGiaNhap = ?,
                                ViTriLuuTru = COALESCE(NULLIF(ViTriLuuTru, N''), ?),
                                TrangThai = N'DangTon'
                            WHERE MaLoThuoc = ? AND TrangThai <> N'BiKhoa'
                            """;
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setBigDecimal(1, line.quantity());
                        statement.setBigDecimal(2, nextCost);
                        setString(statement, 3, line.location());
                        statement.setLong(4, batch.id());
                        if (statement.executeUpdate() != 1) {
                            throw new WarehouseDAOException(
                                    "Không thể cập nhật tồn kho lô " + line.batchNumber() + ".");
                        }
                    }
                }

                private LockedPrescription lockExportablePrescription(
                        Connection connection, long prescriptionId) throws SQLException {
                    String sql = """
                            SELECT dt.MaDonThuoc, dt.TrangThai, bn.HoTen AS TenBenhNhan
                            FROM dbo.DonThuoc dt WITH (UPDLOCK, ROWLOCK, HOLDLOCK)
                            JOIN dbo.LuotKham lk ON lk.MaLuotKham = dt.MaLuotKham
                            JOIN dbo.BenhNhan bn ON bn.MaBenhNhan = lk.MaBenhNhan
                            WHERE dt.MaDonThuoc = ?
                            """;
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setLong(1, prescriptionId);
                        try (ResultSet result = statement.executeQuery()) {
                            if (!result.next()) {
                                throw new WarehouseDAOException("Không tìm thấy đơn thuốc.");
                            }
                            String status = result.getString("TrangThai");
                            if (!"DaKy".equals(status) && !"DaGuiKho".equals(status)) {
                                throw new WarehouseDAOException(
                                        "Chỉ cấp phát đơn thuốc đã ký hoặc đã gửi kho.");
                            }
                            try (PreparedStatement exportCheck = connection.prepareStatement("""
                                    SELECT 1 FROM dbo.PhieuXuat WITH (UPDLOCK, HOLDLOCK)
                                    WHERE MaDonThuoc = ?
                                    """)) {
                                exportCheck.setLong(1, prescriptionId);
                                try (ResultSet existing = exportCheck.executeQuery()) {
                                    if (existing.next()) {
                                        throw new WarehouseDAOException(
                                                "Đơn thuốc này đã có phiếu xuất; không thể cấp phát trùng.");
                                    }
                                }
                            }
                            return new LockedPrescription(
                                    result.getLong("MaDonThuoc"),
                                    result.getString("TenBenhNhan"));
                        }
                    }
                }

                private List<LockedAllocation> allocateByFefo(
                        List<PrescriptionLine> lines,
                        Map<String, List<LockedLot>> availableLots) {
                    Map<Long, BigDecimal> remainingByLot = new LinkedHashMap<>();
                    for (List<LockedLot> lots : availableLots.values()) {
                        for (LockedLot lot : lots) {
                            remainingByLot.put(lot.id(), lot.stock());
                        }
                    }
                    List<LockedAllocation> allocations = new ArrayList<>();
                    for (PrescriptionLine line : lines) {
                        BigDecimal remaining = line.quantity();
                        List<LockedLot> lots = availableLots.getOrDefault(line.medicineId(), List.of());
                        for (LockedLot lot : lots) {
                            BigDecimal available = remainingByLot.getOrDefault(lot.id(), BigDecimal.ZERO);
                            if (available.signum() <= 0) {
                                continue;
                            }
                            BigDecimal quantity = remaining.min(available);
                            allocations.add(new LockedAllocation(line, lot, quantity));
                            remainingByLot.put(lot.id(), available.subtract(quantity));
                            remaining = remaining.subtract(quantity);
                            if (remaining.signum() == 0) {
                                break;
                            }
                        }
                        if (remaining.signum() > 0) {
                            throw new WarehouseDAOException(
                                    "Tồn kho khả dụng không đủ cho thuốc " + line.medicineId()
                                            + ": cần " + line.quantity() + ", còn "
                                            + line.quantity().subtract(remaining) + ".");
                        }
                    }
                    return allocations;
                }

                private long insertExportHeader(
                        Connection connection,
                        String receiptNumber,
                        LockedPrescription prescription,
                        String employeeId) throws SQLException {
                    String sql = """
                            INSERT INTO dbo.PhieuXuat
                                (SoPhieu, NgayXuat, LoaiXuat, MaDonThuoc, MaNhanVienXuat,
                                 NguoiNhan, TrangThai, GhiChu)
                            VALUES (?, SYSDATETIME(), N'CapPhatDonThuoc', ?, ?, ?, N'Nhap',
                                    N'Cấp phát theo đơn thuốc')
                            """;
                    try (PreparedStatement statement = connection.prepareStatement(
                            sql, Statement.RETURN_GENERATED_KEYS)) {
                        statement.setString(1, receiptNumber);
                        statement.setLong(2, prescription.id());
                        statement.setString(3, employeeId);
                        statement.setNString(4, prescription.patientName());
                        statement.executeUpdate();
                        try (ResultSet keys = statement.getGeneratedKeys()) {
                            if (!keys.next()) {
                                throw new SQLException("SQL Server không trả về mã phiếu xuất mới.");
                            }
                            return keys.getLong(1);
                        }
                    }
                }

                private void updateStockForAllocations(
                        Connection connection, List<LockedAllocation> allocations) throws SQLException {
                    Map<Long, BigDecimal> deductionByLot = new LinkedHashMap<>();
                    Map<Long, LockedLot> lotById = new LinkedHashMap<>();
                    for (LockedAllocation allocation : allocations) {
                        deductionByLot.merge(
                                allocation.lot().id(), allocation.quantity(), BigDecimal::add);
                        lotById.put(allocation.lot().id(), allocation.lot());
                    }
                    String sql = """
                            UPDATE dbo.LoThuoc
                            SET SoLuongTon = SoLuongTon - ?,
                                TrangThai = CASE WHEN SoLuongTon - ? <= 0
                                                 THEN N'HetHang' ELSE N'DangTon' END
                            WHERE MaLoThuoc = ? AND SoLuongTon >= ?
                              AND TrangThai = N'DangTon'
                              AND HanSuDung >= CAST(SYSDATETIME() AS date)
                            """;
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        for (Map.Entry<Long, BigDecimal> entry : deductionByLot.entrySet()) {
                            BigDecimal quantity = entry.getValue();
                            statement.setBigDecimal(1, quantity);
                            statement.setBigDecimal(2, quantity);
                            statement.setLong(3, entry.getKey());
                            statement.setBigDecimal(4, quantity);
                            if (statement.executeUpdate() != 1) {
                                LockedLot lot = lotById.get(entry.getKey());
                                throw new WarehouseDAOException(
                                        "Tồn lô " + lot.batchNumber()
                                                + " vừa thay đổi; hãy tải lại và thử lại.");
                            }
                        }
                    }
                }

                private void insertExportLines(
                        Connection connection, long receiptId, List<LockedAllocation> allocations)
                        throws SQLException {
                    String sql = """
                            INSERT INTO dbo.ChiTietPhieuXuat
                                (MaPhieuXuat, MaLoThuoc, MaChiTietDon, SoLuong, DonGia)
                            VALUES (?, ?, ?, ?, ?)
                            """;
                    try (PreparedStatement statement = connection.prepareStatement(sql)) {
                        for (LockedAllocation allocation : allocations) {
                            statement.setLong(1, receiptId);
                            statement.setLong(2, allocation.lot().id());
                            statement.setLong(3, allocation.prescriptionLine().lineId());
                            statement.setBigDecimal(4, allocation.quantity());
                            statement.setBigDecimal(5, allocation.lot().unitCost());
                            statement.addBatch();
                        }
                        statement.executeBatch();
                    }
                }

                private void confirmExportHeader(Connection connection, long receiptId)
                        throws SQLException {
                    try (PreparedStatement statement = connection.prepareStatement("""
                            UPDATE dbo.PhieuXuat
                            SET TrangThai = N'DaXacNhan'
                            WHERE MaPhieuXuat = ? AND TrangThai = N'Nhap'
                            """)) {
                        statement.setLong(1, receiptId);
                        if (statement.executeUpdate() != 1) {
                            throw new WarehouseDAOException("Không thể xác nhận phiếu xuất vừa tạo.");
                        }
                    }
                }

                private void recordAudit(
                        Connection connection,
                        long accountId,
                        String action,
                        String object,
                        String objectId) throws SQLException {
                    try (PreparedStatement statement = connection.prepareStatement("""
                            INSERT INTO dbo.NhatKyHeThong
                                (MaTaiKhoan, HanhDong, DoiTuong, MaDoiTuong, NoiDung)
                            VALUES (?, ?, ?, ?, ?)
                            """)) {
                        statement.setLong(1, accountId);
                        statement.setNString(2, action);
                        statement.setNString(3, object);
                        statement.setString(4, objectId);
                        statement.setNString(5, "{}");
                        statement.executeUpdate();
                    }
                }

                private Batch findBatchById(long batchId) {
                    String sql = BATCH_SELECT + " WHERE l.MaLoThuoc = ?";
                    try (Connection connection = DatabaseConnection.getConnection();
                         PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setLong(1, batchId);
                        try (ResultSet result = statement.executeQuery()) {
                            return result.next() ? mapBatch(result) : null;
                        }
                    } catch (SQLException exception) {
                        throw databaseError("findBatchById", exception);
                    }
                }

                private List<StockMovement> findMovementsForBatch(long batchId) {
                    String sql = "WITH MovementData AS (" + MOVEMENT_SOURCE + """
                            )
                            SELECT m.* FROM MovementData m
                            WHERE m.MaLoThuoc = ?
                            ORDER BY m.NgayGiaoDich DESC, m.SoPhieu DESC
                            """;
                    List<StockMovement> movements = new ArrayList<>();
                    try (Connection connection = DatabaseConnection.getConnection();
                         PreparedStatement statement = connection.prepareStatement(sql)) {
                        statement.setLong(1, batchId);
                        try (ResultSet result = statement.executeQuery()) {
                            while (result.next()) {
                                movements.add(mapMovement(result));
                            }
                        }
                        return movements;
                    } catch (SQLException exception) {
                        throw databaseError("findMovementsForBatch", exception);
                    }
                }

                private String inventoryCte(int expiryWarningDays) {
                    return """
                            WITH InventoryByMedicine AS (
                                SELECT l.MaThuoc,
                                       COALESCE(SUM(l.SoLuongTon), 0) AS TongTon,
                                       COUNT_BIG(*) AS SoLo,
                                       SUM(CASE WHEN l.SoLuongTon > 0
                                                     AND l.HanSuDung >= CAST(SYSDATETIME() AS date)
                                                     AND l.HanSuDung < DATEADD(day, %d,
                                                            CAST(SYSDATETIME() AS date))
                                                THEN 1 ELSE 0 END) AS LoSapHetHan,
                                       SUM(CASE WHEN l.SoLuongTon > 0
                                                     AND l.HanSuDung < CAST(SYSDATETIME() AS date)
                                                THEN 1 ELSE 0 END) AS LoHetHan,
                                       SUM(CASE WHEN l.SoLuongTon <= 0
                                                THEN 1 ELSE 0 END) AS LoHetHang
                                FROM dbo.LoThuoc l
                                GROUP BY l.MaThuoc
                            )
                            """.formatted(expiryWarningDays);
                }

                private String inventoryStockCondition(String stockStatus) {
                    if (stockStatus == null || stockStatus.isBlank()) {
                        return "";
                    }
                    return switch (stockStatus) {
                        case "HetHang" -> "COALESCE(i.TongTon, 0) <= 0";
                        case "ConHang" -> "COALESCE(i.TongTon, 0) > 0";
                        case "SapHetHan" -> "COALESCE(i.LoSapHetHan, 0) > 0";
                        case "HetHan" -> "COALESCE(i.LoHetHan, 0) > 0";
                        default -> "";
                    };
                }

                private void appendMedicineSearch(
                        StringBuilder where, List<Object> parameters, String search) {
                    if (search == null || search.isBlank()) {
                        return;
                    }
                    where.append("""
                             AND (m.MaThuoc LIKE ? ESCAPE '\\'
                                  OR m.TenThuoc LIKE ? ESCAPE '\\'
                                  OR m.HoatChat LIKE ? ESCAPE '\\'
                                  OR m.HamLuong LIKE ? ESCAPE '\\')
                            """);
                    String pattern = "%" + escapeLike(search.trim()) + "%";
                    for (int i = 0; i < 4; i++) {
                        parameters.add(pattern);
                    }
                }

                private void appendBatchFilters(
                        StringBuilder where,
                        List<Object> parameters,
                        BatchFilter filter,
                        int expiryWarningDays) {
                    appendBatchSearch(where, parameters, filter.searchText());
                    if (filter.medicineId() != null && !filter.medicineId().isBlank()) {
                        where.append(" AND l.MaThuoc = ?");
                        parameters.add(filter.medicineId());
                    }
                    if (filter.status() != null && !filter.status().isBlank()) {
                        where.append("""
                                 AND CASE
                                       WHEN l.TrangThai = N'BiKhoa' THEN N'BiKhoa'
                                       WHEN l.HanSuDung < CAST(SYSDATETIME() AS date) THEN N'HetHan'
                                       WHEN l.SoLuongTon <= 0 THEN N'HetHang'
                                       WHEN l.HanSuDung < DATEADD(day, ?, CAST(SYSDATETIME() AS date))
                                            THEN N'SapHetHan'
                                       ELSE l.TrangThai
                                     END = ?
                                """);
                        parameters.add(expiryWarningDays);
                        parameters.add(filter.status());
                    }
                    if (filter.expiryFrom() != null) {
                        where.append(" AND l.HanSuDung >= ?");
                        parameters.add(Date.valueOf(filter.expiryFrom()));
                    }
                    if (filter.expiryTo() != null) {
                        where.append(" AND l.HanSuDung < ?");
                        parameters.add(Date.valueOf(filter.expiryTo().plusDays(1)));
                    }
                }

                private void appendBatchSearch(
                        StringBuilder where, List<Object> parameters, String search) {
                    if (search == null || search.isBlank()) {
                        return;
                    }
                    where.append("""
                             AND (l.MaThuoc LIKE ? ESCAPE '\\'
                                  OR t.TenThuoc LIKE ? ESCAPE '\\'
                                  OR l.SoLo LIKE ? ESCAPE '\\')
                            """);
                    String pattern = "%" + escapeLike(search.trim()) + "%";
                    for (int i = 0; i < 3; i++) {
                        parameters.add(pattern);
                    }
                }

                private void appendInventorySearch(
                        StringBuilder where, List<Object> parameters, String search) {
                    if (search == null || search.isBlank()) {
                        return;
                    }
                    where.append("""
                             AND (t.MaThuoc LIKE ? ESCAPE '\\'
                                  OR t.TenThuoc LIKE ? ESCAPE '\\'
                                  OR t.HoatChat LIKE ? ESCAPE '\\')
                            """);
                    String pattern = "%" + escapeLike(search.trim()) + "%";
                    for (int i = 0; i < 3; i++) {
                        parameters.add(pattern);
                    }
                }

                private Medicine mapMedicine(ResultSet result) throws SQLException {
                    return new Medicine(
                            result.getString("MaThuoc"),
                            result.getString("TenThuoc"),
                            result.getString("HoatChat"),
                            result.getString("HamLuong"),
                            result.getString("DangBaoChe"),
                            result.getString("DonViTinh"),
                            result.getString("DuongDungMacDinh"),
                            result.getString("HangSanXuat"),
                            result.getString("NuocSanXuat"),
                            result.getString("MoTa"),
                            result.getString("TrangThai"));
                }

                private Batch mapBatch(ResultSet result) throws SQLException {
                    return new Batch(
                            result.getLong("MaLoThuoc"),
                            result.getString("MaThuoc"),
                            result.getString("TenThuoc"),
                            result.getString("SoLo"),
                            localDate(result, "NgaySanXuat"),
                            localDate(result, "HanSuDung"),
                            result.getBigDecimal("DonGiaNhap"),
                            result.getBigDecimal("SoLuongTon"),
                            result.getString("ViTriLuuTru"),
                            result.getString("TrangThaiHienTai"),
                            result.getBigDecimal("SoLuongNhap"),
                            result.getBigDecimal("SoLuongXuat"));
                }

                private Receipt mapReceipt(ResultSet result, boolean inbound) throws SQLException {
                    long prescriptionId = result.getLong("MaDonThuoc");
                    Long nullablePrescriptionId = result.wasNull() ? null : prescriptionId;
                    Timestamp timestamp = result.getTimestamp("NgayGiaoDich");
                    return new Receipt(
                            result.getLong("MaPhieu"),
                            result.getString("SoPhieu"),
                            timestamp == null ? null : timestamp.toLocalDateTime(),
                            result.getString("MaNhanVien"),
                            result.getString("TenNhanVien"),
                            result.getString("DoiTac"),
                            inbound ? "Nhap" : "Xuat",
                            nullablePrescriptionId,
                            result.getString("TrangThai"),
                            result.getBigDecimal("TongTien"),
                            result.getLong("SoDong"),
                            result.getLong("SoDongHoaChat"));
                }

                private LockedLot mapLockedLot(ResultSet result) throws SQLException {
                    return new LockedLot(
                            result.getLong("MaLoThuoc"),
                            result.getString("MaThuoc"),
                            result.getString("SoLo"),
                            localDate(result, "HanSuDung"),
                            result.getBigDecimal("SoLuongTon"),
                            result.getBigDecimal("DonGiaNhap"),
                            result.getString("ViTriLuuTru"));
                }

                private StockMovement mapMovement(ResultSet result) throws SQLException {
                    Timestamp timestamp = result.getTimestamp("NgayGiaoDich");
                    return new StockMovement(
                            timestamp == null ? null : timestamp.toLocalDateTime(),
                            result.getString("LoaiGiaoDich"),
                            result.getString("SoPhieu"),
                            result.getString("TrangThai"),
                            result.getLong("MaLoThuoc"),
                            result.getString("SoLo"),
                            result.getString("MaThuoc"),
                            result.getString("TenThuoc"),
                            result.getBigDecimal("SoLuong"),
                            result.getBigDecimal("DonGia"),
                            result.getString("NhanVien"),
                            result.getString("GhiChu"));
                }

                private ExportCandidate mapExportCandidate(ResultSet result) throws SQLException {
                    Timestamp timestamp = result.getTimestamp("NgayKe");
                    return new ExportCandidate(
                            result.getLong("MaDonThuoc"),
                            result.getLong("MaLuotKham"),
                            result.getString("MaBenhNhan"),
                            result.getString("TenBenhNhan"),
                            result.getString("TenBacSi"),
                            timestamp == null ? null : timestamp.toLocalDateTime(),
                            result.getString("TrangThai"),
                            result.getLong("SoLoaiThuoc"));
                }

                private long count(String sql, List<Object> parameters, String operation) {
                    try (Connection connection = DatabaseConnection.getConnection();
                         PreparedStatement statement = connection.prepareStatement(sql)) {
                        bind(statement, parameters);
                        try (ResultSet result = statement.executeQuery()) {
                            if (!result.next()) {
                                throw new SQLException("SQL Server không trả về số lượng bản ghi.");
                            }
                            return result.getLong(1);
                        }
                    } catch (SQLException exception) {
                        throw databaseError(operation, exception);
                    }
                }

                private List<Object> withPaging(List<Object> parameters, int page, int pageSize) {
                    List<Object> queryParameters = new ArrayList<>(parameters);
                    queryParameters.add((long) page * pageSize);
                    queryParameters.add(pageSize);
                    return queryParameters;
                }

                private void bind(PreparedStatement statement, List<?> parameters)
                        throws SQLException {
                    for (int i = 0; i < parameters.size(); i++) {
                        statement.setObject(i + 1, parameters.get(i));
                    }
                }

                private void bindMedicine(PreparedStatement statement, MedicineDraft draft)
                        throws SQLException {
                    statement.setString(1, draft.id());
                    statement.setNString(2, draft.name());
                    setNString(statement, 3, draft.activeIngredient());
                    setNString(statement, 4, draft.strength());
                    setNString(statement, 5, draft.dosageForm());
                    setNString(statement, 6, draft.unit());
                    setNString(statement, 7, draft.defaultRoute());
                    setNString(statement, 8, draft.manufacturer());
                    setNString(statement, 9, draft.country());
                    setNString(statement, 10, draft.description());
                    statement.setNString(11, draft.status());
                }

                private void bindMedicineUpdate(PreparedStatement statement, MedicineDraft draft)
                        throws SQLException {
                    statement.setNString(1, draft.name());
                    setNString(statement, 2, draft.activeIngredient());
                    setNString(statement, 3, draft.strength());
                    setNString(statement, 4, draft.dosageForm());
                    setNString(statement, 5, draft.unit());
                    setNString(statement, 6, draft.defaultRoute());
                    setNString(statement, 7, draft.manufacturer());
                    setNString(statement, 8, draft.country());
                    setNString(statement, 9, draft.description());
                    statement.setNString(10, draft.status());
                    statement.setString(11, draft.id());
                }

                private void setNString(PreparedStatement statement, int index, String value)
                        throws SQLException {
                    if (value == null) {
                        statement.setNull(index, Types.NVARCHAR);
                    } else {
                        statement.setNString(index, value);
                    }
                }

                private void setString(PreparedStatement statement, int index, String value)
                        throws SQLException {
                    if (value == null) {
                        statement.setNull(index, Types.NVARCHAR);
                    } else {
                        statement.setNString(index, value);
                    }
                }

                private void setDate(PreparedStatement statement, int index, LocalDate value)
                        throws SQLException {
                    if (value == null) {
                        statement.setNull(index, Types.DATE);
                    } else {
                        statement.setDate(index, Date.valueOf(value));
                    }
                }

                private LocalDate localDate(ResultSet result, String column) throws SQLException {
                    Date value = result.getDate(column);
                    return value == null ? null : value.toLocalDate();
                }

                private boolean sameDate(LocalDate left, LocalDate right) {
                    return left == null ? right == null : left.equals(right);
                }

                private BigDecimal zeroIfNull(BigDecimal value) {
                    return value == null ? BigDecimal.ZERO : value;
                }

                private String placeholders(int count) {
                    return String.join(", ", java.util.Collections.nCopies(count, "?"));
                }

                private String escapeLike(String value) {
                    return value.replace("\\", "\\\\")
                            .replace("%", "\\%")
                            .replace("_", "\\_")
                            .replace("[", "\\[");
                }

                private String createReceiptNumber(String prefix) {
                    return prefix + "-" + LocalDateTime.now().format(RECEIPT_STAMP)
                            + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                }

                private void rollback(Connection connection, Exception original) {
                    try {
                        connection.rollback();
                    } catch (SQLException rollbackException) {
                        original.addSuppressed(rollbackException);
                    }
                }

                private WarehouseDAOException databaseError(String operation, SQLException exception) {
                    String message = "Lỗi SQL Server tại " + operation
                            + " (SQLState=" + exception.getSQLState()
                            + ", mã lỗi=" + exception.getErrorCode() + "): "
                            + exception.getMessage();
                    return new WarehouseDAOException(message, exception);
                }

                private record PrescriptionLine(
                        long lineId, String medicineId, String medicineName, BigDecimal quantity) {
                }

                private record LockedLot(
                        long id,
                        String medicineId,
                        String batchNumber,
                        LocalDate expiryDate,
                        BigDecimal stock,
                        BigDecimal unitCost,
                        String location) {
                }

                private record LockedBatch(
                        long id,
                        BigDecimal stock,
                        BigDecimal unitCost,
                        String location,
                        boolean created) {
                }

                private record LockedPrescription(long id, String patientName) {
                }

                private record LockedAllocation(
                        PrescriptionLine prescriptionLine, LockedLot lot, BigDecimal quantity) {
                }
}

