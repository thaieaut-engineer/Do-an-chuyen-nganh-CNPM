package dao.appointment;

import config.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import model.appointment.AppointmentDoctor;
import model.appointment.AppointmentDraft;
import model.appointment.AppointmentFilter;
import model.appointment.AppointmentHistoryEntry;
import model.appointment.AppointmentPage;
import model.appointment.AppointmentPatient;
import model.appointment.AppointmentSlot;
import model.appointment.AppointmentSpecialty;
import model.appointment.AppointmentStats;
import model.appointment.LichHen;

public final class LichHenDAO {
    private static final String ACTIVE_APPOINTMENT_STATES =
            "N'ChoXacNhan', N'DaXacNhan', N'DaDen', N'DangKham'";

    public LocalDate getDatabaseDate() {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT CAST(SYSDATETIME() AS date)");
             ResultSet result = statement.executeQuery()) {
            if (!result.next()) {
                throw new SQLException("SQL Server không trả về ngày hiện tại.");
            }
            return result.getDate(1).toLocalDate();
        } catch (SQLException exception) {
            throw databaseError("Không thể tải ngày hiện tại từ SQL Server.", exception);
        }
    }

    public AppointmentPage search(AppointmentFilter filter) {
        QueryConditions conditions = conditions(filter);
        String from = """
                FROM dbo.LichHen lh
                JOIN dbo.BenhNhan bn ON bn.MaBenhNhan = lh.MaBenhNhan
                LEFT JOIN dbo.NhanVien nv ON nv.MaNhanVien = lh.MaNhanVien
                LEFT JOIN dbo.BacSi bs ON bs.MaNhanVien = lh.MaNhanVien
                LEFT JOIN dbo.ChuyenKhoa ck
                    ON ck.MaChuyenKhoa = COALESCE(lh.MaChuyenKhoa, bs.MaChuyenKhoa)
                LEFT JOIN dbo.NhanVien creator ON creator.MaNhanVien = lh.NguoiTao
                """;
        String countSql = "SELECT COUNT_BIG(*) " + from + conditions.sql();
        String pageSql = """
                SELECT lh.MaLichHen, lh.MaBenhNhan, bn.HoTen AS TenBenhNhan,
                       bn.SoDienThoai AS SoDienThoaiBenhNhan, lh.MaKhungGio,
                       lh.MaNhanVien, nv.HoTen AS TenBacSi,
                       COALESCE(lh.MaChuyenKhoa, bs.MaChuyenKhoa) AS MaChuyenKhoa,
                       ck.TenChuyenKhoa, lh.ThoiGianHen, lh.LyDoKham, lh.TrangThai,
                       lh.NguoiTao, creator.HoTen AS TenNguoiTao, lh.GhiChu
                """ + from + conditions.sql() + """
                 ORDER BY lh.ThoiGianHen, lh.MaLichHen
                 OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """;
        try (Connection connection = DatabaseConnection.getConnection()) {
            long total = executeCount(connection, countSql, conditions.parameters());
            List<LichHen> appointments = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(pageSql)) {
                bind(statement, conditions.parameters(), 1);
                int parameter = conditions.parameters().size() + 1;
                statement.setLong(parameter++, (long) filter.page() * filter.pageSize());
                statement.setInt(parameter, filter.pageSize());
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        appointments.add(mapAppointment(result));
                    }
                }

            }
            return new AppointmentPage(
                    appointments, total, filter.page(), filter.pageSize());
        } catch (SQLException exception) {
            throw databaseError("Không thể tải danh sách lịch hẹn.", exception);
        }
    }

    public AppointmentStats getTodayStats() {
        String sql = """
                SELECT
                    COUNT_BIG(*) AS TotalToday,
                    COALESCE(SUM(CASE WHEN TrangThai = N'ChoXacNhan'
                                      THEN 1 ELSE 0 END), 0) AS PendingToday,
                    COALESCE(SUM(CASE WHEN TrangThai = N'DaXacNhan'
                                      THEN 1 ELSE 0 END), 0) AS ConfirmedToday,
                    COALESCE(SUM(CASE WHEN TrangThai = N'DaHuy'
                                      THEN 1 ELSE 0 END), 0) AS CancelledToday
                FROM dbo.LichHen
                WHERE ThoiGianHen >= CAST(SYSDATETIME() AS date)
                  AND ThoiGianHen < DATEADD(day, 1, CAST(SYSDATETIME() AS date))
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            if (!result.next()) {
                throw new SQLException("SQL Server không trả về số liệu lịch hẹn.");
            }
            return new AppointmentStats(
                    result.getLong("TotalToday"),
                    result.getLong("PendingToday"),
                    result.getLong("ConfirmedToday"),
                    result.getLong("CancelledToday"));
        } catch (SQLException exception) {
            throw databaseError("Không thể tải thống kê lịch hẹn.", exception);
        }
    }

    public List<String> findStatuses() {
        String sql = "SELECT DISTINCT TrangThai FROM dbo.LichHen ORDER BY TrangThai";
        List<String> statuses = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                statuses.add(result.getString(1));
            }
            return List.copyOf(statuses);
        } catch (SQLException exception) {
            throw databaseError("Không thể tải trạng thái lịch hẹn.", exception);
        }
    }

    public List<AppointmentPatient> searchPatients(String searchText) {
        String sql = """
                SELECT TOP (50) MaBenhNhan, HoTen, SoDienThoai
                FROM dbo.BenhNhan
                WHERE TrangThai = N'HoatDong'
                  AND (? = N'' OR MaBenhNhan LIKE ? ESCAPE '~'
                       OR HoTen LIKE ? ESCAPE '~'
                       OR SoDienThoai LIKE ? ESCAPE '~')
                ORDER BY HoTen, MaBenhNhan
                """;
        String normalized = searchText == null ? "" : searchText.trim();
        String pattern = "%" + escapeLike(normalized) + "%";
        List<AppointmentPatient> patients = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setNString(1, normalized);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            statement.setString(4, pattern);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    patients.add(new AppointmentPatient(
                            result.getString("MaBenhNhan"),
                            result.getString("HoTen"),
                            result.getString("SoDienThoai")));
                }
            }
            return List.copyOf(patients);
        } catch (SQLException exception) {
            throw databaseError("Không thể tìm bệnh nhân để đặt lịch.", exception);
        }
    }

    public List<AppointmentSpecialty> findSpecialties() {
        String sql = """
                SELECT MaChuyenKhoa, TenChuyenKhoa
                FROM dbo.ChuyenKhoa
                WHERE TrangThai = N'HoatDong'
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
            throw databaseError("Không thể tải danh sách chuyên khoa.", exception);
        }
    }

    public List<AppointmentDoctor> findDoctors(String specialtyId, LocalDate date) {
        String sql = """
                SELECT DISTINCT nv.MaNhanVien, nv.HoTen,
                       bs.MaChuyenKhoa, ck.TenChuyenKhoa
                FROM dbo.BacSi bs
                JOIN dbo.NhanVien nv ON nv.MaNhanVien = bs.MaNhanVien
                JOIN dbo.ChuyenKhoa ck ON ck.MaChuyenKhoa = bs.MaChuyenKhoa
                WHERE bs.MaChuyenKhoa = ?
                  AND ck.TrangThai = N'HoatDong'
                  AND nv.TrangThaiCongTac IN (N'ThuViec', N'DangLam')
                  AND EXISTS (
                      SELECT 1
                      FROM dbo.LichPhanCong lp
                      JOIN dbo.CaLamViec ca ON ca.MaCa = lp.MaCa
                      WHERE lp.MaNhanVien = bs.MaNhanVien
                        AND lp.NgayLam = ?
                        AND lp.TrangThai = N'DaXacNhan'
                        AND ca.TrangThai = N'HoatDong')
                ORDER BY nv.HoTen, nv.MaNhanVien
                """;
        List<AppointmentDoctor> doctors = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, specialtyId);
            statement.setObject(2, date);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    doctors.add(new AppointmentDoctor(
                            result.getString("MaNhanVien"),
                            result.getString("HoTen"),
                            result.getString("MaChuyenKhoa"),
                            result.getString("TenChuyenKhoa")));
                }
            }
            return List.copyOf(doctors);
        } catch (SQLException exception) {
            throw databaseError("Không thể tải bác sĩ theo lịch phân công.", exception);
        }
    }

    public List<AppointmentDoctor> findDoctors() {
        String sql = """
                SELECT nv.MaNhanVien, nv.HoTen, bs.MaChuyenKhoa, ck.TenChuyenKhoa
                FROM dbo.BacSi bs
                JOIN dbo.NhanVien nv ON nv.MaNhanVien = bs.MaNhanVien
                JOIN dbo.ChuyenKhoa ck ON ck.MaChuyenKhoa = bs.MaChuyenKhoa
                WHERE ck.TrangThai = N'HoatDong'
                  AND nv.TrangThaiCongTac IN (N'ThuViec', N'DangLam')
                ORDER BY nv.HoTen, nv.MaNhanVien
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
            throw databaseError("Không thể tải danh sách bác sĩ.", exception);
        }
    }

    public List<AppointmentSlot> findAvailableSlots(
            LocalDate date, String doctorId, String specialtyId,
            String patientId, Long excludeAppointmentId) {
        String sql = """
                SELECT kg.MaKhungGio, kg.NgayKham, kg.GioBatDau, kg.GioKetThuc,
                       COALESCE(kg.SoLuongToiDa, 1) AS SoLuongToiDa,
                       (SELECT COUNT_BIG(*)
                        FROM dbo.LichHen booked
                        WHERE booked.MaKhungGio = kg.MaKhungGio
                          AND booked.TrangThai IN (%s)
                          AND (? IS NULL OR booked.MaLichHen <> ?)) AS BookedCount
                FROM dbo.KhungGioKham kg
                JOIN dbo.BacSi bs ON bs.MaNhanVien = kg.MaNhanVien
                JOIN dbo.NhanVien nv ON nv.MaNhanVien = bs.MaNhanVien
                JOIN dbo.ChuyenKhoa ck ON ck.MaChuyenKhoa = bs.MaChuyenKhoa
                WHERE kg.NgayKham = ?
                  AND kg.MaNhanVien = ?
                  AND bs.MaChuyenKhoa = ?
                  AND ck.TrangThai = N'HoatDong'
                  AND nv.TrangThaiCongTac IN (N'ThuViec', N'DangLam')
                  AND (kg.TrangThai = N'Mo' OR kg.MaKhungGio = ?)
                  AND (kg.NgayKham > CAST(SYSDATETIME() AS date)
                       OR (kg.NgayKham = CAST(SYSDATETIME() AS date)
                           AND kg.GioBatDau > CAST(SYSDATETIME() AS time)))
                  AND EXISTS (
                      SELECT 1
                      FROM dbo.LichPhanCong lp
                      JOIN dbo.CaLamViec ca ON ca.MaCa = lp.MaCa
                      WHERE lp.MaNhanVien = kg.MaNhanVien
                        AND lp.NgayLam = kg.NgayKham
                        AND lp.TrangThai = N'DaXacNhan'
                        AND ca.TrangThai = N'HoatDong'
                        AND kg.GioBatDau >= ca.GioBatDau
                        AND kg.GioKetThuc <= ca.GioKetThuc)
                  AND (SELECT COUNT_BIG(*)
                       FROM dbo.LichHen booked
                       WHERE booked.MaKhungGio = kg.MaKhungGio
                         AND booked.TrangThai IN (%s)
                         AND (? IS NULL OR booked.MaLichHen <> ?))
                      < COALESCE(kg.SoLuongToiDa, 1)
                  AND NOT EXISTS (
                      SELECT 1
                      FROM dbo.LichHen other
                      LEFT JOIN dbo.KhungGioKham otherSlot
                        ON otherSlot.MaKhungGio = other.MaKhungGio
                      WHERE other.MaNhanVien = kg.MaNhanVien
                        AND other.TrangThai IN (%s)
                        AND (? IS NULL OR other.MaLichHen <> ?)
                        AND (other.MaKhungGio IS NULL
                             OR other.MaKhungGio <> kg.MaKhungGio)
                        AND other.ThoiGianHen >= CAST(kg.NgayKham AS datetime2)
                        AND other.ThoiGianHen < DATEADD(day, 1, CAST(kg.NgayKham AS datetime2))
                        AND ((otherSlot.MaKhungGio IS NOT NULL
                              AND otherSlot.GioBatDau < kg.GioKetThuc
                              AND otherSlot.GioKetThuc > kg.GioBatDau)
                             OR (otherSlot.MaKhungGio IS NULL
                                 AND other.ThoiGianHen =
                                     DATEADD(second, DATEDIFF(second, CAST('00:00:00' AS time),
                                         kg.GioBatDau), CAST(kg.NgayKham AS datetime2)))))
                  AND (? IS NULL OR NOT EXISTS (
                      SELECT 1
                      FROM dbo.LichHen other
                      LEFT JOIN dbo.KhungGioKham otherSlot
                        ON otherSlot.MaKhungGio = other.MaKhungGio
                      WHERE other.MaBenhNhan = ?
                        AND other.TrangThai IN (%s)
                        AND (? IS NULL OR other.MaLichHen <> ?)
                        AND other.ThoiGianHen >= CAST(kg.NgayKham AS datetime2)
                        AND other.ThoiGianHen < DATEADD(day, 1, CAST(kg.NgayKham AS datetime2))
                        AND ((otherSlot.MaKhungGio IS NOT NULL
                              AND otherSlot.GioBatDau < kg.GioKetThuc
                              AND otherSlot.GioKetThuc > kg.GioBatDau)
                             OR (otherSlot.MaKhungGio IS NULL
                                 AND other.ThoiGianHen =
                                     DATEADD(second, DATEDIFF(second, CAST('00:00:00' AS time),
                                         kg.GioBatDau), CAST(kg.NgayKham AS datetime2))))))
                ORDER BY kg.GioBatDau, kg.MaKhungGio
                """.formatted(ACTIVE_APPOINTMENT_STATES, ACTIVE_APPOINTMENT_STATES,
                ACTIVE_APPOINTMENT_STATES, ACTIVE_APPOINTMENT_STATES);
        List<AppointmentSlot> slots = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            Long originalSlotId = excludeAppointmentId == null
                    ? null : findAppointmentSlot(connection, excludeAppointmentId);
            int parameter = 1;
            setNullableLong(statement, parameter++, excludeAppointmentId);
            setNullableLong(statement, parameter++, excludeAppointmentId);
            statement.setObject(parameter++, date);
            statement.setString(parameter++, doctorId);
            statement.setString(parameter++, specialtyId);
            setNullableLong(statement, parameter++, originalSlotId);
            setNullableLong(statement, parameter++, excludeAppointmentId);
            setNullableLong(statement, parameter++, excludeAppointmentId);
            setNullableLong(statement, parameter++, excludeAppointmentId);
            setNullableLong(statement, parameter++, excludeAppointmentId);
            statement.setString(parameter++, patientId);
            statement.setString(parameter++, patientId);
            setNullableLong(statement, parameter++, excludeAppointmentId);
            setNullableLong(statement, parameter, excludeAppointmentId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    slots.add(new AppointmentSlot(
                            result.getLong("MaKhungGio"),
                            result.getDate("NgayKham").toLocalDate(),
                            result.getTime("GioBatDau").toLocalTime(),
                            result.getTime("GioKetThuc").toLocalTime(),
                            result.getInt("SoLuongToiDa"),
                            result.getInt("BookedCount")));
                }
            }
            return List.copyOf(slots);
        } catch (SQLException exception) {
            throw databaseError("Không thể tải khung giờ còn trống.", exception);
        }
    }

    public LichHen findById(long appointmentId) {
        String sql = """
                SELECT lh.MaLichHen, lh.MaBenhNhan, bn.HoTen AS TenBenhNhan,
                       bn.SoDienThoai AS SoDienThoaiBenhNhan, lh.MaKhungGio,
                       lh.MaNhanVien, nv.HoTen AS TenBacSi,
                       COALESCE(lh.MaChuyenKhoa, bs.MaChuyenKhoa) AS MaChuyenKhoa,
                       ck.TenChuyenKhoa, lh.ThoiGianHen, lh.LyDoKham, lh.TrangThai,
                       lh.NguoiTao, creator.HoTen AS TenNguoiTao, lh.GhiChu
                FROM dbo.LichHen lh
                JOIN dbo.BenhNhan bn ON bn.MaBenhNhan = lh.MaBenhNhan
                LEFT JOIN dbo.NhanVien nv ON nv.MaNhanVien = lh.MaNhanVien
                LEFT JOIN dbo.BacSi bs ON bs.MaNhanVien = lh.MaNhanVien
                LEFT JOIN dbo.ChuyenKhoa ck
                    ON ck.MaChuyenKhoa = COALESCE(lh.MaChuyenKhoa, bs.MaChuyenKhoa)
                LEFT JOIN dbo.NhanVien creator ON creator.MaNhanVien = lh.NguoiTao
                WHERE lh.MaLichHen = ?
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, appointmentId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? mapAppointment(result) : null;
            }
        } catch (SQLException exception) {
            throw databaseError("Không thể tải chi tiết lịch hẹn.", exception);
        }
    }

    public List<AppointmentHistoryEntry> findHistory(long appointmentId) {
        String sql = """
                SELECT h.ThoiDiem, nv.HoTen AS NguoiThayDoi, h.HanhDong,
                       h.GiaTriCu, h.GiaTriMoi, h.LyDo
                FROM dbo.LichSuLichHen h
                LEFT JOIN dbo.NhanVien nv ON nv.MaNhanVien = h.NguoiThayDoi
                WHERE h.MaLichHen = ?
                ORDER BY h.ThoiDiem DESC, h.MaLichSu DESC
                """;
        List<AppointmentHistoryEntry> history = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, appointmentId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    java.sql.Timestamp timestamp = result.getTimestamp("ThoiDiem");
                    history.add(new AppointmentHistoryEntry(
                            timestamp == null ? null : timestamp.toLocalDateTime(),
                            result.getString("NguoiThayDoi"),
                            result.getString("HanhDong"),
                            result.getString("GiaTriCu"),
                            result.getString("GiaTriMoi"),
                            result.getString("LyDo")));
                }
            }
            return List.copyOf(history);
        } catch (SQLException exception) {
            throw databaseError("Không thể tải lịch sử lịch hẹn.", exception);
        }
    }

    public long insert(AppointmentDraft draft, String creatorId) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            beginTransaction(connection);
            try {
                SlotDetails slot = lockAndValidateSelection(
                        connection, draft, null, null);
                LocalDateTime appointmentTime = slot.date().atTime(slot.start());
                String sql = """
                        INSERT INTO dbo.LichHen
                            (MaBenhNhan, MaKhungGio, MaChuyenKhoa, MaNhanVien,
                             ThoiGianHen, LyDoKham, TrangThai, NguoiTao, GhiChu)
                        VALUES (?, ?, ?, ?, ?, ?, N'ChoXacNhan', ?, ?)
                        """;
                long appointmentId;
                try (PreparedStatement statement = connection.prepareStatement(
                        sql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setString(1, draft.maBenhNhan());
                    statement.setLong(2, draft.maKhungGio());
                    statement.setString(3, draft.maChuyenKhoa());
                    statement.setString(4, draft.maNhanVien());
                    statement.setObject(5, appointmentTime);
                    statement.setNString(6, draft.lyDoKham());
                    statement.setString(7, creatorId);
                    statement.setNString(8, draft.ghiChu());
                    statement.executeUpdate();
                    try (ResultSet keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Không nhận được mã lịch hẹn mới.");
                        }
                        appointmentId = keys.getLong(1);
                    }
                }
                writeHistory(connection, appointmentId, creatorId,
                        "TaoLichHen", null, snapshot(draft, appointmentTime),
                        "Tạo lịch hẹn");
                refreshSlotState(connection, draft.maKhungGio());
                connection.commit();
                return appointmentId;
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (AppointmentDAOException exception) {
            throw exception;
        } catch (SQLException exception) {
            throw databaseError("Không thể đặt lịch hẹn.", exception);
        }
    }

    public void update(
            long appointmentId, AppointmentDraft draft, String changedBy) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            beginTransaction(connection);
            try {
                LichHen old = lockEditableAppointment(connection, appointmentId);
                SlotDetails slot = lockAndValidateSelection(
                        connection, draft, appointmentId, old.maKhungGio());
                LocalDateTime appointmentTime = slot.date().atTime(slot.start());
                String sql = """
                        UPDATE dbo.LichHen
                        SET MaKhungGio = ?, MaChuyenKhoa = ?, MaNhanVien = ?,
                            ThoiGianHen = ?, LyDoKham = ?, GhiChu = ?
                        WHERE MaLichHen = ?
                        """;
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setLong(1, draft.maKhungGio());
                    statement.setString(2, draft.maChuyenKhoa());
                    statement.setString(3, draft.maNhanVien());
                    statement.setObject(4, appointmentTime);
                    statement.setNString(5, draft.lyDoKham());
                    statement.setNString(6, draft.ghiChu());
                    statement.setLong(7, appointmentId);
                    if (statement.executeUpdate() != 1) {
                        throw new AppointmentDAOException(
                                AppointmentDAOException.Reason.NOT_FOUND,
                                "Không tìm thấy lịch hẹn cần cập nhật.", null);
                    }
                }
                writeHistory(connection, appointmentId, changedBy,
                        "CapNhatLichHen", snapshot(old), snapshot(draft, appointmentTime),
                        "Cập nhật lịch hẹn");
                if (old.maKhungGio() != null
                        && old.maKhungGio() != draft.maKhungGio()) {
                    refreshSlotState(connection, old.maKhungGio());
                }
                refreshSlotState(connection, draft.maKhungGio());
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (AppointmentDAOException exception) {
            throw exception;
        } catch (SQLException exception) {
            throw databaseError("Không thể cập nhật lịch hẹn.", exception);
        }
    }

    public void confirm(long appointmentId, String changedBy) {
        changeStatus(appointmentId, changedBy, "DaXacNhan", "XacNhanLichHen", null);
    }

    public void cancel(long appointmentId, String changedBy, String reason) {
        changeStatus(appointmentId, changedBy, "DaHuy", "HuyLichHen", reason);
    }

    private void changeStatus(
            long appointmentId, String changedBy, String newStatus,
            String action, String reason) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            beginTransaction(connection);
            try {
                LichHen old = lockEditableAppointment(connection, appointmentId);
                if (!"ChoXacNhan".equals(old.trangThai())
                        && !("DaHuy".equals(newStatus)
                        && "DaXacNhan".equals(old.trangThai()))) {
                    throw new AppointmentDAOException(
                            AppointmentDAOException.Reason.INVALID_STATE,
                            "Chỉ lịch chờ xác nhận hoặc đã xác nhận mới được thao tác này.",
                            null);
                }
                ensureNoVisit(connection, appointmentId);
                String sql = "UPDATE dbo.LichHen SET TrangThai = ? WHERE MaLichHen = ?";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, newStatus);
                    statement.setLong(2, appointmentId);
                    if (statement.executeUpdate() != 1) {
                        throw new AppointmentDAOException(
                                AppointmentDAOException.Reason.NOT_FOUND,
                                "Không tìm thấy lịch hẹn cần cập nhật trạng thái.", null);
                    }
                }
                writeHistory(connection, appointmentId, changedBy, action,
                        old.trangThai(), newStatus, reason);
                if (old.maKhungGio() != null) {
                    refreshSlotState(connection, old.maKhungGio());
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (AppointmentDAOException exception) {
            throw exception;
        } catch (SQLException exception) {
            throw databaseError("Không thể cập nhật trạng thái lịch hẹn.", exception);
        }
    }

    private SlotDetails lockAndValidateSelection(
            Connection connection,
            AppointmentDraft draft,
            Long excludeAppointmentId,
            Long originalSlotId) throws SQLException {
        lockActivePatient(connection, draft.maBenhNhan());
        SlotDetails slot = findSlotForUpdate(connection, draft.maKhungGio());
        if (slot == null) {
            throw new AppointmentDAOException(
                    AppointmentDAOException.Reason.SLOT_UNAVAILABLE,
                    "Khung giờ không còn tồn tại.", null);
        }
        lockDoctorAssignments(connection, draft.maNhanVien(), slot.date());
        if (!slot.doctorId().equals(draft.maNhanVien())
                || !slot.specialtyId().equals(draft.maChuyenKhoa())) {
            throw new AppointmentDAOException(
                    AppointmentDAOException.Reason.INVALID_REFERENCE,
                    "Bác sĩ, chuyên khoa và khung giờ không khớp.", null);
        }
        if (!"Mo".equals(slot.status())
                && (originalSlotId == null || originalSlotId != slot.slotId())) {
            throw new AppointmentDAOException(
                    AppointmentDAOException.Reason.SLOT_UNAVAILABLE,
                    "Khung giờ đã bị khóa hoặc không còn mở.", null);
        }
        LocalDateTime appointmentTime = slot.date().atTime(slot.start());
        if (!isFuture(connection, appointmentTime)) {
            throw new AppointmentDAOException(
                    AppointmentDAOException.Reason.SLOT_UNAVAILABLE,
                    "Không thể đặt lịch vào thời điểm đã qua.", null);
        }
        if (!isAssignedDuringSlot(connection, slot)) {
            throw new AppointmentDAOException(
                    AppointmentDAOException.Reason.SCHEDULE_UNAVAILABLE,
                    "Bác sĩ không có lịch phân công đã xác nhận cho khung giờ này.", null);
        }
        ensureCapacityAndNoConflict(connection, slot, draft.maBenhNhan(),
                excludeAppointmentId);
        return slot;
    }

    private void lockActivePatient(Connection connection, String patientId)
            throws SQLException {
        String sql = """
                SELECT MaBenhNhan
                FROM dbo.BenhNhan WITH (UPDLOCK, HOLDLOCK)
                WHERE MaBenhNhan = ? AND TrangThai = N'HoatDong'
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, patientId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new AppointmentDAOException(
                            AppointmentDAOException.Reason.INVALID_REFERENCE,
                            "Bệnh nhân không tồn tại hoặc đang tạm khóa.", null);
                }
            }
        }
    }

    private Long findAppointmentSlot(Connection connection, long appointmentId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT MaKhungGio FROM dbo.LichHen WHERE MaLichHen = ?")) {
            statement.setLong(1, appointmentId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                long slotId = result.getLong(1);
                return result.wasNull() ? null : slotId;
            }
        }
    }

    private void lockDoctorAssignments(
            Connection connection, String doctorId, LocalDate date)
            throws SQLException {
        String dayLockSql = """
                SELECT lp.MaPhanCong
                FROM dbo.LichPhanCong lp WITH (UPDLOCK, HOLDLOCK)
                JOIN dbo.CaLamViec ca ON ca.MaCa = lp.MaCa
                WHERE lp.MaNhanVien = ?
                  AND lp.NgayLam = ?
                  AND lp.TrangThai = N'DaXacNhan'
                """;
        try (PreparedStatement statement = connection.prepareStatement(dayLockSql)) {
            statement.setString(1, doctorId);
            statement.setObject(2, date);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    result.getLong(1);
                }
            }
        }
    }

    private SlotDetails findSlotForUpdate(Connection connection, long slotId)
            throws SQLException {
        String sql = """
                SELECT kg.MaKhungGio, kg.MaNhanVien, bs.MaChuyenKhoa,
                       kg.NgayKham, kg.GioBatDau, kg.GioKetThuc,
                       COALESCE(kg.SoLuongToiDa, 1) AS SoLuongToiDa,
                       kg.TrangThai
                FROM dbo.KhungGioKham kg WITH (UPDLOCK, HOLDLOCK)
                JOIN dbo.BacSi bs ON bs.MaNhanVien = kg.MaNhanVien
                JOIN dbo.NhanVien nv ON nv.MaNhanVien = bs.MaNhanVien
                JOIN dbo.ChuyenKhoa ck ON ck.MaChuyenKhoa = bs.MaChuyenKhoa
                WHERE kg.MaKhungGio = ?
                  AND ck.TrangThai = N'HoatDong'
                  AND nv.TrangThaiCongTac IN (N'ThuViec', N'DangLam')
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, slotId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                return new SlotDetails(
                        result.getLong("MaKhungGio"),
                        result.getString("MaNhanVien"),
                        result.getString("MaChuyenKhoa"),
                        result.getDate("NgayKham").toLocalDate(),
                        result.getTime("GioBatDau").toLocalTime(),
                        result.getTime("GioKetThuc").toLocalTime(),
                        result.getInt("SoLuongToiDa"),
                        result.getString("TrangThai"));
            }
        }
    }

    private boolean isAssignedDuringSlot(Connection connection, SlotDetails slot)
            throws SQLException {
        String sql = """
                SELECT TOP (1) 1
                FROM dbo.LichPhanCong lp
                JOIN dbo.CaLamViec ca ON ca.MaCa = lp.MaCa
                WHERE lp.MaNhanVien = ?
                  AND lp.NgayLam = ?
                  AND lp.TrangThai = N'DaXacNhan'
                  AND ca.TrangThai = N'HoatDong'
                  AND ? >= ca.GioBatDau
                  AND ? <= ca.GioKetThuc
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, slot.doctorId());
            statement.setObject(2, slot.date());
            statement.setObject(3, slot.start());
            statement.setObject(4, slot.end());
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private boolean isFuture(Connection connection, LocalDateTime appointmentTime)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT CASE WHEN ? > SYSDATETIME() THEN 1 ELSE 0 END")) {
            statement.setObject(1, appointmentTime);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() && result.getBoolean(1);
            }
        }
    }

    private void ensureCapacityAndNoConflict(
            Connection connection,
            SlotDetails slot,
            String patientId,
            Long excludeAppointmentId) throws SQLException {
        String capacitySql = """
                SELECT COUNT_BIG(*)
                FROM dbo.LichHen
                WHERE MaKhungGio = ?
                  AND TrangThai IN (%s)
                  AND (? IS NULL OR MaLichHen <> ?)
                """.formatted(ACTIVE_APPOINTMENT_STATES);
        long booked = countForSlot(
                connection, capacitySql, slot.slotId(), excludeAppointmentId);
        if (booked >= slot.capacity()) {
            throw new AppointmentDAOException(
                    AppointmentDAOException.Reason.SLOT_UNAVAILABLE,
                    "Khung giờ vừa hết chỗ. Vui lòng tải lại và chọn giờ khác.", null);
        }
        if (hasOverlap(connection, slot, slot.doctorId(), null, excludeAppointmentId, true)) {
            throw new AppointmentDAOException(
                    AppointmentDAOException.Reason.CONFLICT,
                    "Bác sĩ đã có lịch hẹn khác bị trùng thời gian.", null);
        }
        if (hasOverlap(connection, slot, null, patientId, excludeAppointmentId, false)) {
            throw new AppointmentDAOException(
                    AppointmentDAOException.Reason.CONFLICT,
                    "Bệnh nhân đã có lịch hẹn khác bị trùng thời gian.", null);
        }
    }

    private long countForSlot(
            Connection connection, String sql, long slotId, Long excludeAppointmentId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, slotId);
            setNullableLong(statement, 2, excludeAppointmentId);
            setNullableLong(statement, 3, excludeAppointmentId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong(1);
            }
        }
    }

    private boolean hasOverlap(
            Connection connection,
            SlotDetails slot,
            String doctorId,
            String patientId,
            Long excludeAppointmentId,
            boolean ignoreSameSlot) throws SQLException {
        String column = doctorId == null ? "other.MaBenhNhan" : "other.MaNhanVien";
        String value = doctorId == null ? patientId : doctorId;
        String sql = """
                SELECT TOP (1) 1
                FROM dbo.LichHen other WITH (UPDLOCK, HOLDLOCK)
                LEFT JOIN dbo.KhungGioKham otherSlot
                  ON otherSlot.MaKhungGio = other.MaKhungGio
                WHERE %s = ?
                  AND other.TrangThai IN (%s)
                  AND (? IS NULL OR other.MaLichHen <> ?)
                  AND (? = 0 OR other.MaKhungGio IS NULL
                       OR other.MaKhungGio <> ?)
                  AND other.ThoiGianHen >= ? AND other.ThoiGianHen < ?
                  AND ((otherSlot.MaKhungGio IS NOT NULL
                        AND otherSlot.GioBatDau < ? AND otherSlot.GioKetThuc > ?)
                       OR (otherSlot.MaKhungGio IS NULL AND other.ThoiGianHen = ?))
                """.formatted(column, ACTIVE_APPOINTMENT_STATES);
        LocalDateTime dayStart = slot.date().atStartOfDay();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, value);
            setNullableLong(statement, 2, excludeAppointmentId);
            setNullableLong(statement, 3, excludeAppointmentId);
            statement.setInt(4, ignoreSameSlot ? 1 : 0);
            statement.setLong(5, slot.slotId());
            statement.setObject(6, dayStart);
            statement.setObject(7, dayStart.plusDays(1));
            statement.setObject(8, slot.end());
            statement.setObject(9, slot.start());
            statement.setObject(10, dayStart.plus(slot.start().toSecondOfDay(),
                    java.time.temporal.ChronoUnit.SECONDS));
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private LichHen lockEditableAppointment(Connection connection, long appointmentId)
            throws SQLException {
        String sql = """
                SELECT lh.MaLichHen, lh.MaBenhNhan, bn.HoTen AS TenBenhNhan,
                       bn.SoDienThoai AS SoDienThoaiBenhNhan, lh.MaKhungGio,
                       lh.MaNhanVien, nv.HoTen AS TenBacSi,
                       COALESCE(lh.MaChuyenKhoa, bs.MaChuyenKhoa) AS MaChuyenKhoa,
                       ck.TenChuyenKhoa, lh.ThoiGianHen, lh.LyDoKham, lh.TrangThai,
                       lh.NguoiTao, creator.HoTen AS TenNguoiTao, lh.GhiChu
                FROM dbo.LichHen lh WITH (UPDLOCK, HOLDLOCK)
                JOIN dbo.BenhNhan bn ON bn.MaBenhNhan = lh.MaBenhNhan
                LEFT JOIN dbo.NhanVien nv ON nv.MaNhanVien = lh.MaNhanVien
                LEFT JOIN dbo.BacSi bs ON bs.MaNhanVien = lh.MaNhanVien
                LEFT JOIN dbo.ChuyenKhoa ck
                    ON ck.MaChuyenKhoa = COALESCE(lh.MaChuyenKhoa, bs.MaChuyenKhoa)
                LEFT JOIN dbo.NhanVien creator ON creator.MaNhanVien = lh.NguoiTao
                WHERE lh.MaLichHen = ?
                """;
        LichHen appointment;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, appointmentId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new AppointmentDAOException(
                            AppointmentDAOException.Reason.NOT_FOUND,
                            "Không tìm thấy lịch hẹn.", null);
                }
                appointment = mapAppointment(result);
            }
        }
        if (!isFuture(connection, appointment.thoiGianHen())
                || (!"ChoXacNhan".equals(appointment.trangThai())
                && !"DaXacNhan".equals(appointment.trangThai()))) {
            throw new AppointmentDAOException(
                    AppointmentDAOException.Reason.INVALID_STATE,
                    "Chỉ lịch trong tương lai đang chờ hoặc đã xác nhận mới được sửa/hủy.",
                    null);
        }
        ensureNoVisit(connection, appointmentId);
        return appointment;
    }

    private void ensureNoVisit(Connection connection, long appointmentId)
            throws SQLException {
        String sql = "SELECT TOP (1) 1 FROM dbo.LuotKham WHERE MaLichHen = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, appointmentId);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    throw new AppointmentDAOException(
                            AppointmentDAOException.Reason.INVALID_STATE,
                            "Lịch hẹn đã phát sinh lượt khám nên không thể sửa hoặc hủy.",
                            null);
                }
            }
        }
    }

    private void refreshSlotState(Connection connection, long slotId) throws SQLException {
        String sql = """
                SELECT kg.SoLuongToiDa, kg.TrangThai,
                       (SELECT COUNT_BIG(*)
                        FROM dbo.LichHen lh
                        WHERE lh.MaKhungGio = kg.MaKhungGio
                          AND lh.TrangThai IN (%s)) AS BookedCount
                FROM dbo.KhungGioKham kg WITH (UPDLOCK, HOLDLOCK)
                WHERE kg.MaKhungGio = ?
                """.formatted(ACTIVE_APPOINTMENT_STATES);
        int maximum;
        String status;
        long booked;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, slotId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return;
                }
                maximum = result.getInt("SoLuongToiDa");
                if (result.wasNull()) {
                    maximum = 1;
                }
                status = result.getString("TrangThai");
                booked = result.getLong("BookedCount");
            }
        }
        if ("Khoa".equals(status)) {
            return;
        }
        String newStatus = booked >= maximum ? "Day" : "Mo";
        if (!newStatus.equals(status)) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "UPDATE dbo.KhungGioKham SET TrangThai = ? WHERE MaKhungGio = ?")) {
                statement.setString(1, newStatus);
                statement.setLong(2, slotId);
                statement.executeUpdate();
            }
        }
    }

    private void writeHistory(
            Connection connection, long appointmentId, String changedBy,
            String action, String oldValue, String newValue, String reason)
            throws SQLException {
        String sql = """
                INSERT INTO dbo.LichSuLichHen
                    (MaLichHen, NguoiThayDoi, HanhDong, GiaTriCu, GiaTriMoi, LyDo)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, appointmentId);
            statement.setString(2, changedBy);
            statement.setString(3, action);
            statement.setNString(4, oldValue);
            statement.setNString(5, newValue);
            statement.setNString(6, reason);
            statement.executeUpdate();
        }
    }

    private QueryConditions conditions(AppointmentFilter filter) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> parameters = new ArrayList<>();
        String search = filter.searchText().trim();
        if (!search.isEmpty()) {
            String pattern = "%" + escapeLike(search) + "%";
            where.append("""
                     AND (CONVERT(varchar(20), lh.MaLichHen) LIKE ? ESCAPE '~'
                          OR bn.HoTen LIKE ? ESCAPE '~'
                          OR bn.SoDienThoai LIKE ? ESCAPE '~'
                          OR nv.HoTen LIKE ? ESCAPE '~')
                    """);
            parameters.add(pattern);
            parameters.add(pattern);
            parameters.add(pattern);
            parameters.add(pattern);
        }
        if (filter.fromDate() != null) {
            where.append(" AND lh.ThoiGianHen >= ?");
            parameters.add(filter.fromDate().atStartOfDay());
        }
        if (filter.toDate() != null) {
            where.append(" AND lh.ThoiGianHen < ?");
            parameters.add(filter.toDate().plusDays(1).atStartOfDay());
        }
        if (!filter.status().isBlank()) {
            where.append(" AND lh.TrangThai = ?");
            parameters.add(filter.status());
        }
        if (!filter.doctorId().isBlank()) {
            where.append(" AND lh.MaNhanVien = ?");
            parameters.add(filter.doctorId());
        }
        if (!filter.specialtyId().isBlank()) {
            where.append(" AND COALESCE(lh.MaChuyenKhoa, bs.MaChuyenKhoa) = ?");
            parameters.add(filter.specialtyId());
        }
        return new QueryConditions(where.toString(), List.copyOf(parameters));
    }

    private long executeCount(
            Connection connection, String sql, List<Object> parameters)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters, 1);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong(1);
            }
        }
    }

    private void bind(
            PreparedStatement statement, List<Object> parameters, int first)
            throws SQLException {
        for (int i = 0; i < parameters.size(); i++) {
            Object value = parameters.get(i);
            if (value instanceof LocalDateTime dateTime) {
                statement.setObject(first + i, dateTime);
            } else if (value instanceof String text) {
                statement.setString(first + i, text);
            } else {
                statement.setObject(first + i, value);
            }
        }
    }

    private LichHen mapAppointment(ResultSet result) throws SQLException {
        java.sql.Timestamp timestamp = result.getTimestamp("ThoiGianHen");
        long slotValue = result.getLong("MaKhungGio");
        Long slotId = result.wasNull() ? null : slotValue;
        return new LichHen(
                result.getLong("MaLichHen"),
                result.getString("MaBenhNhan"),
                result.getString("TenBenhNhan"),
                result.getString("SoDienThoaiBenhNhan"),
                slotId,
                result.getString("MaNhanVien"),
                result.getString("TenBacSi"),
                result.getString("MaChuyenKhoa"),
                result.getString("TenChuyenKhoa"),
                timestamp == null ? null : timestamp.toLocalDateTime(),
                result.getString("LyDoKham"),
                result.getString("TrangThai"),
                result.getString("NguoiTao"),
                result.getString("TenNguoiTao"),
                result.getString("GhiChu"));
    }

    private String snapshot(LichHen appointment) {
        return "{\"maBenhNhan\":\"" + json(appointment.maBenhNhan())
                + "\",\"maNhanVien\":\"" + json(appointment.maNhanVien())
                + "\",\"maKhungGio\":\"" + appointment.maKhungGio()
                + "\",\"thoiGianHen\":\"" + json(String.valueOf(appointment.thoiGianHen()))
                + "\",\"lyDoKham\":\"" + json(appointment.lyDoKham())
                + "\",\"trangThai\":\"" + json(appointment.trangThai())
                + "\",\"ghiChu\":\"" + json(appointment.ghiChu()) + "\"}";
    }

    private String snapshot(AppointmentDraft draft, LocalDateTime appointmentTime) {
        return "{\"maBenhNhan\":\"" + json(draft.maBenhNhan())
                + "\",\"maNhanVien\":\"" + json(draft.maNhanVien())
                + "\",\"maChuyenKhoa\":\"" + json(draft.maChuyenKhoa())
                + "\",\"maKhungGio\":\"" + draft.maKhungGio()
                + "\",\"thoiGianHen\":\"" + appointmentTime
                + "\",\"lyDoKham\":\"" + json(draft.lyDoKham())
                + "\",\"ghiChu\":\"" + json(draft.ghiChu()) + "\"}";
    }

    private String json(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private String escapeLike(String value) {
        return value.replace("~", "~~")
                .replace("%", "~%")
                .replace("_", "~_")
                .replace("[", "~[");
    }

    private void beginTransaction(Connection connection) throws SQLException {
        connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        connection.setAutoCommit(false);
    }

    private void rollback(Connection connection, Exception original) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            original.addSuppressed(rollbackException);
        }
    }

    private void setNullableLong(
            PreparedStatement statement, int parameter, Long value)
            throws SQLException {
        if (value == null) {
            statement.setNull(parameter, Types.BIGINT);
        } else {
            statement.setLong(parameter, value);
        }
    }

    private AppointmentDAOException databaseError(String message, SQLException cause) {
        if (cause.getErrorCode() == 1205) {
            return new AppointmentDAOException(
                    AppointmentDAOException.Reason.CONFLICT,
                    "Lịch vừa được cập nhật bởi người dùng khác. Vui lòng tải lại.",
                    cause);
        }
        return new AppointmentDAOException(
                AppointmentDAOException.Reason.DATABASE_ERROR, message, cause);
    }

    private record QueryConditions(String sql, List<Object> parameters) {
    }

    private record SlotDetails(
            long slotId,
            String doctorId,
            String specialtyId,
            LocalDate date,
            LocalTime start,
            LocalTime end,
            int capacity,
            String status) {
    }
}
