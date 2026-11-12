package dao.visit;

import config.DatabaseConnection;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import model.appointment.AppointmentDoctor;
import model.appointment.AppointmentSpecialty;
import model.visit.VisitDetail;
import model.visit.VisitFilter;
import model.visit.VisitListItem;
import model.visit.VisitPage;
import model.visit.VisitStats;

public final class LuotKhamDAO {
    private static final String VISIT_FROM = """
            FROM dbo.LuotKham lv
            JOIN dbo.BenhNhan bn ON bn.MaBenhNhan = lv.MaBenhNhan
            JOIN dbo.BacSi bs ON bs.MaNhanVien = lv.MaNhanVien
            JOIN dbo.NhanVien nv ON nv.MaNhanVien = bs.MaNhanVien
            LEFT JOIN dbo.ChuyenKhoa ck ON ck.MaChuyenKhoa = bs.MaChuyenKhoa
            LEFT JOIN dbo.LichHen lh ON lh.MaLichHen = lv.MaLichHen
            """;

    public VisitStats getTodayStats() {
        String sql = """
                SELECT COUNT_BIG(*) AS TotalToday,
                       COALESCE(SUM(CASE WHEN TrangThai = N'ChoKham' THEN 1 ELSE 0 END), 0) AS Waiting,
                       COALESCE(SUM(CASE WHEN TrangThai IN (N'DangKham', N'ChoKetQua')
                                         THEN 1 ELSE 0 END), 0) AS InProgress,
                       COALESCE(SUM(CASE WHEN TrangThai = N'HoanTat' THEN 1 ELSE 0 END), 0) AS Completed
                FROM dbo.LuotKham
                WHERE ThoiGianTiepNhan >= CAST(SYSDATETIME() AS date)
                  AND ThoiGianTiepNhan < DATEADD(day, 1, CAST(SYSDATETIME() AS date))
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            if (!result.next()) {
                throw new SQLException("SQL Server không trả về thống kê lượt khám.");
            }
            return new VisitStats(
                    result.getLong("TotalToday"),
                    result.getLong("Waiting"),
                    result.getLong("InProgress"),
                    result.getLong("Completed"));
        } catch (SQLException exception) {
            throw databaseError("getTodayStats", exception);
        }
    }

    public VisitPage findPage(VisitFilter filter) {
        Query query = buildQuery(filter);
        String from = VISIT_FROM + query.whereClause();
        try (Connection connection = DatabaseConnection.getConnection()) {
            long totalCount;
            try (PreparedStatement count = connection.prepareStatement(
                    "SELECT COUNT_BIG(*) " + from)) {
                bind(count, query.parameters(), 1);
                try (ResultSet result = count.executeQuery()) {
                    if (!result.next()) {
                        throw new SQLException("SQL Server không trả về tổng số lượt khám.");
                    }
                    totalCount = result.getLong(1);
                }
            }

            String sql = """
                    SELECT lv.MaLuotKham, lv.MaBenhNhan, bn.HoTen AS TenBenhNhan,
                           lv.MaNhanVien, nv.HoTen AS TenBacSi, bs.MaChuyenKhoa,
                           ck.TenChuyenKhoa, lv.MaLichHen, lv.ThoiGianTiepNhan,
                           lv.ThoiGianKetThuc, lv.TrangThai
                    """ + from + """
                     ORDER BY lv.ThoiGianTiepNhan DESC, lv.MaLuotKham DESC
                     OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                    """;
            List<VisitListItem> visits = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, query.parameters(), 1);
                int parameter = query.parameters().size() + 1;
                statement.setLong(parameter++, (long) filter.page() * filter.pageSize());
                statement.setInt(parameter, filter.pageSize());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        visits.add(mapVisit(result));
                    }
                }
            }
            return new VisitPage(visits, totalCount, filter.page(), filter.pageSize());
        } catch (SQLException exception) {
            throw databaseError("findPage", exception);
        }
    }

    public List<String> findStatuses() {
        String sql = "SELECT DISTINCT TrangThai FROM dbo.LuotKham ORDER BY TrangThai";
        List<String> statuses = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                statuses.add(result.getString(1));
            }
            return List.copyOf(statuses);
        } catch (SQLException exception) {
            throw databaseError("findStatuses", exception);
        }
    }

    public List<AppointmentDoctor> findDoctors() {
        String sql = """
                SELECT bs.MaNhanVien, nv.HoTen, bs.MaChuyenKhoa, ck.TenChuyenKhoa
                FROM dbo.BacSi bs
                JOIN dbo.NhanVien nv ON nv.MaNhanVien = bs.MaNhanVien
                LEFT JOIN dbo.ChuyenKhoa ck ON ck.MaChuyenKhoa = bs.MaChuyenKhoa
                ORDER BY nv.HoTen, bs.MaNhanVien
                """;
        List<AppointmentDoctor> doctors = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                doctors.add(new AppointmentDoctor(
                        result.getString("MaNhanVien"),
                        result.getString("HoTen"),
                        result.getString("MaChuyenKhoa"),
                        result.getString("TenChuyenKhoa")));
            }
            return List.copyOf(doctors);
        } catch (SQLException exception) {
            throw databaseError("findDoctors", exception);
        }
    }

    public List<AppointmentSpecialty> findSpecialties() {
        String sql = """
                SELECT MaChuyenKhoa, TenChuyenKhoa
                FROM dbo.ChuyenKhoa
                ORDER BY TenChuyenKhoa
                """;
        List<AppointmentSpecialty> specialties = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                specialties.add(new AppointmentSpecialty(
                        result.getString("MaChuyenKhoa"),
                        result.getString("TenChuyenKhoa")));
            }
            return List.copyOf(specialties);
        } catch (SQLException exception) {
            throw databaseError("findSpecialties", exception);
        }
    }

    public VisitDetail findById(long visitId) {
        String sql = """
                SELECT lv.MaLuotKham, lv.TrangThai, lv.ThoiGianTiepNhan,
                       lv.ThoiGianKetThuc, lv.GhiChu, bn.MaBenhNhan,
                       bn.HoTen AS TenBenhNhan, bn.NgaySinh, bn.GioiTinh,
                       bn.SoDienThoai, bn.DiaChi, bs.MaNhanVien, nv.HoTen AS TenBacSi,
                       bs.MaChuyenKhoa, ck.TenChuyenKhoa, lh.MaLichHen,
                       lh.ThoiGianHen, lh.TrangThai AS TrangThaiLichHen,
                       pk.MaPhieuKham, pk.TrangThai AS TrangThaiPhieuKham,
                       (SELECT COUNT_BIG(*)
                        FROM dbo.ChiDinhDichVu cddv
                        WHERE cddv.MaPhieuKham = pk.MaPhieuKham
                          AND cddv.TrangThai IN (N'ChoThucHien', N'DangThucHien'))
                           AS ChiDinhDangXuLy
                """ + VISIT_FROM + """
                LEFT JOIN dbo.PhieuKham pk ON pk.MaLuotKham = lv.MaLuotKham
                WHERE lv.MaLuotKham = ?
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, visitId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? mapDetail(result) : null;
            }
        } catch (SQLException exception) {
            throw databaseError("findById", exception);
        }
    }

    public void updateStatus(
            long visitId,
            String targetStatus,
            String employeeId,
            long accountId,
            boolean administratorOverride) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String currentStatus;
                String assignedDoctorId;
                String lockSql = """
                        SELECT MaNhanVien, TrangThai
                        FROM dbo.LuotKham WITH (UPDLOCK, HOLDLOCK)
                        WHERE MaLuotKham = ?
                        """;
                try (PreparedStatement statement = connection.prepareStatement(lockSql)) {
                    statement.setLong(1, visitId);
                    try (ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new VisitDAOException("Không tìm thấy lượt khám.");
                        }
                        assignedDoctorId = result.getString("MaNhanVien");
                        currentStatus = result.getString("TrangThai");
                    }
                }

                if (!administratorOverride && !employeeId.equals(assignedDoctorId)) {
                    throw new VisitDAOException(
                            "Bác sĩ chỉ được cập nhật trạng thái lượt khám do mình phụ trách.");
                }
                validateTransition(currentStatus, targetStatus);
                if ("HoanTat".equals(targetStatus)) {
                    validateCompletion(connection, visitId, assignedDoctorId);
                }

                String updateSql = "UPDATE dbo.LuotKham SET TrangThai = ?, "
                        + "ThoiGianKetThuc = CASE WHEN ? = N'HoanTat' "
                        + "THEN SYSDATETIME() ELSE ThoiGianKetThuc END "
                        + "WHERE MaLuotKham = ? AND TrangThai = ?";
                try (PreparedStatement statement = connection.prepareStatement(updateSql)) {
                    statement.setNString(1, targetStatus);
                    statement.setNString(2, targetStatus);
                    statement.setLong(3, visitId);
                    statement.setNString(4, currentStatus);
                    if (statement.executeUpdate() != 1) {
                        throw new VisitDAOException(
                                "Trạng thái lượt khám đã thay đổi. Hãy tải lại danh sách.");
                    }
                }
                recordStatusAudit(
                        connection, accountId, visitId, currentStatus, targetStatus);
                connection.commit();
            } catch (VisitDAOException exception) {
                rollback(connection, exception);
                throw exception;
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw databaseError("updateStatus", exception);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw databaseError("updateStatus", exception);
        }
    }

    private void recordStatusAudit(
            Connection connection,
            long accountId,
            long visitId,
            String previousStatus,
            String nextStatus) throws SQLException {
        String sql = """
                INSERT INTO dbo.NhatKyHeThong
                    (MaTaiKhoan, HanhDong, DoiTuong, MaDoiTuong, NoiDung)
                VALUES (?, N'CAP_NHAT_TRANG_THAI', N'LuotKham', ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accountId);
            statement.setString(2, Long.toString(visitId));
            statement.setNString(
                    3, "TrangThaiCu=" + previousStatus + "; TrangThaiMoi=" + nextStatus);
            statement.executeUpdate();
        }
    }

    private void validateCompletion(
            Connection connection, long visitId, String assignedDoctorId) throws SQLException {
        String recordSql = """
                SELECT pk.MaPhieuKham, pk.TrangThai, pk.MaNhanVien
                FROM dbo.PhieuKham pk WITH (UPDLOCK, HOLDLOCK)
                WHERE pk.MaLuotKham = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(recordSql)) {
            statement.setLong(1, visitId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new VisitDAOException(
                            "Chưa thể hoàn tất: lượt khám chưa có phiếu khám.");
                }
                if (!assignedDoctorId.equals(result.getString("MaNhanVien"))) {
                    throw new VisitDAOException(
                            "Chưa thể hoàn tất: phiếu khám không thuộc bác sĩ phụ trách lượt khám.");
                }
                String status = result.getString("TrangThai");
                if (!"HoanTat".equals(status) && !"DaKy".equals(status)
                        && !"DaKhoa".equals(status)) {
                    throw new VisitDAOException(
                            "Chưa thể hoàn tất: phiếu khám chưa hoàn tất hoặc chưa được ký.");
                }
                long recordId = result.getLong("MaPhieuKham");
                String serviceSql = """
                        SELECT TOP (1) 1
                        FROM dbo.ChiDinhDichVu WITH (UPDLOCK, HOLDLOCK)
                        WHERE MaPhieuKham = ?
                          AND TrangThai IN (N'ChoThucHien', N'DangThucHien')
                        """;
                try (PreparedStatement serviceStatement = connection.prepareStatement(serviceSql)) {
                    serviceStatement.setLong(1, recordId);
                    try (ResultSet serviceResult = serviceStatement.executeQuery()) {
                        if (serviceResult.next()) {
                            throw new VisitDAOException(
                                    "Chưa thể hoàn tất: vẫn còn chỉ định dịch vụ đang chờ hoặc đang thực hiện.");
                        }
                    }
                }
            }
        }
    }

    private void validateTransition(String currentStatus, String targetStatus) {
        boolean valid = ("ChoKham".equals(currentStatus) && "DangKham".equals(targetStatus))
                || ("DangKham".equals(currentStatus) && "ChoKetQua".equals(targetStatus))
                || ("ChoKetQua".equals(currentStatus) && "HoanTat".equals(targetStatus));
        if (!valid) {
            throw new VisitDAOException(
                    "Không thể chuyển trạng thái lượt khám từ "
                    + statusLabel(currentStatus) + " sang " + statusLabel(targetStatus) + ".");
        }
    }

    private Query buildQuery(VisitFilter filter) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> parameters = new ArrayList<>();
        String search = filter.searchText() == null ? "" : filter.searchText().trim();
        if (!search.isEmpty()) {
            String pattern = "%" + escapeLike(search) + "%";
            where.append("""
                     AND (CONVERT(nvarchar(20), lv.MaLuotKham) LIKE ? ESCAPE '~'
                       OR lv.MaBenhNhan LIKE ? ESCAPE '~'
                       OR bn.HoTen LIKE ? ESCAPE '~'
                       OR bn.SoDienThoai LIKE ? ESCAPE '~'
                       OR nv.HoTen LIKE ? ESCAPE '~'
                       OR CONVERT(nvarchar(20), lh.MaLichHen) LIKE ? ESCAPE '~')
                    """);
            for (int i = 0; i < 6; i++) {
                parameters.add(pattern);
            }
        }
        if (filter.fromDate() != null) {
            where.append(" AND lv.ThoiGianTiepNhan >= ?");
            parameters.add(filter.fromDate());
        }
        if (filter.toDate() != null) {
            where.append(" AND lv.ThoiGianTiepNhan < DATEADD(day, 1, ?)");
            parameters.add(filter.toDate());
        }
        if (filter.status() != null && !filter.status().isBlank()) {
            where.append(" AND lv.TrangThai = ?");
            parameters.add(filter.status());
        }
        if (filter.doctorId() != null && !filter.doctorId().isBlank()) {
            where.append(" AND lv.MaNhanVien = ?");
            parameters.add(filter.doctorId());
        }
        if (filter.specialtyId() != null && !filter.specialtyId().isBlank()) {
            where.append(" AND bs.MaChuyenKhoa = ?");
            parameters.add(filter.specialtyId());
        }
        return new Query(where.toString(), List.copyOf(parameters));
    }

    private void bind(PreparedStatement statement, List<Object> parameters, int start)
            throws SQLException {
        int index = start;
        for (Object parameter : parameters) {
            if (parameter instanceof LocalDate date) {
                statement.setDate(index++, Date.valueOf(date));
            } else if (parameter instanceof String text) {
                statement.setString(index++, text);
            } else {
                statement.setObject(index++, parameter);
            }
        }
    }

    private VisitListItem mapVisit(ResultSet result) throws SQLException {
        return new VisitListItem(
                result.getLong("MaLuotKham"),
                result.getString("MaBenhNhan"),
                result.getString("TenBenhNhan"),
                result.getString("MaNhanVien"),
                result.getString("TenBacSi"),
                result.getString("MaChuyenKhoa"),
                result.getString("TenChuyenKhoa"),
                nullableLong(result, "MaLichHen"),
                localDateTime(result, "ThoiGianTiepNhan"),
                localDateTime(result, "ThoiGianKetThuc"),
                result.getString("TrangThai"));
    }

    private VisitDetail mapDetail(ResultSet result) throws SQLException {
        Date birthDate = result.getDate("NgaySinh");
        return new VisitDetail(
                result.getLong("MaLuotKham"),
                result.getString("TrangThai"),
                localDateTime(result, "ThoiGianTiepNhan"),
                localDateTime(result, "ThoiGianKetThuc"),
                result.getString("GhiChu"),
                result.getString("MaBenhNhan"),
                result.getString("TenBenhNhan"),
                birthDate == null ? null : birthDate.toLocalDate(),
                result.getString("GioiTinh"),
                result.getString("SoDienThoai"),
                result.getString("DiaChi"),
                result.getString("MaNhanVien"),
                result.getString("TenBacSi"),
                result.getString("MaChuyenKhoa"),
                result.getString("TenChuyenKhoa"),
                nullableLong(result, "MaLichHen"),
                localDateTime(result, "ThoiGianHen"),
                result.getString("TrangThaiLichHen"),
                nullableLong(result, "MaPhieuKham"),
                result.getString("TrangThaiPhieuKham"),
                result.getLong("ChiDinhDangXuLy"));
    }

    private Long nullableLong(ResultSet result, String column) throws SQLException {
        Object value = result.getObject(column);
        return value == null ? null : ((Number) value).longValue();
    }

    private LocalDateTime localDateTime(ResultSet result, String column) throws SQLException {
        java.sql.Timestamp value = result.getTimestamp(column);
        return value == null ? null : value.toLocalDateTime();
    }

    private void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            original.addSuppressed(rollbackException);
        }
    }

    private String escapeLike(String value) {
        return value.replace("~", "~~").replace("%", "~%")
                .replace("_", "~_").replace("[", "~[");
    }

    private String statusLabel(String status) {
        return switch (status == null ? "" : status) {
            case "ChoKham" -> "Chờ khám";
            case "DangKham" -> "Đang khám";
            case "ChoKetQua" -> "Chờ kết quả";
            case "HoanTat" -> "Hoàn tất";
            case "DaHuy" -> "Đã hủy";
            default -> status == null ? "—" : status;
        };
    }

    private VisitDAOException databaseError(String method, SQLException exception) {
        return new VisitDAOException(
                "Lỗi SQL trong LuotKhamDAO." + method
                + " (SQLState=" + exception.getSQLState()
                + ", mã lỗi=" + exception.getErrorCode() + "): "
                + exception.getMessage(),
                exception);
    }

    private record Query(String whereClause, List<Object> parameters) {
    }
}
