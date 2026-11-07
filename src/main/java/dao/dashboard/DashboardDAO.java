package dao.dashboard;

import config.DatabaseConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import model.dashboard.DailyRevenueStatistics;
import model.dashboard.DailyVisitStatistics;
import model.dashboard.DashboardData;
import model.dashboard.DashboardStatistics;
import model.dashboard.UpcomingAppointment;

public final class DashboardDAO {
    public DashboardData getDashboardData() {
        try (Connection connection = DatabaseConnection.getConnection()) {
            LocalDateTime databaseNow = getDatabaseTime(connection);
            LocalDate today = databaseNow.toLocalDate();
            return new DashboardData(
                    today,
                    getStatistics(connection, today),
                    getDailyVisits(connection, today),
                    getDailyRevenue(connection, today),
                    getUpcomingAppointments(connection, databaseNow, 5));
        } catch (SQLException exception) {
            throw new DashboardDAOException(
                    "Không thể truy vấn dữ liệu tổng quan phòng khám.", exception);
        }
    }

    private LocalDateTime getDatabaseTime(Connection connection) throws SQLException {
        String sql = "SELECT SYSDATETIME() AS DatabaseNow";
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            if (!result.next()) {
                throw new SQLException("SQL Server không trả về thời gian hiện tại.");
            }
            return result.getObject("DatabaseNow", LocalDateTime.class);
        }
    }

    private DashboardStatistics getStatistics(Connection connection, LocalDate today)
            throws SQLException {
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime startOfTomorrow = today.plusDays(1).atStartOfDay();
        LocalDateTime startOfMonth = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime startOfNextMonth = today.plusMonths(1)
                .withDayOfMonth(1).atStartOfDay();

        String sql = """
                SELECT
                    (SELECT COUNT_BIG(*)
                     FROM dbo.LichHen
                     WHERE ThoiGianHen >= ? AND ThoiGianHen < ?) AS AppointmentsToday,
                    (SELECT COUNT_BIG(*)
                     FROM dbo.LichHen
                     WHERE ThoiGianHen >= ? AND ThoiGianHen < ?
                       AND TrangThai = N'DaXacNhan') AS ConfirmedAppointmentsToday,
                    (SELECT COUNT_BIG(*)
                     FROM dbo.LichHen
                     WHERE ThoiGianHen >= ? AND ThoiGianHen < ?
                       AND TrangThai = N'DaHuy') AS CancelledAppointmentsToday,
                    (SELECT COUNT_BIG(*)
                     FROM dbo.LuotKham
                     WHERE ThoiGianTiepNhan >= ? AND ThoiGianTiepNhan < ?) AS VisitsToday,
                    (SELECT COUNT_BIG(*)
                     FROM dbo.LuotKham
                     WHERE ThoiGianTiepNhan >= ? AND ThoiGianTiepNhan < ?
                       AND TrangThai = N'HoanTat') AS CompletedVisitsToday,
                    (SELECT COUNT_BIG(*)
                     FROM dbo.LuotKham
                     WHERE ThoiGianTiepNhan >= ? AND ThoiGianTiepNhan < ?
                       AND TrangThai IN (N'ChoKham', N'DangKham', N'ChoKetQua'))
                        AS ActiveVisitsToday,
                    (SELECT COUNT_BIG(*) FROM dbo.BenhNhan) AS TotalPatients,
                    (SELECT COUNT_BIG(*)
                     FROM dbo.BenhNhan
                     WHERE NgayTaoHoSo >= ? AND NgayTaoHoSo < ?) AS NewPatientsThisMonth,
                    (SELECT COALESCE(SUM(g.SoTien), 0)
                     FROM dbo.GiaoDichThanhToan g
                     JOIN dbo.HoaDon h ON h.MaHoaDon = g.MaHoaDon
                     WHERE g.ThoiGianGiaoDich >= ? AND g.ThoiGianGiaoDich < ?
                       AND g.TrangThai = N'ThanhCong'
                       AND h.TrangThai NOT IN (N'DaHuy', N'DaHoanTien'))
                        AS RevenueToday,
                    (SELECT COALESCE(SUM(
                         CASE WHEN h.SoTienPhaiTra > COALESCE(p.PaidAmount, 0)
                              THEN h.SoTienPhaiTra - COALESCE(p.PaidAmount, 0)
                              ELSE 0 END), 0)
                     FROM dbo.HoaDon h
                     OUTER APPLY (
                         SELECT SUM(g.SoTien) AS PaidAmount
                         FROM dbo.GiaoDichThanhToan g
                         WHERE g.MaHoaDon = h.MaHoaDon
                           AND g.TrangThai = N'ThanhCong'
                     ) p
                     WHERE h.TrangThai IN (N'ChuaThanhToan', N'ThanhToanMotPhan'))
                        AS OutstandingBalance
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int parameter = 1;
            setRange(statement, parameter, startOfDay, startOfTomorrow);
            parameter += 2;
            setRange(statement, parameter, startOfDay, startOfTomorrow);
            parameter += 2;
            setRange(statement, parameter, startOfDay, startOfTomorrow);
            parameter += 2;
            setRange(statement, parameter, startOfDay, startOfTomorrow);
            parameter += 2;
            setRange(statement, parameter, startOfDay, startOfTomorrow);
            parameter += 2;
            setRange(statement, parameter, startOfDay, startOfTomorrow);
            parameter += 2;
            setRange(statement, parameter, startOfMonth, startOfNextMonth);
            parameter += 2;
            setRange(statement, parameter, startOfDay, startOfTomorrow);

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("SQL Server không trả về số liệu tổng quan.");
                }
                return new DashboardStatistics(
                        result.getLong("AppointmentsToday"),
                        result.getLong("ConfirmedAppointmentsToday"),
                        result.getLong("CancelledAppointmentsToday"),
                        result.getLong("VisitsToday"),
                        result.getLong("CompletedVisitsToday"),
                        result.getLong("ActiveVisitsToday"),
                        result.getLong("TotalPatients"),
                        result.getLong("NewPatientsThisMonth"),
                        result.getBigDecimal("RevenueToday"),
                        result.getBigDecimal("OutstandingBalance"));
            }
        }
    }

    private List<DailyVisitStatistics> getDailyVisits(
            Connection connection, LocalDate today) throws SQLException {
        LocalDate firstDay = today.minusDays(6);
        LocalDateTime start = firstDay.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        String sql = """
                SELECT CAST(ThoiGianTiepNhan AS date) AS StatisticDate,
                       COUNT_BIG(*) AS VisitCount
                FROM dbo.LuotKham
                WHERE ThoiGianTiepNhan >= ? AND ThoiGianTiepNhan < ?
                GROUP BY CAST(ThoiGianTiepNhan AS date)
                ORDER BY StatisticDate
                """;
        Map<LocalDate, Long> counts = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setRange(statement, 1, start, end);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    counts.put(result.getDate("StatisticDate").toLocalDate(),
                            result.getLong("VisitCount"));
                }
            }
        }

        List<DailyVisitStatistics> statistics = new ArrayList<>(7);
        for (int day = 0; day < 7; day++) {
            LocalDate date = firstDay.plusDays(day);
            statistics.add(new DailyVisitStatistics(date, counts.getOrDefault(date, 0L)));
        }
        return List.copyOf(statistics);
    }

    private List<DailyRevenueStatistics> getDailyRevenue(
            Connection connection, LocalDate today) throws SQLException {
        LocalDate firstDay = today.minusDays(6);
        LocalDateTime start = firstDay.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        String sql = """
                SELECT CAST(g.ThoiGianGiaoDich AS date) AS StatisticDate,
                       SUM(g.SoTien) AS Revenue
                FROM dbo.GiaoDichThanhToan g
                JOIN dbo.HoaDon h ON h.MaHoaDon = g.MaHoaDon
                WHERE g.ThoiGianGiaoDich >= ? AND g.ThoiGianGiaoDich < ?
                  AND g.TrangThai = N'ThanhCong'
                  AND h.TrangThai NOT IN (N'DaHuy', N'DaHoanTien')
                GROUP BY CAST(g.ThoiGianGiaoDich AS date)
                ORDER BY StatisticDate
                """;
        Map<LocalDate, BigDecimal> amounts = new HashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setRange(statement, 1, start, end);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    amounts.put(result.getDate("StatisticDate").toLocalDate(),
                            result.getBigDecimal("Revenue"));
                }
            }
        }

        List<DailyRevenueStatistics> statistics = new ArrayList<>(7);
        for (int day = 0; day < 7; day++) {
            LocalDate date = firstDay.plusDays(day);
            statistics.add(new DailyRevenueStatistics(
                    date, amounts.getOrDefault(date, BigDecimal.ZERO)));
        }
        return List.copyOf(statistics);
    }

    private List<UpcomingAppointment> getUpcomingAppointments(
            Connection connection, LocalDateTime databaseNow, int limit) throws SQLException {
        String sql = """
                SELECT TOP (?) lh.MaLichHen, lh.ThoiGianHen, bn.HoTen AS PatientName,
                       COALESCE(nv.HoTen, N'Chưa phân bác sĩ') AS DoctorName,
                       COALESCE(ck.TenChuyenKhoa, N'Chưa phân chuyên khoa') AS Specialty,
                       lh.TrangThai
                FROM dbo.LichHen lh
                JOIN dbo.BenhNhan bn ON bn.MaBenhNhan = lh.MaBenhNhan
                LEFT JOIN dbo.NhanVien nv ON nv.MaNhanVien = lh.MaNhanVien
                LEFT JOIN dbo.BacSi bs ON bs.MaNhanVien = lh.MaNhanVien
                LEFT JOIN dbo.ChuyenKhoa ck
                    ON ck.MaChuyenKhoa = COALESCE(lh.MaChuyenKhoa, bs.MaChuyenKhoa)
                WHERE lh.ThoiGianHen >= ?
                  AND lh.TrangThai NOT IN (N'DaHuy', N'VangMat', N'HoanTat')
                ORDER BY lh.ThoiGianHen, lh.MaLichHen
                """;
        List<UpcomingAppointment> appointments = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            statement.setObject(2, databaseNow);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    appointments.add(new UpcomingAppointment(
                            result.getLong("MaLichHen"),
                            result.getTimestamp("ThoiGianHen").toLocalDateTime(),
                            result.getString("PatientName"),
                            result.getString("DoctorName"),
                            result.getString("Specialty"),
                            result.getString("TrangThai")));
                }
            }
        }
        return List.copyOf(appointments);
    }

    private void setRange(
            PreparedStatement statement, int firstParameter,
            LocalDateTime start, LocalDateTime end) throws SQLException {
        statement.setObject(firstParameter, start);
        statement.setObject(firstParameter + 1, end);
    }
}
