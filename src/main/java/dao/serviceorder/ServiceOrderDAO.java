package dao.serviceorder;

import config.DatabaseConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import model.serviceorder.EligibleOrderVisit;
import model.serviceorder.ServiceCatalogItem;
import model.serviceorder.ServiceOrderDetail;
import model.serviceorder.ServiceOrderDraft;
import model.serviceorder.ServiceOrderFilter;
import model.serviceorder.ServiceOrderListItem;
import model.serviceorder.ServiceOrderPage;
import model.serviceorder.ServiceOrderStats;

public final class ServiceOrderDAO {
    private static final String ORDER_FROM = """
            FROM dbo.ChiDinhDichVu cd
            JOIN dbo.PhieuKham pk ON pk.MaPhieuKham = cd.MaPhieuKham
            JOIN dbo.LuotKham lv ON lv.MaLuotKham = pk.MaLuotKham
            JOIN dbo.BenhNhan bn ON bn.MaBenhNhan = lv.MaBenhNhan
            JOIN dbo.BacSi bs ON bs.MaNhanVien = pk.MaNhanVien
            JOIN dbo.NhanVien nv ON nv.MaNhanVien = bs.MaNhanVien
            JOIN dbo.DichVu dv ON dv.MaDichVu = cd.MaDichVu
            """;

