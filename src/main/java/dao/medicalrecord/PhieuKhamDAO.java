package dao.medicalrecord;

import config.DatabaseConnection;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import model.appointment.AppointmentDoctor;
import model.appointment.AppointmentSpecialty;
import model.medicalrecord.EligibleVisit;
import model.medicalrecord.MedicalRecordDetail;
import model.medicalrecord.MedicalRecordDraft;
import model.medicalrecord.MedicalRecordFilter;
import model.medicalrecord.MedicalRecordListItem;
import model.medicalrecord.MedicalRecordPage;
import model.medicalrecord.MedicalRecordStats;

public final class PhieuKhamDAO {
    private static final String RECORD_FROM = """
            FROM dbo.PhieuKham pk
            JOIN dbo.LuotKham lv ON lv.MaLuotKham = pk.MaLuotKham
            JOIN dbo.BenhNhan bn ON bn.MaBenhNhan = lv.MaBenhNhan
            JOIN dbo.BacSi bs ON bs.MaNhanVien = pk.MaNhanVien
            JOIN dbo.NhanVien nv ON nv.MaNhanVien = bs.MaNhanVien
            LEFT JOIN dbo.ChuyenKhoa ck ON ck.MaChuyenKhoa = bs.MaChuyenKhoa
            """;

    public MedicalRecordStats getStats(String employeeId, boolean allowAll) {
        String sql = """
                SELECT COUNT_BIG(*) AS TotalRecords,
                       COALESCE(SUM(CASE
                           WHEN ThoiGianLap >= CAST(SYSDATETIME() AS date)
                            AND ThoiGianLap < DATEADD(day, 1, CAST(SYSDATETIME() AS date))
                           THEN 1 ELSE 0 END), 0) AS CreatedToday,
                       COALESCE(SUM(CASE WHEN TrangThai = N'Nhap' THEN 1 ELSE 0 END), 0) AS Drafts,
                       COALESCE(SUM(CASE WHEN TrangThai IN (N'HoanTat', N'DaKy', N'DaKhoa')
                                         THEN 1 ELSE 0 END), 0) AS Completed
                FROM dbo.PhieuKham
                WHERE (? = 1 OR MaNhanVien = ?)
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, allowAll ? 1 : 0);
            statement.setString(2, employeeId);
            try (ResultSet result = statement.executeQuery()) {
            if (!result.next()) {
                throw new SQLException("SQL Server không trả về thống kê phiếu khám.");
            }
            return new MedicalRecordStats(
                    result.getLong("TotalRecords"),
                    result.getLong("CreatedToday"),
                    result.getLong("Drafts"),
                    result.getLong("Completed"));
            }
        } catch (SQLException exception) {
            throw databaseError("getStats", exception);
        }
    }

    public MedicalRecordPage findPage(
            MedicalRecordFilter filter, String employeeId, boolean allowAll) {
        Query query = buildQuery(filter);
        List<Object> parameters = new ArrayList<>(query.parameters());
        parameters.add(allowAll ? 1 : 0);
        parameters.add(employeeId);
        String from = RECORD_FROM + query.whereClause()
                + " AND (? = 1 OR pk.MaNhanVien = ?)";
        try (Connection connection = DatabaseConnection.getConnection()) {
            long total;
            try (PreparedStatement count = connection.prepareStatement(
                    "SELECT COUNT_BIG(*) " + from)) {
                bind(count, parameters, 1);
                try (ResultSet result = count.executeQuery()) {
                    if (!result.next()) {
                        throw new SQLException("SQL Server không trả về tổng số phiếu khám.");
                    }
                    total = result.getLong(1);
                }
            }

            String sql = """
                    SELECT pk.MaPhieuKham, pk.MaLuotKham, lv.MaBenhNhan,
                           bn.HoTen AS TenBenhNhan, nv.HoTen AS TenBacSi,
                           pk.MaNhanVien, bs.MaChuyenKhoa, ck.TenChuyenKhoa,
                           pk.ThoiGianLap, pk.TrangThai, lv.TrangThai AS TrangThaiLuotKham
                    """ + from + """
                     ORDER BY pk.ThoiGianLap DESC, pk.MaPhieuKham DESC
                     OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                    """;
            List<MedicalRecordListItem> records = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, parameters, 1);
                int parameter = parameters.size() + 1;
                statement.setLong(parameter++, (long) filter.page() * filter.pageSize());
                statement.setInt(parameter, filter.pageSize());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        String state = result.getString("TrangThai");
                        String visitState = result.getString("TrangThaiLuotKham");
                        records.add(new MedicalRecordListItem(
                                result.getLong("MaPhieuKham"),
                                result.getLong("MaLuotKham"),
                                result.getString("MaBenhNhan"),
                                result.getString("TenBenhNhan"),
                                result.getString("TenBacSi"),
                                result.getString("MaNhanVien"),
                                result.getString("MaChuyenKhoa"),
                                result.getString("TenChuyenKhoa"),
                                localDateTime(result, "ThoiGianLap"),
                                state,
                                "Nhap".equals(state)
                                        && ("DangKham".equals(visitState)
                                        || "ChoKetQua".equals(visitState))));
                    }
                }
            }
            return new MedicalRecordPage(records, total, filter.page(), filter.pageSize());
        } catch (SQLException exception) {
            throw databaseError("findPage", exception);
        }
    }

    public List<String> findStatuses() {
        String sql = "SELECT DISTINCT TrangThai FROM dbo.PhieuKham ORDER BY TrangThai";
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
                SELECT DISTINCT bs.MaNhanVien, nv.HoTen, bs.MaChuyenKhoa, ck.TenChuyenKhoa
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

    public List<EligibleVisit> findEligibleVisits(String employeeId, boolean allowAll) {
        String sql = """
                SELECT lv.MaLuotKham, lv.MaBenhNhan, bn.HoTen AS TenBenhNhan,
                       bn.SoDienThoai, lv.MaNhanVien, nv.HoTen AS TenBacSi,
                       bs.MaChuyenKhoa, ck.TenChuyenKhoa, lv.ThoiGianTiepNhan, lv.TrangThai
                FROM dbo.LuotKham lv
                JOIN dbo.BenhNhan bn ON bn.MaBenhNhan = lv.MaBenhNhan
                JOIN dbo.BacSi bs ON bs.MaNhanVien = lv.MaNhanVien
                JOIN dbo.NhanVien nv ON nv.MaNhanVien = bs.MaNhanVien
                LEFT JOIN dbo.ChuyenKhoa ck ON ck.MaChuyenKhoa = bs.MaChuyenKhoa
                WHERE lv.TrangThai IN (N'DangKham', N'ChoKetQua')
                  AND (? = 1 OR lv.MaNhanVien = ?)
                  AND NOT EXISTS (
                      SELECT 1 FROM dbo.PhieuKham pk
                      WHERE pk.MaLuotKham = lv.MaLuotKham)
                ORDER BY lv.ThoiGianTiepNhan, lv.MaLuotKham
                """;
        List<EligibleVisit> visits = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, allowAll ? 1 : 0);
            statement.setString(2, employeeId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    visits.add(new EligibleVisit(
                            result.getLong("MaLuotKham"),
                            result.getString("MaBenhNhan"),
                            result.getString("TenBenhNhan"),
                            result.getString("SoDienThoai"),
                            result.getString("MaNhanVien"),
                            result.getString("TenBacSi"),
                            result.getString("MaChuyenKhoa"),
                            result.getString("TenChuyenKhoa"),
                            localDateTime(result, "ThoiGianTiepNhan"),
                            result.getString("TrangThai")));
                }
            }
            return List.copyOf(visits);
        } catch (SQLException exception) {
            throw databaseError("findEligibleVisits", exception);
        }
    }

    public MedicalRecordDetail findById(long recordId) {
        String sql = """
                SELECT pk.MaPhieuKham, pk.MaLuotKham, pk.TrangThai AS TrangThaiPhieu,
                       pk.ThoiGianLap, pk.ThoiGianKy, pk.LyDoKham, pk.TrieuChung,
                       pk.DauHieuSinhTon, pk.KetQuaLamSang, pk.ChanDoan, pk.HuongDieuTri,
                       lv.MaBenhNhan, bn.HoTen AS TenBenhNhan, bn.NgaySinh,
                       bn.GioiTinh, bn.SoDienThoai, bn.DiaChi, lv.ThoiGianTiepNhan,
                       lv.TrangThai AS TrangThaiLuotKham, bs.MaNhanVien,
                       nv.HoTen AS TenBacSi, bs.MaChuyenKhoa, ck.TenChuyenKhoa
                """ + RECORD_FROM + """
                WHERE pk.MaPhieuKham = ?
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, recordId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? mapDetail(result) : null;
            }
        } catch (SQLException exception) {
            throw databaseError("findById", exception);
        }
    }

    public long create(
            MedicalRecordDraft draft,
            String employeeId,
            long accountId,
            boolean allowAll) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String doctorId = lockEligibleVisit(
                        connection, draft.maLuotKham(), employeeId, allowAll);
                String sql = """
                        INSERT INTO dbo.PhieuKham
                            (MaLuotKham, MaNhanVien, LyDoKham, TrieuChung,
                             DauHieuSinhTon, KetQuaLamSang, ChanDoan,
                             HuongDieuTri, TrangThai, ThoiGianLap)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, N'Nhap', SYSDATETIME())
                        """;
                long recordId;
                try (PreparedStatement statement = connection.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setLong(1, draft.maLuotKham());
                    statement.setString(2, doctorId);
                    bindDraft(statement, draft, 3);
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("SQL Server không trả về mã phiếu khám mới.");
                        }
                        recordId = keys.getLong(1);
                    }
                }
                recordAudit(connection, accountId, "TAO_PHIEU_KHAM", recordId);
                connection.commit();
                return recordId;
            } catch (MedicalRecordDAOException exception) {
                rollback(connection, exception);
                throw exception;
            } catch (SQLException exception) {
                rollback(connection, exception);
                if (exception.getErrorCode() == 2601 || exception.getErrorCode() == 2627) {
                    throw new MedicalRecordDAOException(
                            "Lượt khám này đã có phiếu khám. Hãy làm mới danh sách.", exception);
                }
                throw databaseError("create", exception);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw databaseError("create", exception);
        }
    }

    public void update(
            long recordId,
            MedicalRecordDraft draft,
            String employeeId,
            long accountId,
            boolean allowAll) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                RecordLock record = lockEditableRecord(
                        connection, recordId, employeeId, allowAll);
                if (record.visitId() != draft.maLuotKham()) {
                    throw new MedicalRecordDAOException(
                            "Không được thay đổi lượt khám liên kết với phiếu khám.");
                }
                String sql = """
                        UPDATE dbo.PhieuKham
                        SET LyDoKham = ?, TrieuChung = ?, DauHieuSinhTon = ?,
                            KetQuaLamSang = ?, ChanDoan = ?, HuongDieuTri = ?
                        WHERE MaPhieuKham = ? AND TrangThai = N'Nhap'
                        """;
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    bindDraft(statement, draft, 1);
                    statement.setLong(7, recordId);
                    if (statement.executeUpdate() != 1) {
                        throw new MedicalRecordDAOException(
                                "Phiếu khám đã thay đổi hoặc không còn ở trạng thái nháp.");
                    }
                }
                recordAudit(connection, accountId, "CAP_NHAT_PHIEU_KHAM", recordId);
                connection.commit();
            } catch (MedicalRecordDAOException exception) {
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

    private String lockEligibleVisit(
            Connection connection, long visitId, String employeeId, boolean allowAll)
            throws SQLException {
        String sql = """
                SELECT lv.MaNhanVien
                FROM dbo.LuotKham lv WITH (UPDLOCK, HOLDLOCK)
                JOIN dbo.BenhNhan bn ON bn.MaBenhNhan = lv.MaBenhNhan
                JOIN dbo.BacSi bs ON bs.MaNhanVien = lv.MaNhanVien
                WHERE lv.MaLuotKham = ?
                  AND lv.TrangThai IN (N'DangKham', N'ChoKetQua')
                  AND (? = 1 OR lv.MaNhanVien = ?)
                  AND NOT EXISTS (
                      SELECT 1 FROM dbo.PhieuKham pk WITH (UPDLOCK, HOLDLOCK)
                      WHERE pk.MaLuotKham = lv.MaLuotKham)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, visitId);
            statement.setInt(2, allowAll ? 1 : 0);
            statement.setString(3, employeeId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new MedicalRecordDAOException(
                            "Không tìm thấy lượt khám đủ điều kiện, không thuộc quyền phụ trách, "
                            + "hoặc lượt khám đã có phiếu.");
                }
                return result.getString("MaNhanVien");
            }
        }
    }

    private RecordLock lockEditableRecord(
            Connection connection, long recordId, String employeeId, boolean allowAll)
            throws SQLException {
        String sql = """
                SELECT pk.MaLuotKham, pk.TrangThai AS TrangThaiPhieu,
                       pk.MaNhanVien AS BacSiPhieu, lv.TrangThai AS TrangThaiLuot,
                       lv.MaNhanVien AS BacSiLuot
                FROM dbo.PhieuKham pk WITH (UPDLOCK, HOLDLOCK)
                JOIN dbo.LuotKham lv WITH (UPDLOCK, HOLDLOCK)
                    ON lv.MaLuotKham = pk.MaLuotKham
                WHERE pk.MaPhieuKham = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, recordId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new MedicalRecordDAOException("Không tìm thấy phiếu khám.");
                }
                long visitId = result.getLong("MaLuotKham");
                String recordStatus = result.getString("TrangThaiPhieu");
                String visitStatus = result.getString("TrangThaiLuot");
                String recordDoctor = result.getString("BacSiPhieu");
                String visitDoctor = result.getString("BacSiLuot");
                if (!"Nhap".equals(recordStatus)) {
                    throw new MedicalRecordDAOException(
                            "Phiếu khám đã hoàn tất/ký/khóa và không được chỉnh sửa.");
                }
                if (!"DangKham".equals(visitStatus) && !"ChoKetQua".equals(visitStatus)) {
                    throw new MedicalRecordDAOException(
                            "Lượt khám không ở trạng thái cho phép cập nhật phiếu.");
                }
                if (!recordDoctor.equals(visitDoctor)) {
                    throw new MedicalRecordDAOException(
                            "Bác sĩ trên phiếu không khớp bác sĩ phụ trách lượt khám.");
                }
                if (!allowAll && !employeeId.equals(recordDoctor)) {
                    throw new MedicalRecordDAOException(
                            "Bác sĩ chỉ được cập nhật phiếu khám của lượt do mình phụ trách.");
                }
                return new RecordLock(visitId);
            }
        }
    }

    private void bindDraft(PreparedStatement statement, MedicalRecordDraft draft, int start)
            throws SQLException {
        statement.setNString(start, draft.lyDoKham());
        statement.setNString(start + 1, draft.trieuChung());
        statement.setNString(start + 2, draft.dauHieuSinhTon());
        statement.setNString(start + 3, draft.ketQuaLamSang());
        statement.setNString(start + 4, draft.chanDoan());
        statement.setNString(start + 5, draft.huongDieuTri());
    }

    private void recordAudit(
            Connection connection, long accountId, String action, long recordId)
            throws SQLException {
        String sql = """
                INSERT INTO dbo.NhatKyHeThong
                    (MaTaiKhoan, HanhDong, DoiTuong, MaDoiTuong, NoiDung)
                VALUES (?, ?, N'PhieuKham', ?, N'')
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accountId);
            statement.setNString(2, action);
            statement.setString(3, Long.toString(recordId));
            statement.executeUpdate();
        }
    }

    private Query buildQuery(MedicalRecordFilter filter) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> parameters = new ArrayList<>();
        String search = filter.searchText() == null ? "" : filter.searchText().trim();
        if (!search.isEmpty()) {
            String pattern = "%" + escapeLike(search) + "%";
            where.append("""
                     AND (CONVERT(nvarchar(20), pk.MaPhieuKham) LIKE ? ESCAPE '~'
                       OR CONVERT(nvarchar(20), pk.MaLuotKham) LIKE ? ESCAPE '~'
                       OR lv.MaBenhNhan LIKE ? ESCAPE '~'
                       OR bn.HoTen LIKE ? ESCAPE '~'
                       OR bn.SoDienThoai LIKE ? ESCAPE '~'
                       OR nv.HoTen LIKE ? ESCAPE '~')
                    """);
            for (int i = 0; i < 6; i++) {
                parameters.add(pattern);
            }
        }
        if (filter.fromDate() != null) {
            where.append(" AND pk.ThoiGianLap >= ?");
            parameters.add(filter.fromDate());
        }
        if (filter.toDate() != null) {
            where.append(" AND pk.ThoiGianLap < DATEADD(day, 1, ?)");
            parameters.add(filter.toDate());
        }
        if (filter.status() != null && !filter.status().isBlank()) {
            where.append(" AND pk.TrangThai = ?");
            parameters.add(filter.status());
        }
        if (filter.doctorId() != null && !filter.doctorId().isBlank()) {
            where.append(" AND pk.MaNhanVien = ?");
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

    private MedicalRecordDetail mapDetail(ResultSet result) throws SQLException {
        Date birth = result.getDate("NgaySinh");
        return new MedicalRecordDetail(
                result.getLong("MaPhieuKham"),
                result.getLong("MaLuotKham"),
                result.getString("TrangThaiPhieu"),
                localDateTime(result, "ThoiGianLap"),
                localDateTime(result, "ThoiGianKy"),
                result.getString("LyDoKham"),
                result.getString("TrieuChung"),
                result.getString("DauHieuSinhTon"),
                result.getString("KetQuaLamSang"),
                result.getString("ChanDoan"),
                result.getString("HuongDieuTri"),
                result.getString("MaBenhNhan"),
                result.getString("TenBenhNhan"),
                birth == null ? null : birth.toLocalDate(),
                result.getString("GioiTinh"),
                result.getString("SoDienThoai"),
                result.getString("DiaChi"),
                localDateTime(result, "ThoiGianTiepNhan"),
                result.getString("TrangThaiLuotKham"),
                result.getString("MaNhanVien"),
                result.getString("TenBacSi"),
                result.getString("MaChuyenKhoa"),
                result.getString("TenChuyenKhoa"));
    }

    private LocalDateTime localDateTime(ResultSet result, String column) throws SQLException {
        java.sql.Timestamp value = result.getTimestamp(column);
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

    private MedicalRecordDAOException databaseError(String method, SQLException exception) {
        return new MedicalRecordDAOException(
                "Lỗi SQL trong PhieuKhamDAO." + method
                + " (SQLState=" + exception.getSQLState()
                + ", mã lỗi=" + exception.getErrorCode() + "): "
                + exception.getMessage(),
                exception);
    }

    private record Query(String whereClause, List<Object> parameters) {
    }

    private record RecordLock(long visitId) {
    }
}
