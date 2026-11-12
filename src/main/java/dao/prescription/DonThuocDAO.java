package dao.prescription;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import model.appointment.AppointmentDoctor;
import model.appointment.AppointmentSpecialty;
import model.prescription.EligiblePrescriptionVisit;
import model.prescription.MedicationCatalogItem;
import model.prescription.PrescriptionDetail;
import model.prescription.PrescriptionDraft;
import model.prescription.PrescriptionFilter;
import model.prescription.PrescriptionLineDetail;
import model.prescription.PrescriptionLineDraft;
import model.prescription.PrescriptionListItem;
import model.prescription.PrescriptionPage;
import model.prescription.PrescriptionStats;

public final class DonThuocDAO {
    private static final String PRESCRIPTION_FROM = """
            FROM dbo.DonThuoc dt
            JOIN dbo.LuotKham lk ON lk.MaLuotKham = dt.MaLuotKham
            JOIN dbo.BenhNhan bn ON bn.MaBenhNhan = lk.MaBenhNhan
            JOIN dbo.BacSi bs ON bs.MaNhanVien = dt.MaNhanVien
            JOIN dbo.NhanVien nv ON nv.MaNhanVien = bs.MaNhanVien
            LEFT JOIN dbo.ChuyenKhoa ck ON ck.MaChuyenKhoa = bs.MaChuyenKhoa
            """;