    public ServiceOrderStats getStats(String employeeId, boolean allowAll) {
        String sql = """
                SELECT COUNT_BIG(*) AS TotalOrders,
                       COALESCE(SUM(CASE
                           WHEN cd.ThoiGianChiDinh >= CAST(SYSDATETIME() AS date)
                            AND cd.ThoiGianChiDinh < DATEADD(day, 1, CAST(SYSDATETIME() AS date))
                           THEN 1 ELSE 0 END), 0) AS OrderedToday,
                       COALESCE(SUM(CASE WHEN cd.TrangThai = N'ChoThucHien'
                                         THEN 1 ELSE 0 END), 0) AS PendingOrders,
                       COALESCE(SUM(CASE WHEN cd.TrangThai = N'HoanTat'
                                         THEN 1 ELSE 0 END), 0) AS CompletedOrders
                """ + ORDER_FROM + """
                WHERE (? = 1 OR pk.MaNhanVien = ?)
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, allowAll ? 1 : 0);
            statement.setString(2, employeeId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("SQL Server không trả về thống kê chỉ định.");
                }
                return new ServiceOrderStats(
                        result.getLong("TotalOrders"),
                        result.getLong("OrderedToday"),
                        result.getLong("PendingOrders"),
                        result.getLong("CompletedOrders"));
            }
        } catch (SQLException exception) {
            throw databaseError("getStats", exception);
        }
    }

    public ServiceOrderPage findPage(
            ServiceOrderFilter filter, String employeeId, boolean allowAll) {
        Query query = buildQuery(filter);
        List<Object> parameters = new ArrayList<>(query.parameters());
        parameters.add(allowAll ? 1 : 0);
        parameters.add(employeeId);
        String from = ORDER_FROM + query.whereClause()
                + " AND (? = 1 OR pk.MaNhanVien = ?)";
        try (Connection connection = DatabaseConnection.getConnection()) {
            long total;
            try (PreparedStatement count = connection.prepareStatement(
                    "SELECT COUNT_BIG(*) " + from)) {
                bind(count, parameters);
                try (ResultSet result = count.executeQuery()) {
                    result.next();
                    total = result.getLong(1);
                }
            }

            String sql = """
                    SELECT cd.MaChiDinh, lv.MaLuotKham, pk.MaPhieuKham,
                           bn.MaBenhNhan, bn.HoTen AS TenBenhNhan,
                           dv.MaDichVu, dv.TenDichVu, dv.LoaiDichVu,
                           pk.MaNhanVien AS MaBacSi, nv.HoTen AS TenBacSi,
                           cd.ThoiGianChiDinh, lv.TrangThai AS TrangThaiLuotKham,
                           cd.TrangThai, cd.GhiChu,
                           CASE WHEN EXISTS (
                               SELECT 1 FROM dbo.ChiTietPhieuTinhPhi ctp
                               WHERE ctp.MaChiDinh = cd.MaChiDinh
                           ) THEN 1 ELSE 0 END AS DaTinhPhi
                    """ + from + """
                     ORDER BY cd.ThoiGianChiDinh DESC, cd.MaChiDinh DESC
                     OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                    """;
            List<ServiceOrderListItem> orders = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, parameters);
                int index = parameters.size() + 1;
                statement.setLong(index++, (long) filter.page() * filter.pageSize());
                statement.setInt(index, filter.pageSize());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        orders.add(mapListItem(result));
                    }
                }
            }
            return new ServiceOrderPage(orders, total, filter.page(), filter.pageSize());
        } catch (SQLException exception) {
            throw databaseError("findPage", exception);
        }
    }

    public List<ServiceCatalogItem> findServices(String searchText, String serviceType) {
        StringBuilder sql = new StringBuilder("""
                SELECT dv.MaDichVu, dv.TenDichVu, dv.LoaiDichVu, dv.DonViTinh,
                       dv.MoTa, dv.TrangThai, price.PriceCount, price.DonGia
                FROM dbo.DichVu dv
                OUTER APPLY (
                    SELECT COUNT_BIG(*) AS PriceCount, MIN(ct.DonGia) AS DonGia
                    FROM dbo.ChiTietBangGia ct
                    JOIN dbo.BangGiaDichVu bg ON bg.MaBangGia = ct.MaBangGia
                    WHERE ct.MaDichVu = dv.MaDichVu
                      AND bg.TrangThai = N'DangApDung'
                      AND bg.TuNgay <= CONVERT(date, SYSDATETIME())
                      AND (bg.DenNgay IS NULL OR bg.DenNgay >= CONVERT(date, SYSDATETIME()))
                ) price
                WHERE 1 = 1
                """);
        List<String> parameters = new ArrayList<>();
        if (searchText != null && !searchText.isBlank()) {
            String pattern = "%" + escapeLike(searchText.trim()) + "%";
            sql.append("""
                     AND (dv.MaDichVu LIKE ? ESCAPE '~'
                       OR dv.TenDichVu LIKE ? ESCAPE '~'
                       OR COALESCE(dv.LoaiDichVu, N'') LIKE ? ESCAPE '~')
                    """);
            parameters.add(pattern);
            parameters.add(pattern);
            parameters.add(pattern);
        }
        if (serviceType != null && !serviceType.isBlank()) {
            sql.append(" AND dv.LoaiDichVu = ?");
            parameters.add(serviceType);
        }
        sql.append(" ORDER BY dv.LoaiDichVu, dv.TenDichVu, dv.MaDichVu");

        List<ServiceCatalogItem> services = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                statement.setNString(i + 1, parameters.get(i));
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    long priceCount = result.getLong("PriceCount");
                    if (priceCount > 1) {
                        throw new ServiceOrderDAOException(
                                "Dịch vụ " + result.getString("MaDichVu")
                                + " có nhiều bảng giá đang hiệu lực; cần quản trị dữ liệu bảng giá.");
                    }
                    BigDecimal price = result.getBigDecimal("DonGia");
                    services.add(new ServiceCatalogItem(
                            result.getString("MaDichVu"),
                            result.getString("TenDichVu"),
                            result.getString("LoaiDichVu"),
                            result.getString("DonViTinh"),
                            result.getString("MoTa"),
                            result.getString("TrangThai"),
                            price));
                }
            }
            return List.copyOf(services);
        } catch (SQLException exception) {
            throw databaseError("findServices", exception);
        }
    }

    public List<String> findServiceTypes() {
        String sql = """
                SELECT DISTINCT LoaiDichVu
                FROM dbo.DichVu
                WHERE LoaiDichVu IS NOT NULL AND LTRIM(RTRIM(LoaiDichVu)) <> N''
                ORDER BY LoaiDichVu
                """;
        List<String> types = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                types.add(result.getString(1));
            }
            return List.copyOf(types);
        } catch (SQLException exception) {
            throw databaseError("findServiceTypes", exception);
        }
    }

    public List<String> findOrderStatuses() {
        return List.of("ChoThucHien", "DangThucHien", "HoanTat", "DaHuy");
    }

    public List<EligibleOrderVisit> findEligibleVisits(String employeeId, boolean allowAll) {
        String sql = """
                SELECT lv.MaLuotKham, pk.MaPhieuKham, pk.TrangThai AS TrangThaiPhieuKham,
                       lv.MaBenhNhan, bn.HoTen AS TenBenhNhan, bn.GioiTinh, bn.NgaySinh,
                       lv.MaNhanVien, nv.HoTen AS TenBacSi, ck.TenChuyenKhoa,
                       lv.ThoiGianTiepNhan, lv.TrangThai AS TrangThaiLuotKham
                FROM dbo.LuotKham lv
                JOIN dbo.PhieuKham pk ON pk.MaLuotKham = lv.MaLuotKham
                JOIN dbo.BenhNhan bn ON bn.MaBenhNhan = lv.MaBenhNhan
                JOIN dbo.BacSi bs ON bs.MaNhanVien = lv.MaNhanVien
                JOIN dbo.NhanVien nv ON nv.MaNhanVien = bs.MaNhanVien
                LEFT JOIN dbo.ChuyenKhoa ck ON ck.MaChuyenKhoa = bs.MaChuyenKhoa
                WHERE lv.TrangThai IN (N'DangKham', N'ChoKetQua')
                  AND pk.MaNhanVien = lv.MaNhanVien
                  AND (? = 1 OR lv.MaNhanVien = ?)
                ORDER BY lv.ThoiGianTiepNhan DESC, lv.MaLuotKham DESC
                """;
        List<EligibleOrderVisit> visits = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, allowAll ? 1 : 0);
            statement.setString(2, employeeId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Date birth = result.getDate("NgaySinh");
                    visits.add(new EligibleOrderVisit(
                            result.getLong("MaLuotKham"),
                            result.getLong("MaPhieuKham"),
                            result.getString("TrangThaiPhieuKham"),
                            result.getString("MaBenhNhan"),
                            result.getString("TenBenhNhan"),
                            result.getString("GioiTinh"),
                            birth == null ? null : birth.toLocalDate(),
                            result.getString("MaNhanVien"),
                            result.getString("TenBacSi"),
                            result.getString("TenChuyenKhoa"),
                            localDateTime(result, "ThoiGianTiepNhan"),
                            result.getString("TrangThaiLuotKham")));
                }
            }
            return List.copyOf(visits);
        } catch (SQLException exception) {
            throw databaseError("findEligibleVisits", exception);
        }
    }

    public ServiceOrderDetail findById(long orderId, String employeeId, boolean allowAll) {
        String sql = detailSql() + """
                WHERE cd.MaChiDinh = ? AND (? = 1 OR pk.MaNhanVien = ?)
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            statement.setInt(2, allowAll ? 1 : 0);
            statement.setString(3, employeeId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? mapDetail(result) : null;
            }
        } catch (SQLException exception) {
            throw databaseError("findById", exception);
        }
    }

    public List<ServiceOrderListItem> findByVisit(
            long visitId, String employeeId, boolean allowAll) {
        String sql = """
                SELECT cd.MaChiDinh, lv.MaLuotKham, pk.MaPhieuKham,
                       bn.MaBenhNhan, bn.HoTen AS TenBenhNhan,
                       dv.MaDichVu, dv.TenDichVu, dv.LoaiDichVu,
                       pk.MaNhanVien AS MaBacSi, nv.HoTen AS TenBacSi,
                       cd.ThoiGianChiDinh, lv.TrangThai AS TrangThaiLuotKham,
                       cd.TrangThai, cd.GhiChu,
                       CASE WHEN EXISTS (
                           SELECT 1 FROM dbo.ChiTietPhieuTinhPhi ctp
                           WHERE ctp.MaChiDinh = cd.MaChiDinh
                       ) THEN 1 ELSE 0 END AS DaTinhPhi
                """ + ORDER_FROM + """
                WHERE lv.MaLuotKham = ? AND (? = 1 OR pk.MaNhanVien = ?)
                ORDER BY cd.ThoiGianChiDinh DESC, cd.MaChiDinh DESC
                """;
        List<ServiceOrderListItem> orders = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, visitId);
            statement.setInt(2, allowAll ? 1 : 0);
            statement.setString(3, employeeId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    orders.add(mapListItem(result));
                }
            }
            return List.copyOf(orders);
        } catch (SQLException exception) {
            throw databaseError("findByVisit", exception);
        }
    }

    public long create(
            ServiceOrderDraft draft,
            String employeeId,
            long accountId,
            boolean allowAll) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long recordId = verifyEligibleVisit(
                        connection, draft.maLuotKham(), employeeId, allowAll);
                verifyActiveService(connection, draft.maDichVu());
                String sql = """
                        INSERT INTO dbo.ChiDinhDichVu
                            (MaPhieuKham, MaDichVu, MaNhanVienThucHien,
                             ThoiGianChiDinh, TrangThai, GhiChu)
                        VALUES (?, ?, NULL, SYSDATETIME(), N'ChoThucHien', ?)
                        """;
                long orderId;
                try (PreparedStatement statement = connection.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setLong(1, recordId);
                    statement.setString(2, draft.maDichVu());
                    statement.setNString(3, draft.ghiChu());
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("SQL Server không trả về mã chỉ định mới.");
                        }
                        orderId = keys.getLong(1);
                    }
                }
                recordAudit(connection, accountId, "TAO_CHI_DINH_DICH_VU", orderId);
                connection.commit();
                return orderId;
            } catch (ServiceOrderDAOException exception) {
                rollback(connection, exception);
                throw exception;
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw databaseError("create", exception);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw databaseError("create", exception);
        }
    }

    public void update(
            long orderId,
            ServiceOrderDraft draft,
            String employeeId,
            long accountId,
            boolean allowAll) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                LockedOrder order = lockOrder(connection, orderId, employeeId, allowAll);
                requireEditableOrder(connection, orderId, order);
                verifyActiveService(connection, draft.maDichVu());
                String sql = """
                        UPDATE dbo.ChiDinhDichVu
                        SET MaDichVu = ?, GhiChu = ?
                        WHERE MaChiDinh = ? AND TrangThai = N'ChoThucHien'
                        """;
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, draft.maDichVu());
                    statement.setNString(2, draft.ghiChu());
                    statement.setLong(3, orderId);
                    if (statement.executeUpdate() != 1) {
                        throw new ServiceOrderDAOException(
                                "Chỉ định đã thay đổi; hãy tải lại dữ liệu.");
                    }
                }
                recordAudit(connection, accountId, "CAP_NHAT_CHI_DINH_DICH_VU", orderId);
                connection.commit();
            } catch (ServiceOrderDAOException exception) {
                rollback(connection, exception);
                throw exception;
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw databaseError("update", exception);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw databaseError("update", exception);
        }
    }

    public void cancel(
            long orderId,
            String reason,
            String employeeId,
            long accountId,
            boolean allowAll) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                LockedOrder order = lockOrder(connection, orderId, employeeId, allowAll);
                requireEditableOrder(connection, orderId, order);
                String note = order.note() == null || order.note().isBlank()
                        ? "Hủy: " + reason
                        : order.note() + " | Hủy: " + reason;
                if (note.length() > 255) {
                    throw new ServiceOrderDAOException(
                            "Ghi chú hiện tại không đủ chỗ lưu lý do hủy (tối đa 255 ký tự).");
                }
                String sql = """
                        UPDATE dbo.ChiDinhDichVu
                        SET TrangThai = N'DaHuy', GhiChu = ?
                        WHERE MaChiDinh = ? AND TrangThai = N'ChoThucHien'
                        """;
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setNString(1, note);
                    statement.setLong(2, orderId);
                    if (statement.executeUpdate() != 1) {
                        throw new ServiceOrderDAOException(
                                "Chỉ định đã thay đổi; hãy tải lại dữ liệu.");
                    }
                }
                recordAudit(connection, accountId, "HUY_CHI_DINH_DICH_VU", orderId);
                connection.commit();
            } catch (ServiceOrderDAOException exception) {
                rollback(connection, exception);
                throw exception;
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw databaseError("cancel", exception);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw databaseError("cancel", exception);
        }
    }

    private String detailSql() {
        return """
                SELECT cd.MaChiDinh, lv.MaLuotKham, pk.MaPhieuKham,
                       pk.TrangThai AS TrangThaiPhieuKham,
                       lv.TrangThai AS TrangThaiLuotKham,
                       bn.MaBenhNhan, bn.HoTen AS TenBenhNhan, bn.NgaySinh, bn.GioiTinh,
                       pk.MaNhanVien AS MaBacSi, nv.HoTen AS TenBacSi,
                       ck.TenChuyenKhoa, dv.MaDichVu, dv.TenDichVu, dv.LoaiDichVu,
                       dv.DonViTinh, cd.ThoiGianChiDinh, cd.ThoiGianThucHien,
                       cd.TrangThai, cd.MaNhanVienThucHien, nvth.HoTen AS TenNguoiThucHien,
                       cd.KetQua, cd.GhiChu,
                       CASE WHEN EXISTS (
                           SELECT 1 FROM dbo.ChiTietPhieuTinhPhi ctp
                           WHERE ctp.MaChiDinh = cd.MaChiDinh
                       ) THEN 1 ELSE 0 END AS DaTinhPhi
                """ + ORDER_FROM + """
                LEFT JOIN dbo.ChuyenKhoa ck ON ck.MaChuyenKhoa = bs.MaChuyenKhoa
                LEFT JOIN dbo.NhanVien nvth ON nvth.MaNhanVien = cd.MaNhanVienThucHien
                """;
    }

    private long verifyEligibleVisit(
            Connection connection, long visitId, String employeeId, boolean allowAll)
            throws SQLException {
        String sql = """
                SELECT pk.MaPhieuKham, lv.MaNhanVien, lv.TrangThai AS TrangThaiLuot,
                       pk.MaNhanVien AS MaBacSiPhieu
                FROM dbo.LuotKham lv WITH (UPDLOCK, HOLDLOCK)
                JOIN dbo.PhieuKham pk WITH (UPDLOCK, HOLDLOCK)
                    ON pk.MaLuotKham = lv.MaLuotKham
                WHERE lv.MaLuotKham = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, visitId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new ServiceOrderDAOException(
                            "Lượt khám không tồn tại hoặc chưa có phiếu khám.");
                }
                String assignedDoctor = result.getString("MaNhanVien");
                String visitState = result.getString("TrangThaiLuot");
                if (!assignedDoctor.equals(result.getString("MaBacSiPhieu"))) {
                    throw new ServiceOrderDAOException(
                            "Bác sĩ trên phiếu khám không khớp bác sĩ phụ trách lượt khám.");
                }
                if (!"DangKham".equals(visitState) && !"ChoKetQua".equals(visitState)) {
                    throw new ServiceOrderDAOException(
                            "Chỉ được chỉ định dịch vụ cho lượt khám đang khám hoặc chờ kết quả.");
                }
                if (!allowAll && !employeeId.equals(assignedDoctor)) {
                    throw new ServiceOrderDAOException(
                            "Bạn chỉ được chỉ định dịch vụ cho lượt khám do mình phụ trách.");
                }
                return result.getLong("MaPhieuKham");
            }
        }
    }

    private void verifyActiveService(Connection connection, String serviceId)
            throws SQLException {
        String sql = """
                SELECT TrangThai
                FROM dbo.DichVu WITH (UPDLOCK, HOLDLOCK)
                WHERE MaDichVu = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, serviceId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new ServiceOrderDAOException("Không tìm thấy dịch vụ.");
                }
                if (!"DangApDung".equals(result.getString("TrangThai"))) {
                    throw new ServiceOrderDAOException(
                            "Không thể chọn dịch vụ đã ngừng áp dụng.");
                }
            }
        }
    }

    private LockedOrder lockOrder(
            Connection connection, long orderId, String employeeId, boolean allowAll)
            throws SQLException {
        String sql = """
                SELECT cd.MaChiDinh, cd.TrangThai, cd.GhiChu,
                       lv.TrangThai AS TrangThaiLuot, pk.MaNhanVien AS MaBacSiPhieu,
                       lv.MaNhanVien AS MaBacSiLuot,
                       CASE WHEN EXISTS (
                           SELECT 1 FROM dbo.ChiTietPhieuTinhPhi ctp WITH (UPDLOCK, HOLDLOCK)
                           WHERE ctp.MaChiDinh = cd.MaChiDinh
                       ) THEN 1 ELSE 0 END AS DaTinhPhi
                FROM dbo.ChiDinhDichVu cd WITH (UPDLOCK, HOLDLOCK)
                JOIN dbo.PhieuKham pk WITH (UPDLOCK, HOLDLOCK)
                    ON pk.MaPhieuKham = cd.MaPhieuKham
                JOIN dbo.LuotKham lv WITH (UPDLOCK, HOLDLOCK)
                    ON lv.MaLuotKham = pk.MaLuotKham
                WHERE cd.MaChiDinh = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new ServiceOrderDAOException("Không tìm thấy chỉ định dịch vụ.");
                }
                String assignedDoctor = result.getString("MaBacSiPhieu");
                if (!assignedDoctor.equals(result.getString("MaBacSiLuot"))) {
                    throw new ServiceOrderDAOException(
                            "Bác sĩ trên phiếu không khớp bác sĩ phụ trách lượt khám.");
                }
                if (!allowAll && !employeeId.equals(assignedDoctor)) {
                    throw new ServiceOrderDAOException(
                            "Bạn không được phép thay đổi chỉ định của bác sĩ khác.");
                }
                return new LockedOrder(
                        result.getString("TrangThai"),
                        result.getString("TrangThaiLuot"),
                        result.getString("GhiChu"),
                        result.getBoolean("DaTinhPhi"));
            }
        }
    }

    private void requireEditableOrder(
            Connection connection, long orderId, LockedOrder order)
            throws SQLException {
        if (!"ChoThucHien".equals(order.status())) {
            throw new ServiceOrderDAOException(
                    "Chỉ định chỉ được sửa hoặc hủy khi đang ở trạng thái Chờ thực hiện.");
        }
        if (!"DangKham".equals(order.visitStatus())
                && !"ChoKetQua".equals(order.visitStatus())) {
            throw new ServiceOrderDAOException(
                    "Lượt khám không còn ở trạng thái cho phép sửa/hủy chỉ định.");
        }
        if (order.billed() || hasBillingDetail(connection, orderId)) {
            throw new ServiceOrderDAOException(
                    "Chỉ định đã được đưa vào phiếu tính phí, không thể sửa hoặc hủy.");
        }
    }

    private boolean hasBillingDetail(Connection connection, long orderId)
            throws SQLException {
        String sql = """
                SELECT TOP (1) 1 FROM dbo.ChiTietPhieuTinhPhi WITH (UPDLOCK, HOLDLOCK)
                WHERE MaChiDinh = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, orderId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private void recordAudit(
            Connection connection, long accountId, String action, long orderId)
            throws SQLException {
        String sql = """
                INSERT INTO dbo.NhatKyHeThong
                    (MaTaiKhoan, HanhDong, DoiTuong, MaDoiTuong, NoiDung)
                VALUES (?, ?, N'ChiDinhDichVu', ?, N'')
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accountId);
            statement.setNString(2, action);
            statement.setString(3, Long.toString(orderId));
            statement.executeUpdate();
        }
    }

    private ServiceOrderListItem mapListItem(ResultSet result) throws SQLException {
        return new ServiceOrderListItem(
                result.getLong("MaChiDinh"),
                result.getLong("MaLuotKham"),
                result.getLong("MaPhieuKham"),
                result.getString("MaBenhNhan"),
                result.getString("TenBenhNhan"),
                result.getString("MaDichVu"),
                result.getString("TenDichVu"),
                result.getString("LoaiDichVu"),
                result.getString("MaBacSi"),
                result.getString("TenBacSi"),
                localDateTime(result, "ThoiGianChiDinh"),
                result.getString("TrangThaiLuotKham"),
                result.getString("TrangThai"),
                result.getString("GhiChu"),
                result.getBoolean("DaTinhPhi"));
    }

    private ServiceOrderDetail mapDetail(ResultSet result) throws SQLException {
        Date birth = result.getDate("NgaySinh");
        return new ServiceOrderDetail(
                result.getLong("MaChiDinh"),
                result.getLong("MaLuotKham"),
                result.getLong("MaPhieuKham"),
                result.getString("TrangThaiPhieuKham"),
                result.getString("TrangThaiLuotKham"),
                result.getString("MaBenhNhan"),
                result.getString("TenBenhNhan"),
                birth == null ? null : birth.toLocalDate(),
                result.getString("GioiTinh"),
                result.getString("MaBacSi"),
                result.getString("TenBacSi"),
                result.getString("TenChuyenKhoa"),
                result.getString("MaDichVu"),
                result.getString("TenDichVu"),
                result.getString("LoaiDichVu"),
                result.getString("DonViTinh"),
                localDateTime(result, "ThoiGianChiDinh"),
                localDateTime(result, "ThoiGianThucHien"),
                result.getString("TrangThai"),
                result.getString("MaNhanVienThucHien"),
                result.getString("TenNguoiThucHien"),
                result.getString("KetQua"),
                result.getString("GhiChu"),
                result.getBoolean("DaTinhPhi"));
    }

    private Query buildQuery(ServiceOrderFilter filter) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> parameters = new ArrayList<>();
        String search = filter.searchText() == null ? "" : filter.searchText().trim();
        if (!search.isEmpty()) {
            String pattern = "%" + escapeLike(search) + "%";
            where.append("""
                     AND (CONVERT(nvarchar(20), cd.MaChiDinh) LIKE ? ESCAPE '~'
                       OR CONVERT(nvarchar(20), lv.MaLuotKham) LIKE ? ESCAPE '~'
                       OR bn.MaBenhNhan LIKE ? ESCAPE '~'
                       OR bn.HoTen LIKE ? ESCAPE '~'
                       OR dv.MaDichVu LIKE ? ESCAPE '~'
                       OR dv.TenDichVu LIKE ? ESCAPE '~'
                       OR nv.HoTen LIKE ? ESCAPE '~')
                    """);
            for (int i = 0; i < 7; i++) {
                parameters.add(pattern);
            }
        }
        if (filter.fromDate() != null) {
            where.append(" AND cd.ThoiGianChiDinh >= ?");
            parameters.add(filter.fromDate());
        }
        if (filter.toDate() != null) {
            where.append(" AND cd.ThoiGianChiDinh < DATEADD(day, 1, ?)");
            parameters.add(filter.toDate());
        }
        if (filter.serviceType() != null && !filter.serviceType().isBlank()) {
            where.append(" AND dv.LoaiDichVu = ?");
            parameters.add(filter.serviceType());
        }
        if (filter.status() != null && !filter.status().isBlank()) {
            where.append(" AND cd.TrangThai = ?");
            parameters.add(filter.status());
        }
        return new Query(where.toString(), List.copyOf(parameters));
    }

    private void bind(PreparedStatement statement, List<Object> parameters)
            throws SQLException {
        int index = 1;
        for (Object parameter : parameters) {
            if (parameter instanceof LocalDate date) {
                statement.setDate(index++, Date.valueOf(date));
            } else if (parameter instanceof String text) {
                statement.setNString(index++, text);
            } else if (parameter instanceof Integer number) {
                statement.setInt(index++, number);
            } else {
                statement.setObject(index++, parameter);
            }
        }
    }

    private LocalDateTime localDateTime(ResultSet result, String column)
            throws SQLException {
        Timestamp value = result.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private String escapeLike(String value) {
        return value.replace("~", "~~").replace("%", "~%")
                .replace("_", "~_").replace("[", "~[");
    }

    private void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            original.addSuppressed(rollbackException);
        }
    }

    private ServiceOrderDAOException databaseError(String method, SQLException exception) {
        return new ServiceOrderDAOException(
                "Lỗi SQL trong ServiceOrderDAO." + method
                + " (SQLState=" + exception.getSQLState()
                + ", mã lỗi=" + exception.getErrorCode() + "): "
                + exception.getMessage(),
                exception);
    }

    private record Query(String whereClause, List<Object> parameters) {
    }

    private record LockedOrder(
            String status, String visitStatus, String note, boolean billed) {
    }
}
