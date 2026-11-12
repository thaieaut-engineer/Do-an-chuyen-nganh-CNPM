package dao.reception;

import config.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import model.appointment.AppointmentDoctor;
import model.reception.ReceptionAppointment;
import model.reception.ReceptionStats;
import model.reception.ReceptionVisit;

public final class TiepNhanDAO {
    public ReceptionStats getStats() {
        String sql = """
                SELECT
                    (SELECT COUNT(*)
                     FROM dbo.LichHen lh
                     WHERE lh.ThoiGianHen >= CAST(SYSDATETIME() AS date)
                       AND lh.ThoiGianHen < DATEADD(day, 1, CAST(SYSDATETIME() AS date))) AS TotalToday,
                    (SELECT COUNT(*)
                     FROM dbo.LichHen lh
                     WHERE lh.ThoiGianHen >= CAST(SYSDATETIME() AS date)
                       AND lh.ThoiGianHen < DATEADD(day, 1, CAST(SYSDATETIME() AS date))
                       AND lh.TrangThai IN (N'ChoXacNhan', N'DaXacNhan', N'DaDen')
                       AND NOT EXISTS (SELECT 1 FROM dbo.LuotKham lv WHERE lv.MaLichHen = lh.MaLichHen)) AS WaitingForReception,
                    (SELECT COUNT(*)
                     FROM dbo.LuotKham lv
                     WHERE lv.ThoiGianTiepNhan >= CAST(SYSDATETIME() AS date)
                       AND lv.ThoiGianTiepNhan < DATEADD(day, 1, CAST(SYSDATETIME() AS date))) AS ReceivedToday,
                    (SELECT COUNT(*)
                     FROM dbo.LichHen lh
                     WHERE lh.ThoiGianHen >= CAST(SYSDATETIME() AS date)
                       AND lh.ThoiGianHen < DATEADD(day, 1, CAST(SYSDATETIME() AS date))
                       AND lh.TrangThai IN (N'DaHuy', N'VangMat')) AS CancelledOrNoShow
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            if (!result.next()) {
                return new ReceptionStats(0, 0, 0, 0);
            }
            return new ReceptionStats(
                    result.getLong("TotalToday"),
                    result.getLong("WaitingForReception"),
                    result.getLong("ReceivedToday"),
                    result.getLong("CancelledOrNoShow"));
        } catch (SQLException exception) {
            throw new RuntimeException("Không thể tải thống kê tiếp nhận từ SQL Server.", exception);
        }
    }

    public List<ReceptionAppointment> listAppointments(
            boolean receivedOnly,
            String searchText,
            String status,
            LocalDate date,
            String doctorId) {
        String sql = """
                SELECT lh.MaLichHen, lh.MaBenhNhan, bn.HoTen AS HoTenBenhNhan,
                       bn.SoDienThoai AS SoDienThoaiBenhNhan, lh.MaNhanVien,
                       nv.HoTen AS TenBacSi, COALESCE(lh.MaChuyenKhoa, bs.MaChuyenKhoa) AS MaChuyenKhoa,
                       ck.TenChuyenKhoa, lh.ThoiGianHen, lh.LyDoKham, lh.TrangThai,
                       lv.MaLuotKham, lv.ThoiGianTiepNhan, lv.TrangThai AS TrangThaiLuotKham
                FROM dbo.LichHen lh
                JOIN dbo.BenhNhan bn ON bn.MaBenhNhan = lh.MaBenhNhan
                LEFT JOIN dbo.NhanVien nv ON nv.MaNhanVien = lh.MaNhanVien
                LEFT JOIN dbo.BacSi bs ON bs.MaNhanVien = lh.MaNhanVien
                LEFT JOIN dbo.ChuyenKhoa ck ON ck.MaChuyenKhoa = COALESCE(lh.MaChuyenKhoa, bs.MaChuyenKhoa)
                LEFT JOIN dbo.LuotKham lv ON lv.MaLichHen = lh.MaLichHen
                WHERE (? = N'' OR lh.MaLichHen LIKE ? ESCAPE '~'
                    OR lh.MaBenhNhan LIKE ? ESCAPE '~'
                    OR bn.HoTen LIKE ? ESCAPE '~'
                    OR bn.SoDienThoai LIKE ? ESCAPE '~')
                  AND (? = N'' OR lh.TrangThai = ?)
                  AND (? IS NULL OR CAST(lh.ThoiGianHen AS date) = ?)
                  AND (? = N'' OR lh.MaNhanVien = ?)
                  AND (CASE WHEN ? = 1 THEN lv.MaLuotKham IS NOT NULL ELSE lv.MaLuotKham IS NULL END)
                  AND lh.TrangThai IN (N'ChoXacNhan', N'DaXacNhan', N'DaDen')
                ORDER BY lh.ThoiGianHen, lh.MaLichHen
                """;
        String normalizedSearch = searchText == null ? "" : searchText.trim();
        String pattern = "%" + escapeLike(normalizedSearch) + "%";
        List<ReceptionAppointment> list = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int param = 1;
            statement.setNString(param++, normalizedSearch);
            statement.setString(param++, pattern);
            statement.setString(param++, pattern);
            statement.setString(param++, pattern);
            statement.setString(param++, pattern);
            statement.setString(param++, pattern);
            statement.setNString(param++, status == null ? "" : status);
            statement.setNString(param++, status == null ? "" : status);
            if (date == null) {
                statement.setNull(param++, java.sql.Types.DATE);
            } else {
                statement.setDate(param++, java.sql.Date.valueOf(date));
            }
            if (date == null) {
                statement.setNull(param++, java.sql.Types.DATE);
            } else {
                statement.setDate(param++, java.sql.Date.valueOf(date));
            }
            statement.setString(param++, doctorId == null ? "" : doctorId);
            statement.setString(param++, doctorId == null ? "" : doctorId);
            statement.setInt(param++, receivedOnly ? 1 : 0);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    list.add(new ReceptionAppointment(
                            result.getLong("MaLichHen"),
                            result.getString("MaBenhNhan"),
                            result.getString("HoTenBenhNhan"),
                            result.getString("SoDienThoaiBenhNhan"),
                            result.getString("MaNhanVien"),
                            result.getString("TenBacSi"),
                            result.getString("MaChuyenKhoa"),
                            result.getString("TenChuyenKhoa"),
                            result.getTimestamp("ThoiGianHen") == null ? null : result.getTimestamp("ThoiGianHen").toLocalDateTime(),
                            result.getString("LyDoKham"),
                            result.getString("TrangThai"),
                            result.getObject("MaLuotKham") == null ? null : result.getLong("MaLuotKham"),
                            result.getTimestamp("ThoiGianTiepNhan") == null ? null : result.getTimestamp("ThoiGianTiepNhan").toLocalDateTime(),
                            result.getString("TrangThaiLuotKham")));
                }
            }
            return list;
        } catch (SQLException exception) {
            throw new RuntimeException("Không thể tải danh sách lịch hẹn tiếp nhận.", exception);
        }
    }

    public List<ReceptionVisit> listReceivedVisits(String searchText, LocalDate date) {
        String sql = """
                SELECT lv.MaLuotKham, lh.MaLichHen, lv.MaBenhNhan, bn.HoTen AS HoTenBenhNhan,
                       lv.MaNhanVien, nv.HoTen AS TenBacSi, lv.ThoiGianTiepNhan, lv.TrangThai
                FROM dbo.LuotKham lv
                JOIN dbo.BenhNhan bn ON bn.MaBenhNhan = lv.MaBenhNhan
                LEFT JOIN dbo.LichHen lh ON lh.MaLichHen = lv.MaLichHen
                LEFT JOIN dbo.NhanVien nv ON nv.MaNhanVien = lv.MaNhanVien
                WHERE (? = N'' OR lv.MaLuotKham LIKE ? ESCAPE '~' OR lh.MaLichHen LIKE ? ESCAPE '~'
                    OR lv.MaBenhNhan LIKE ? ESCAPE '~' OR bn.HoTen LIKE ? ESCAPE '~')
                  AND (? IS NULL OR CAST(lv.ThoiGianTiepNhan AS date) = ?)
                ORDER BY lv.ThoiGianTiepNhan DESC, lv.MaLuotKham DESC
                """;
        String normalized = searchText == null ? "" : searchText.trim();
        String pattern = "%" + escapeLike(normalized) + "%";
        List<ReceptionVisit> visits = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            int param = 1;
            statement.setNString(param++, normalized);
            statement.setString(param++, pattern);
            statement.setString(param++, pattern);
            statement.setString(param++, pattern);
            statement.setString(param++, pattern);
            statement.setString(param++, pattern);
            if (date == null) {
                statement.setNull(param++, java.sql.Types.DATE);
            } else {
                statement.setDate(param++, java.sql.Date.valueOf(date));
            }
            if (date == null) {
                statement.setNull(param++, java.sql.Types.DATE);
            } else {
                statement.setDate(param++, java.sql.Date.valueOf(date));
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    visits.add(new ReceptionVisit(
                            result.getLong("MaLuotKham"),
                            result.getLong("MaLichHen"),
                            result.getString("MaBenhNhan"),
                            result.getString("HoTenBenhNhan"),
                            result.getString("MaNhanVien"),
                            result.getString("TenBacSi"),
                            result.getTimestamp("ThoiGianTiepNhan") == null ? null : result.getTimestamp("ThoiGianTiepNhan").toLocalDateTime(),
                            result.getString("TrangThai")));
                }
            }
            return visits;
        } catch (SQLException exception) {
            throw new RuntimeException("Không thể tải lượt khám đã tiếp nhận.", exception);
        }
    }

    public LocalDate findLatestAppointmentDate() {
        String sql = """
                SELECT CAST(ThoiGianHen AS date) AS NgayHen
                FROM dbo.LichHen
                ORDER BY ThoiGianHen DESC
                OFFSET 0 ROWS FETCH NEXT 1 ROWS ONLY
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            if (!result.next()) {
                return null;
            }
            java.sql.Date date = result.getDate("NgayHen");
            return date == null ? null : date.toLocalDate();
        } catch (SQLException exception) {
            throw new RuntimeException("Không thể tải ngày lịch hẹn gần nhất.", exception);
        }
    }

    public List<String> findStatuses() {
        String sql = "SELECT DISTINCT TrangThai FROM dbo.LichHen WHERE TrangThai IN (N'ChoXacNhan', N'DaXacNhan', N'DaDen', N'DangKham', N'HoanTat', N'DaHuy', N'VangMat') ORDER BY TrangThai";
        List<String> statuses = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                statuses.add(result.getString(1));
            }
            return statuses;
        } catch (SQLException exception) {
            throw new RuntimeException("Không thể tải trạng thái lịch hẹn.", exception);
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
            return doctors;
        } catch (SQLException exception) {
            throw new RuntimeException("Không thể tải danh sách bác sĩ.", exception);
        }
    }

    public ReceptionAppointment getAppointmentDetail(long appointmentId) {
        String sql = """
                SELECT lh.MaLichHen, lh.MaBenhNhan, bn.HoTen AS HoTenBenhNhan,
                       bn.SoDienThoai AS SoDienThoaiBenhNhan, lh.MaNhanVien, nv.HoTen AS TenBacSi,
                       COALESCE(lh.MaChuyenKhoa, bs.MaChuyenKhoa) AS MaChuyenKhoa,
                       ck.TenChuyenKhoa, lh.ThoiGianHen, lh.LyDoKham, lh.TrangThai,
                       lv.MaLuotKham, lv.ThoiGianTiepNhan, lv.TrangThai AS TrangThaiLuotKham
                FROM dbo.LichHen lh
                JOIN dbo.BenhNhan bn ON bn.MaBenhNhan = lh.MaBenhNhan
                LEFT JOIN dbo.NhanVien nv ON nv.MaNhanVien = lh.MaNhanVien
                LEFT JOIN dbo.BacSi bs ON bs.MaNhanVien = lh.MaNhanVien
                LEFT JOIN dbo.ChuyenKhoa ck ON ck.MaChuyenKhoa = COALESCE(lh.MaChuyenKhoa, bs.MaChuyenKhoa)
                LEFT JOIN dbo.LuotKham lv ON lv.MaLichHen = lh.MaLichHen
                WHERE lh.MaLichHen = ?
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, appointmentId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return null;
                }
                return new ReceptionAppointment(
                        result.getLong("MaLichHen"),
                        result.getString("MaBenhNhan"),
                        result.getString("HoTenBenhNhan"),
                        result.getString("SoDienThoaiBenhNhan"),
                        result.getString("MaNhanVien"),
                        result.getString("TenBacSi"),
                        result.getString("MaChuyenKhoa"),
                        result.getString("TenChuyenKhoa"),
                        result.getTimestamp("ThoiGianHen") == null ? null : result.getTimestamp("ThoiGianHen").toLocalDateTime(),
                        result.getString("LyDoKham"),
                        result.getString("TrangThai"),
                        result.getObject("MaLuotKham") == null ? null : result.getLong("MaLuotKham"),
                        result.getTimestamp("ThoiGianTiepNhan") == null ? null : result.getTimestamp("ThoiGianTiepNhan").toLocalDateTime(),
                        result.getString("TrangThaiLuotKham"));
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Không thể tải chi tiết lịch hẹn.", exception);
        }
    }

    public long createVisit(long appointmentId, String employeeId) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String checkSql = "SELECT MaBenhNhan, MaNhanVien, TrangThai FROM dbo.LichHen WITH (UPDLOCK, HOLDLOCK) WHERE MaLichHen = ?";
                String patientId = null;
                String doctorId = null;
                String status = null;
                try (PreparedStatement checkStatement = connection.prepareStatement(checkSql)) {
                    checkStatement.setLong(1, appointmentId);
                    try (ResultSet result = checkStatement.executeQuery()) {
                        if (!result.next()) {
                            throw new IllegalStateException("Không tìm thấy lịch hẹn cần tiếp nhận.");
                        }
                        patientId = result.getString("MaBenhNhan");
                        doctorId = result.getString("MaNhanVien");
                        status = result.getString("TrangThai");
                        if (!"ChoXacNhan".equals(status) && !"DaXacNhan".equals(status) && !"DaDen".equals(status)) {
                            throw new IllegalStateException("Lịch hẹn không ở trạng thái đủ điều kiện để tiếp nhận.");
                        }
                    }
                }

                String duplicateSql = "SELECT TOP (1) 1 FROM dbo.LuotKham WITH (UPDLOCK, HOLDLOCK) WHERE MaLichHen = ?";
                try (PreparedStatement duplicateStatement = connection.prepareStatement(duplicateSql)) {
                    duplicateStatement.setLong(1, appointmentId);
                    try (ResultSet result = duplicateStatement.executeQuery()) {
                        if (result.next()) {
                            throw new IllegalStateException("Lịch hẹn này đã được tiếp nhận trước đó.");
                        }
                    }
                }

                String insertSql = """
                        INSERT INTO dbo.LuotKham (MaBenhNhan, MaLichHen, MaNhanVien, ThoiGianTiepNhan, TrangThai, GhiChu)
                        VALUES (?, ?, ?, SYSDATETIME(), N'ChoKham', ?)
                        """;
                long visitId;
                try (PreparedStatement insert = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    insert.setString(1, patientId);
                    insert.setLong(2, appointmentId);
                    insert.setString(3, doctorId);
                    insert.setNString(4, "Tiếp nhận từ lịch hẹn bởi nhân viên " + employeeId);
                    insert.executeUpdate();
                    try (ResultSet generatedKeys = insert.getGeneratedKeys()) {
                        if (!generatedKeys.next()) {
                            throw new IllegalStateException("Không nhận được mã lượt khám mới.");
                        }
                        visitId = generatedKeys.getLong(1);
                    }
                }

                String updateAppointmentSql = "UPDATE dbo.LichHen SET TrangThai = N'DaDen' WHERE MaLichHen = ?";
                try (PreparedStatement update = connection.prepareStatement(updateAppointmentSql)) {
                    update.setLong(1, appointmentId);
                    if (update.executeUpdate() != 1) {
                        throw new IllegalStateException("Cập nhật trạng thái lịch hẹn thất bại.");
                    }
                }
                connection.commit();
                return visitId;
            } catch (Exception exception) {
                connection.rollback();
                if (exception instanceof IllegalStateException) {
                    throw new RuntimeException(exception.getMessage(), exception);
                }
                throw new RuntimeException("Không thể tạo lượt khám từ lịch hẹn.", exception);
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new RuntimeException("Không thể kết nối database để tiếp nhận bệnh nhân.", exception);
        }
    }

    private String escapeLike(String input) {
        return input == null ? "" : input.replace("~", "~~").replace("%", "~%").replace("_", "~_");
    }
}