    public PrescriptionStats getStats(String employeeId, boolean allowAll) {
        String sql = """
                SELECT COUNT_BIG(*) AS TotalPrescriptions,
                       COALESCE(SUM(CASE
                           WHEN dt.NgayKe >= CAST(SYSDATETIME() AS date)
                            AND dt.NgayKe < DATEADD(day, 1, CAST(SYSDATETIME() AS date))
                           THEN 1 ELSE 0 END), 0) AS CreatedToday,
                       COALESCE(SUM(CASE WHEN dt.TrangThai = N'Nhap'
                                         THEN 1 ELSE 0 END), 0) AS Drafts,
                       COALESCE(SUM(CASE WHEN dt.TrangThai = N'DaKy'
                                         THEN 1 ELSE 0 END), 0) AS Signed,
                       COALESCE(SUM(CASE WHEN dt.TrangThai IN (N'DaGuiKho', N'DaCapPhat')
                                         THEN 1 ELSE 0 END), 0) AS SentOrDispensed
                FROM dbo.DonThuoc dt
                WHERE (? = 1 OR dt.MaNhanVien = ?)
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, allowAll ? 1 : 0);
            statement.setString(2, employeeId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("SQL Server không trả về thống kê đơn thuốc.");
                }
                return new PrescriptionStats(
                        result.getLong("TotalPrescriptions"),
                        result.getLong("CreatedToday"),
                        result.getLong("Drafts"),
                        result.getLong("Signed"),
                        result.getLong("SentOrDispensed"));
            }
        } catch (SQLException exception) {
            throw databaseError("getStats", exception);
        }
    }

    public PrescriptionPage findPage(
            PrescriptionFilter filter, String employeeId, boolean allowAll) {
        Query query = buildQuery(filter);
        List<Object> parameters = new ArrayList<>(query.parameters());
        parameters.add(allowAll ? 1 : 0);
        parameters.add(employeeId);
        String from = PRESCRIPTION_FROM + query.whereClause()
                + " AND (? = 1 OR dt.MaNhanVien = ?)";
        try (Connection connection = DatabaseConnection.getConnection()) {
            long total;
            try (PreparedStatement count = connection.prepareStatement(
                    "SELECT COUNT_BIG(*) " + from)) {
                bind(count, parameters);
                try (ResultSet result = count.executeQuery()) {
                    if (!result.next()) {
                        throw new SQLException("SQL Server không trả về tổng số đơn thuốc.");
                    }
                    total = result.getLong(1);
                }
            }

            String sql = """
                    SELECT dt.MaDonThuoc, dt.MaLuotKham, bn.MaBenhNhan,
                           bn.HoTen AS TenBenhNhan, bn.SoDienThoai,
                           dt.MaNhanVien AS MaBacSi, nv.HoTen AS TenBacSi,
                           bs.MaChuyenKhoa, ck.TenChuyenKhoa, dt.NgayKe, dt.TrangThai,
                           (SELECT COUNT_BIG(*) FROM dbo.ChiTietDonThuoc ct
                            WHERE ct.MaDonThuoc = dt.MaDonThuoc) AS SoLoaiThuoc,
                           CASE WHEN EXISTS (
                               SELECT 1 FROM dbo.PhieuXuat px
                               WHERE px.MaDonThuoc = dt.MaDonThuoc
                           ) OR EXISTS (
                               SELECT 1
                               FROM dbo.ChiTietDonThuoc ct
                               JOIN dbo.ChiTietPhieuXuat cpx
                                   ON cpx.MaChiTietDon = ct.MaChiTietDon
                               WHERE ct.MaDonThuoc = dt.MaDonThuoc
                           ) THEN 1 ELSE 0 END AS DaXuatThuoc
                    """ + from + """
                     ORDER BY dt.NgayKe DESC, dt.MaDonThuoc DESC
                     OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                    """;
            List<PrescriptionListItem> prescriptions = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                bind(statement, parameters);
                int index = parameters.size() + 1;
                statement.setLong(index++, (long) filter.page() * filter.pageSize());
                statement.setInt(index, filter.pageSize());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        prescriptions.add(mapListItem(result));
                    }
                }
            }
            return new PrescriptionPage(
                    prescriptions, total, filter.page(), filter.pageSize());
        } catch (SQLException exception) {
            throw databaseError("findPage", exception);
        }
    }

    public List<String> findStatuses() {
        String sql = "SELECT DISTINCT TrangThai FROM dbo.DonThuoc ORDER BY TrangThai";
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

    public List<MedicationCatalogItem> findMedications(String search) {
        StringBuilder sql = new StringBuilder("""
                SELECT MaThuoc, TenThuoc, HoatChat, HamLuong, DangBaoChe,
                       DonViTinh, DuongDungMacDinh, TrangThai
                FROM dbo.Thuoc
                WHERE 1 = 1
                """);
        List<String> parameters = new ArrayList<>();
        if (search != null && !search.isBlank()) {
            String pattern = "%" + escapeLike(search.trim()) + "%";
            sql.append("""
                     AND (MaThuoc LIKE ? ESCAPE '~'
                       OR TenThuoc LIKE ? ESCAPE '~'
                       OR COALESCE(HoatChat, N'') LIKE ? ESCAPE '~')
                    """);
            parameters.add(pattern);
            parameters.add(pattern);
            parameters.add(pattern);
        }
        sql.append(" ORDER BY TenThuoc, MaThuoc");
        List<MedicationCatalogItem> medications = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                statement.setString(i + 1, parameters.get(i));
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    medications.add(new MedicationCatalogItem(
                            result.getString("MaThuoc"),
                            result.getString("TenThuoc"),
                            result.getString("HoatChat"),
                            result.getString("HamLuong"),
                            result.getString("DangBaoChe"),
                            result.getString("DonViTinh"),
                            result.getString("DuongDungMacDinh"),
                            result.getString("TrangThai")));
                }
            }
            return List.copyOf(medications);
        } catch (SQLException exception) {
            throw databaseError("findMedications", exception);
        }
    }

    public List<EligiblePrescriptionVisit> findEligibleVisits(
            String employeeId, boolean allowAll) {
        String sql = """
                SELECT lk.MaLuotKham, bn.MaBenhNhan, bn.HoTen AS TenBenhNhan,
                       bn.NgaySinh, bn.GioiTinh, bn.SoDienThoai,
                       lk.MaNhanVien AS MaBacSi, nv.HoTen AS TenBacSi,
                       ck.TenChuyenKhoa, lk.ThoiGianTiepNhan,
                       lk.TrangThai AS TrangThaiLuotKham,
                       pk.MaPhieuKham, pk.TrangThai AS TrangThaiPhieuKham,
                       pk.MaNhanVien AS MaBacSiPhieu
                FROM dbo.LuotKham lk
                JOIN dbo.BenhNhan bn ON bn.MaBenhNhan = lk.MaBenhNhan
                JOIN dbo.BacSi bs ON bs.MaNhanVien = lk.MaNhanVien
                JOIN dbo.NhanVien nv ON nv.MaNhanVien = bs.MaNhanVien
                LEFT JOIN dbo.ChuyenKhoa ck ON ck.MaChuyenKhoa = bs.MaChuyenKhoa
                LEFT JOIN dbo.PhieuKham pk ON pk.MaLuotKham = lk.MaLuotKham
                WHERE lk.TrangThai IN (N'DangKham', N'ChoKetQua')
                  AND (? = 1 OR lk.MaNhanVien = ?)
                  AND (pk.MaPhieuKham IS NULL OR pk.MaNhanVien = lk.MaNhanVien)
                ORDER BY lk.ThoiGianTiepNhan DESC, lk.MaLuotKham DESC
                """;
        List<EligiblePrescriptionVisit> visits = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, allowAll ? 1 : 0);
            statement.setString(2, employeeId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Date birth = result.getDate("NgaySinh");
                    long recordId = result.getLong("MaPhieuKham");
                    Long nullableRecordId = result.wasNull() ? null : recordId;
                    visits.add(new EligiblePrescriptionVisit(
                            result.getLong("MaLuotKham"),
                            result.getString("MaBenhNhan"),
                            result.getString("TenBenhNhan"),
                            birth == null ? null : birth.toLocalDate(),
                            result.getString("GioiTinh"),
                            result.getString("SoDienThoai"),
                            result.getString("MaBacSi"),
                            result.getString("TenBacSi"),
                            result.getString("TenChuyenKhoa"),
                            localDateTime(result, "ThoiGianTiepNhan"),
                            result.getString("TrangThaiLuotKham"),
                            nullableRecordId,
                            result.getString("TrangThaiPhieuKham")));
                }
            }
            return List.copyOf(visits);
        } catch (SQLException exception) {
            throw databaseError("findEligibleVisits", exception);
        }
    }

    public PrescriptionDetail findById(
            long prescriptionId, String employeeId, boolean allowAll) {
        String sql = """
                SELECT dt.MaDonThuoc, dt.MaLuotKham, dt.MaNhanVien AS MaBacSi,
                       nv.HoTen AS TenBacSi, ck.TenChuyenKhoa, dt.NgayKe,
                       dt.LoiDan, dt.TrangThai, lk.TrangThai AS TrangThaiLuotKham,
                       bn.MaBenhNhan, bn.HoTen AS TenBenhNhan, bn.NgaySinh,
                       bn.GioiTinh, bn.SoDienThoai, lk.ThoiGianTiepNhan,
                       CASE WHEN EXISTS (
                           SELECT 1 FROM dbo.PhieuXuat px
                           WHERE px.MaDonThuoc = dt.MaDonThuoc
                       ) OR EXISTS (
                           SELECT 1 FROM dbo.ChiTietDonThuoc c
                           JOIN dbo.ChiTietPhieuXuat cpx
                               ON cpx.MaChiTietDon = c.MaChiTietDon
                           WHERE c.MaDonThuoc = dt.MaDonThuoc
                       ) THEN 1 ELSE 0 END AS DaXuatThuoc
                """ + PRESCRIPTION_FROM + """
                WHERE dt.MaDonThuoc = ? AND (? = 1 OR dt.MaNhanVien = ?)
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, prescriptionId);
            statement.setInt(2, allowAll ? 1 : 0);
            statement.setString(3, employeeId);
            PrescriptionDetail header;
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                header = mapDetailHeader(result);
            }
            return new PrescriptionDetail(
                    header.maDonThuoc(),
                    header.maLuotKham(),
                    header.maBacSi(),
                    header.tenBacSi(),
                    header.tenChuyenKhoa(),
                    header.ngayKe(),
                    header.loiDan(),
                    header.trangThai(),
                    header.trangThaiLuotKham(),
                    header.maBenhNhan(),
                    header.tenBenhNhan(),
                    header.ngaySinh(),
                    header.gioiTinh(),
                    header.soDienThoai(),
                    header.thoiGianTiepNhan(),
                    header.daXuatThuoc(),
                    findLines(connection, prescriptionId));
        } catch (SQLException exception) {
            throw databaseError("findById", exception);
        }
    }

    public long create(
            PrescriptionDraft draft,
            String employeeId,
            long accountId,
            boolean allowAll) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String doctorId = verifyEligibleVisit(
                        connection, draft.maLuotKham(), employeeId, allowAll);
                verifyActiveMedications(connection, draft.lines());
                long prescriptionId;
                String insert = """
                        INSERT INTO dbo.DonThuoc
                            (MaLuotKham, MaNhanVien, NgayKe, LoiDan, TrangThai)
                        VALUES (?, ?, SYSDATETIME(), ?, N'Nhap')
                        """;
                try (PreparedStatement statement = connection.prepareStatement(
                        insert, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setLong(1, draft.maLuotKham());
                    statement.setString(2, doctorId);
                    statement.setNString(3, draft.loiDan());
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException(
                                    "SQL Server không trả về mã đơn thuốc mới.");
                        }
                        prescriptionId = keys.getLong(1);
                    }
                }
                insertLines(connection, prescriptionId, draft.lines());
                recordAudit(connection, accountId, "TAO_DON_THUOC", prescriptionId);
                connection.commit();
                return prescriptionId;
            } catch (PrescriptionDAOException exception) {
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
            long prescriptionId,
            PrescriptionDraft draft,
            String employeeId,
            long accountId,
            boolean allowAll) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            connection.setAutoCommit(false);
            try {
                LockedPrescription prescription = lockEditable(
                        connection, prescriptionId, employeeId, allowAll);
                if (prescription.visitId() != draft.maLuotKham()) {
                    throw new PrescriptionDAOException(
                            "Không được thay đổi lượt khám liên kết với đơn thuốc.");
                }
                verifyActiveMedications(connection, draft.lines());
                updateHeader(connection, prescriptionId, draft.loiDan());
                updateOrInsertLines(connection, prescriptionId, draft.lines());
                recordAudit(connection, accountId, "CAP_NHAT_DON_THUOC", prescriptionId);
                connection.commit();
            } catch (PrescriptionDAOException exception) {
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
            long prescriptionId, String employeeId, long accountId, boolean allowAll) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            connection.setAutoCommit(false);
            try {
                LockedPrescription prescription = lockEditable(
                        connection, prescriptionId, employeeId, allowAll);
                if (prescription.exported()) {
                    throw new PrescriptionDAOException(
                            "Đơn thuốc đã được liên kết với phiếu xuất và không thể hủy.");
                }
                String update = """
                        UPDATE dbo.DonThuoc
                        SET TrangThai = N'Huy'
                        WHERE MaDonThuoc = ? AND TrangThai = N'Nhap'
                        """;
                try (PreparedStatement statement = connection.prepareStatement(update)) {
                    statement.setLong(1, prescriptionId);
                    if (statement.executeUpdate() != 1) {
                        throw new PrescriptionDAOException(
                                "Đơn thuốc đã thay đổi trạng thái; hãy tải lại.");
                    }
                }
                recordAudit(connection, accountId, "HUY_DON_THUOC", prescriptionId);
                connection.commit();
            } catch (PrescriptionDAOException exception) {
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

    private PrescriptionDetail mapDetailHeader(ResultSet result) throws SQLException {
        Date birth = result.getDate("NgaySinh");
        return new PrescriptionDetail(
                result.getLong("MaDonThuoc"),
                result.getLong("MaLuotKham"),
                result.getString("MaBacSi"),
                result.getString("TenBacSi"),
                result.getString("TenChuyenKhoa"),
                localDateTime(result, "NgayKe"),
                result.getString("LoiDan"),
                result.getString("TrangThai"),
                result.getString("TrangThaiLuotKham"),
                result.getString("MaBenhNhan"),
                result.getString("TenBenhNhan"),
                birth == null ? null : birth.toLocalDate(),
                result.getString("GioiTinh"),
                result.getString("SoDienThoai"),
                localDateTime(result, "ThoiGianTiepNhan"),
                result.getBoolean("DaXuatThuoc"),
                List.of());
    }

    private List<PrescriptionLineDetail> findLines(
            Connection connection, long prescriptionId) throws SQLException {
        String sql = """
                SELECT ct.MaChiTietDon, ct.MaThuoc, t.TenThuoc, t.HoatChat,
                       t.HamLuong, t.DangBaoChe, t.DonViTinh AS DonViTinhThuoc,
                       t.TrangThai AS TrangThaiThuoc, ct.LieuDung, ct.DuongDung,
                       ct.TanSuat, ct.ThoiGianDung, ct.SoLuong,
                       ct.DonViTinh, ct.HuongDan,
                       CASE WHEN EXISTS (
                           SELECT 1 FROM dbo.ChiTietPhieuXuat cpx
                           WHERE cpx.MaChiTietDon = ct.MaChiTietDon
                       ) THEN 1 ELSE 0 END AS DaXuat
                FROM dbo.ChiTietDonThuoc ct
                JOIN dbo.Thuoc t ON t.MaThuoc = ct.MaThuoc
                WHERE ct.MaDonThuoc = ?
                ORDER BY ct.MaChiTietDon
                """;
        List<PrescriptionLineDetail> lines = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, prescriptionId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    lines.add(new PrescriptionLineDetail(
                            result.getLong("MaChiTietDon"),
                            result.getString("MaThuoc"),
                            result.getString("TenThuoc"),
                            result.getString("HoatChat"),
                            result.getString("HamLuong"),
                            result.getString("DangBaoChe"),
                            result.getString("DonViTinhThuoc"),
                            result.getString("TrangThaiThuoc"),
                            result.getString("LieuDung"),
                            result.getString("DuongDung"),
                            result.getString("TanSuat"),
                            result.getString("ThoiGianDung"),
                            result.getBigDecimal("SoLuong"),
                            result.getString("DonViTinh"),
                            result.getString("HuongDan"),
                            result.getBoolean("DaXuat")));
                }
            }
        }
        return List.copyOf(lines);
    }

    private PrescriptionListItem mapListItem(ResultSet result) throws SQLException {
        return new PrescriptionListItem(
                result.getLong("MaDonThuoc"),
                result.getLong("MaLuotKham"),
                result.getString("MaBenhNhan"),
                result.getString("TenBenhNhan"),
                result.getString("SoDienThoai"),
                result.getString("MaBacSi"),
                result.getString("TenBacSi"),
                result.getString("MaChuyenKhoa"),
                result.getString("TenChuyenKhoa"),
                localDateTime(result, "NgayKe"),
                result.getInt("SoLoaiThuoc"),
                result.getString("TrangThai"),
                result.getBoolean("DaXuatThuoc"));
    }

    private String verifyEligibleVisit(
            Connection connection, long visitId, String employeeId, boolean allowAll)
            throws SQLException {
        String sql = """
                SELECT lk.MaNhanVien, lk.TrangThai, pk.MaNhanVien AS MaBacSiPhieu
                FROM dbo.LuotKham lk WITH (UPDLOCK, HOLDLOCK)
                LEFT JOIN dbo.PhieuKham pk WITH (UPDLOCK, HOLDLOCK)
                    ON pk.MaLuotKham = lk.MaLuotKham
                WHERE lk.MaLuotKham = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, visitId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new PrescriptionDAOException("Không tìm thấy lượt khám.");
                }
                String assignedDoctor = result.getString("MaNhanVien");
                String state = result.getString("TrangThai");
                String recordDoctor = result.getString("MaBacSiPhieu");
                if (!"DangKham".equals(state) && !"ChoKetQua".equals(state)) {
                    throw new PrescriptionDAOException(
                            "Chỉ được kê đơn cho lượt khám đang khám hoặc chờ kết quả.");
                }
                if (recordDoctor != null && !assignedDoctor.equals(recordDoctor)) {
                    throw new PrescriptionDAOException(
                            "Bác sĩ trên phiếu khám không khớp bác sĩ phụ trách lượt khám.");
                }
                if (!allowAll && !employeeId.equals(assignedDoctor)) {
                    throw new PrescriptionDAOException(
                            "Bác sĩ chỉ được kê đơn cho lượt khám do mình phụ trách.");
                }
                return assignedDoctor;
            }
        }
    }

    private void verifyActiveMedications(
            Connection connection, List<PrescriptionLineDraft> lines) throws SQLException {
        Set<String> medicationIds = new HashSet<>();
        String sql = """
                SELECT TrangThai
                FROM dbo.Thuoc WITH (UPDLOCK, HOLDLOCK)
                WHERE MaThuoc = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (PrescriptionLineDraft line : lines) {
                if (!medicationIds.add(line.maThuoc())) {
                    throw new PrescriptionDAOException(
                            "Một thuốc chỉ được có một dòng trong cùng đơn; hãy sửa dòng thuốc hiện có.");
                }
                statement.setString(1, line.maThuoc());
                try (ResultSet result = statement.executeQuery()) {
                    if (!result.next()) {
                        throw new PrescriptionDAOException(
                                "Không tìm thấy thuốc " + line.maThuoc() + ".");
                    }
                    if (!"DangSuDung".equals(result.getString("TrangThai"))) {
                        throw new PrescriptionDAOException(
                                "Thuốc " + line.maThuoc() + " đã ngừng sử dụng.");
                    }
                }
            }
        }
    }

    private void insertLines(
            Connection connection, long prescriptionId, List<PrescriptionLineDraft> lines)
            throws SQLException {
        String sql = """
                INSERT INTO dbo.ChiTietDonThuoc
                    (MaDonThuoc, MaThuoc, LieuDung, DuongDung, TanSuat,
                     ThoiGianDung, SoLuong, DonViTinh, HuongDan)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (PrescriptionLineDraft line : lines) {
                bindLine(statement, prescriptionId, line);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void updateHeader(
            Connection connection, long prescriptionId, String directions)
            throws SQLException {
        String sql = """
                UPDATE dbo.DonThuoc
                SET LoiDan = ?
                WHERE MaDonThuoc = ? AND TrangThai = N'Nhap'
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setNString(1, directions);
            statement.setLong(2, prescriptionId);
            if (statement.executeUpdate() != 1) {
                throw new PrescriptionDAOException(
                        "Đơn thuốc đã đổi trạng thái; hãy tải lại dữ liệu.");
            }
        }
    }

    private void updateOrInsertLines(
            Connection connection, long prescriptionId, List<PrescriptionLineDraft> lines)
            throws SQLException {
        Set<Long> existingLineIds = findLineIds(connection, prescriptionId);
        Set<Long> submittedLineIds = new HashSet<>();
        String update = """
                UPDATE dbo.ChiTietDonThuoc
                SET MaThuoc = ?, LieuDung = ?, DuongDung = ?, TanSuat = ?,
                    ThoiGianDung = ?, SoLuong = ?, DonViTinh = ?, HuongDan = ?
                WHERE MaChiTietDon = ? AND MaDonThuoc = ?
                """;
        String insert = """
                INSERT INTO dbo.ChiTietDonThuoc
                    (MaDonThuoc, MaThuoc, LieuDung, DuongDung, TanSuat,
                     ThoiGianDung, SoLuong, DonViTinh, HuongDan)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement updateStatement = connection.prepareStatement(update);
             PreparedStatement insertStatement = connection.prepareStatement(insert)) {
            for (PrescriptionLineDraft line : lines) {
                if (line.maChiTietDon() == null) {
                    bindLine(insertStatement, prescriptionId, line);
                    insertStatement.addBatch();
                    continue;
                }
                if (!existingLineIds.contains(line.maChiTietDon())
                        || !submittedLineIds.add(line.maChiTietDon())) {
                    throw new PrescriptionDAOException(
                            "Chi tiết thuốc không thuộc đơn này hoặc bị lặp.");
                }
                updateStatement.setString(1, line.maThuoc());
                updateStatement.setNString(2, line.lieuDung());
                updateStatement.setNString(3, line.duongDung());
                updateStatement.setNString(4, line.tanSuat());
                updateStatement.setNString(5, line.thoiGianDung());
                updateStatement.setBigDecimal(6, line.soLuong());
                updateStatement.setNString(7, line.donViTinh());
                updateStatement.setNString(8, line.huongDan());
                updateStatement.setLong(9, line.maChiTietDon());
                updateStatement.setLong(10, prescriptionId);
                if (updateStatement.executeUpdate() != 1) {
                    throw new PrescriptionDAOException(
                            "Chi tiết thuốc đã thay đổi; hãy tải lại dữ liệu.");
                }
            }
            if (!submittedLineIds.containsAll(existingLineIds)) {
                throw new PrescriptionDAOException(
                        "Không thể xóa chi tiết đã lưu. Giữ các dòng hiện có và chỉ thêm/sửa thuốc.");
            }
            insertStatement.executeBatch();
        }
    }

    private Set<Long> findLineIds(Connection connection, long prescriptionId)
            throws SQLException {
        Set<Long> ids = new HashSet<>();
        String sql = """
                SELECT MaChiTietDon FROM dbo.ChiTietDonThuoc WITH (UPDLOCK, HOLDLOCK)
                WHERE MaDonThuoc = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, prescriptionId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    ids.add(result.getLong(1));
                }
            }
        }
        return ids;
    }

    private void bindLine(
            PreparedStatement statement, long prescriptionId, PrescriptionLineDraft line)
            throws SQLException {
        statement.setLong(1, prescriptionId);
        statement.setString(2, line.maThuoc());
        statement.setNString(3, line.lieuDung());
        statement.setNString(4, line.duongDung());
        statement.setNString(5, line.tanSuat());
        statement.setNString(6, line.thoiGianDung());
        statement.setBigDecimal(7, line.soLuong());
        statement.setNString(8, line.donViTinh());
        statement.setNString(9, line.huongDan());
    }

    private LockedPrescription lockEditable(
            Connection connection, long prescriptionId, String employeeId, boolean allowAll)
            throws SQLException {
        String sql = """
                SELECT dt.MaLuotKham, dt.MaNhanVien, dt.TrangThai,
                       lk.TrangThai AS TrangThaiLuotKham,
                       CASE WHEN EXISTS (
                           SELECT 1 FROM dbo.PhieuXuat px WITH (UPDLOCK, HOLDLOCK)
                           WHERE px.MaDonThuoc = dt.MaDonThuoc
                       ) OR EXISTS (
                           SELECT 1 FROM dbo.ChiTietDonThuoc ct
                           JOIN dbo.ChiTietPhieuXuat cpx WITH (UPDLOCK, HOLDLOCK)
                               ON cpx.MaChiTietDon = ct.MaChiTietDon
                           WHERE ct.MaDonThuoc = dt.MaDonThuoc
                       ) THEN 1 ELSE 0 END AS DaXuatThuoc
                FROM dbo.DonThuoc dt WITH (UPDLOCK, HOLDLOCK)
                JOIN dbo.LuotKham lk WITH (UPDLOCK, HOLDLOCK)
                    ON lk.MaLuotKham = dt.MaLuotKham
                WHERE dt.MaDonThuoc = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, prescriptionId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new PrescriptionDAOException("Không tìm thấy đơn thuốc.");
                }
                String doctorId = result.getString("MaNhanVien");
                String status = result.getString("TrangThai");
                String visitStatus = result.getString("TrangThaiLuotKham");
                boolean exported = result.getBoolean("DaXuatThuoc");
                if (!allowAll && !employeeId.equals(doctorId)) {
                    throw new PrescriptionDAOException(
                            "Bác sĩ chỉ được thay đổi đơn thuốc do mình kê.");
                }
                if (!"Nhap".equals(status)) {
                    throw new PrescriptionDAOException(
                            "Chỉ được sửa hoặc hủy đơn thuốc ở trạng thái Nhap.");
                }
                if (!"DangKham".equals(visitStatus) && !"ChoKetQua".equals(visitStatus)) {
                    throw new PrescriptionDAOException(
                            "Lượt khám không còn ở trạng thái cho phép sửa/hủy đơn.");
                }
                return new LockedPrescription(
                        result.getLong("MaLuotKham"), exported);
            }
        }
    }

    private void recordAudit(
            Connection connection, long accountId, String action, long prescriptionId)
            throws SQLException {
        String sql = """
                INSERT INTO dbo.NhatKyHeThong
                    (MaTaiKhoan, HanhDong, DoiTuong, MaDoiTuong, NoiDung)
                VALUES (?, ?, N'DonThuoc', ?, N'')
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, accountId);
            statement.setNString(2, action);
            statement.setString(3, Long.toString(prescriptionId));
            statement.executeUpdate();
        }
    }

    private Query buildQuery(PrescriptionFilter filter) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> parameters = new ArrayList<>();
        String search = filter.searchText() == null ? "" : filter.searchText().trim();
        if (!search.isEmpty()) {
            String pattern = "%" + escapeLike(search) + "%";
            where.append("""
                     AND (CONVERT(nvarchar(20), dt.MaDonThuoc) LIKE ? ESCAPE '~'
                       OR CONVERT(nvarchar(20), lk.MaLuotKham) LIKE ? ESCAPE '~'
                       OR bn.MaBenhNhan LIKE ? ESCAPE '~'
                       OR bn.HoTen LIKE ? ESCAPE '~'
                       OR bn.SoDienThoai LIKE ? ESCAPE '~'
                       OR nv.HoTen LIKE ? ESCAPE '~')
                    """);
            for (int i = 0; i < 6; i++) {
                parameters.add(pattern);
            }
        }
        if (filter.fromDate() != null) {
            where.append(" AND dt.NgayKe >= ?");
            parameters.add(filter.fromDate());
        }
        if (filter.toDate() != null) {
            where.append(" AND dt.NgayKe < DATEADD(day, 1, ?)");
            parameters.add(filter.toDate());
        }
        if (filter.status() != null && !filter.status().isBlank()) {
            where.append(" AND dt.TrangThai = ?");
            parameters.add(filter.status());
        }
        if (filter.doctorId() != null && !filter.doctorId().isBlank()) {
            where.append(" AND dt.MaNhanVien = ?");
            parameters.add(filter.doctorId());
        }
        if (filter.specialtyId() != null && !filter.specialtyId().isBlank()) {
            where.append(" AND bs.MaChuyenKhoa = ?");
            parameters.add(filter.specialtyId());
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
                statement.setString(index++, text);
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

    private PrescriptionDAOException databaseError(String method, SQLException exception) {
        return new PrescriptionDAOException(
                "Lỗi SQL trong DonThuocDAO." + method
                + " (SQLState=" + exception.getSQLState()
                + ", mã lỗi=" + exception.getErrorCode() + "): "
                + exception.getMessage(),
                exception);
    }

    private record Query(String whereClause, List<Object> parameters) {
    }

    private record LockedPrescription(long visitId, boolean exported) {
    }
}
