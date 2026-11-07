package dao.patient;

import config.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import model.patient.BenhNhan;
import model.patient.PatientFilter;
import model.patient.PatientPage;

public final class BenhNhanDAO {
    public PatientPage search(PatientFilter filter) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<String> searchParameters = new ArrayList<>();
        if (!filter.searchText().isBlank()) {
            where.append(" AND (MaBenhNhan LIKE ? ESCAPE '~'")
                    .append(" OR HoTen LIKE ? ESCAPE '~'")
                    .append(" OR SoDienThoai LIKE ? ESCAPE '~'")
                    .append(" OR Email LIKE ? ESCAPE '~'")
                    .append(" OR SoGiayTo LIKE ? ESCAPE '~')");
            String pattern = "%" + escapeLike(filter.searchText().trim()) + "%";
            searchParameters.add(pattern);
            searchParameters.add(pattern);
            searchParameters.add(pattern);
            searchParameters.add(pattern);
            searchParameters.add(pattern);
        }
        if (!filter.gender().isBlank()) {
            where.append(" AND GioiTinh = ?");
        }
        if (!filter.status().isBlank()) {
            where.append(" AND TrangThai = ?");
        }

        String countSql = "SELECT COUNT_BIG(*) FROM dbo.BenhNhan" + where;
        String pageSql = """
                SELECT MaBenhNhan, HoTen, NgaySinh, GioiTinh, SoGiayTo,
                       SoDienThoai, Email, DiaChi, NgayTaoHoSo, TrangThai
                FROM dbo.BenhNhan
                """ + where + """
                 ORDER BY MaBenhNhan
                 OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """;
        try (Connection connection = DatabaseConnection.getConnection()) {
            long totalCount = count(connection, countSql, filter, searchParameters);
            List<BenhNhan> patients = findPage(
                    connection, pageSql, filter, searchParameters);
            return new PatientPage(
                    patients, totalCount, filter.page(), filter.pageSize());
        } catch (SQLException exception) {
            throw databaseException("Không thể tải danh sách bệnh nhân.", exception);
        }
    }

    public BenhNhan findById(String patientId) {
        String sql = """
                SELECT MaBenhNhan, HoTen, NgaySinh, GioiTinh, SoGiayTo,
                       SoDienThoai, Email, DiaChi, NgayTaoHoSo, TrangThai
                FROM dbo.BenhNhan
                WHERE MaBenhNhan = ?
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, patientId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? mapPatient(result) : null;
            }
        } catch (SQLException exception) {
            throw databaseException("Không thể tải hồ sơ bệnh nhân.", exception);
        }
    }

    public String insert(BenhNhan patient) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            connection.setAutoCommit(false);
            try {
                String patientId = generatePatientId(connection);
                String sql = """
                        INSERT INTO dbo.BenhNhan
                            (MaBenhNhan, HoTen, NgaySinh, GioiTinh, SoGiayTo,
                             SoDienThoai, Email, DiaChi)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """;
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, patientId);
                    bindEditableFields(statement, patient, 2);
                    statement.executeUpdate();
                }
                connection.commit();
                return patientId;
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            throw databaseException("Không thể thêm bệnh nhân.", exception);
        }
    }

    public void update(BenhNhan patient) {
        String sql = """
                UPDATE dbo.BenhNhan
                SET HoTen = ?, NgaySinh = ?, GioiTinh = ?, SoGiayTo = ?,
                    SoDienThoai = ?, Email = ?, DiaChi = ?
                WHERE MaBenhNhan = ?
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindEditableFields(statement, patient, 1);
            statement.setString(8, patient.maBenhNhan());
            if (statement.executeUpdate() != 1) {
                throw new PatientDAOException(
                        PatientDAOException.Reason.NOT_FOUND,
                        "Không tìm thấy hồ sơ bệnh nhân cần cập nhật.", null);
            }
        } catch (PatientDAOException exception) {
            throw exception;
        } catch (SQLException exception) {
            throw databaseException("Không thể cập nhật hồ sơ bệnh nhân.", exception);
        }
    }

    public void updateStatus(String patientId, String status) {
        String sql = "UPDATE dbo.BenhNhan SET TrangThai = ? WHERE MaBenhNhan = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setString(2, patientId);
            if (statement.executeUpdate() != 1) {
                throw new PatientDAOException(
                        PatientDAOException.Reason.NOT_FOUND,
                        "Không tìm thấy hồ sơ bệnh nhân cần cập nhật trạng thái.", null);
            }
        } catch (PatientDAOException exception) {
            throw exception;
        } catch (SQLException exception) {
            throw databaseException("Không thể cập nhật trạng thái hồ sơ bệnh nhân.", exception);
        }
    }

    private long count(
            Connection connection,
            String sql,
            PatientFilter filter,
            List<String> searchParameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindFilters(statement, filter, searchParameters, 1);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("SQL Server không trả về số lượng bệnh nhân.");
                }
                return result.getLong(1);
            }
        }
    }

    private List<BenhNhan> findPage(
            Connection connection,
            String sql,
            PatientFilter filter,
            List<String> searchParameters) throws SQLException {
        List<BenhNhan> patients = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            int nextParameter = bindFilters(statement, filter, searchParameters, 1);
            statement.setLong(nextParameter++, (long) filter.page() * filter.pageSize());
            statement.setInt(nextParameter, filter.pageSize());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    patients.add(mapPatient(result));
                }
            }
        }
        return List.copyOf(patients);
    }

    private int bindFilters(
            PreparedStatement statement,
            PatientFilter filter,
            List<String> searchParameters,
            int firstParameter) throws SQLException {
        int parameter = firstParameter;
        for (String searchParameter : searchParameters) {
            statement.setString(parameter++, searchParameter);
        }
        if (!filter.gender().isBlank()) {
            statement.setString(parameter++, filter.gender());
        }
        if (!filter.status().isBlank()) {
            statement.setString(parameter++, filter.status());
        }
        return parameter;
    }

    private BenhNhan mapPatient(ResultSet result) throws SQLException {
        java.sql.Date dateOfBirth = result.getDate("NgaySinh");
        java.sql.Timestamp createdAt = result.getTimestamp("NgayTaoHoSo");
        return new BenhNhan(
                result.getString("MaBenhNhan"),
                result.getString("HoTen"),
                dateOfBirth == null ? null : dateOfBirth.toLocalDate(),
                result.getString("GioiTinh"),
                result.getString("SoGiayTo"),
                result.getString("SoDienThoai"),
                result.getString("Email"),
                result.getString("DiaChi"),
                createdAt == null ? null : createdAt.toLocalDateTime(),
                result.getString("TrangThai"));
    }

    private void bindEditableFields(
            PreparedStatement statement, BenhNhan patient, int firstParameter)
            throws SQLException {
        statement.setNString(firstParameter, patient.hoTen());
        if (patient.ngaySinh() == null) {
            statement.setNull(firstParameter + 1, java.sql.Types.DATE);
        } else {
            statement.setDate(firstParameter + 1, java.sql.Date.valueOf(patient.ngaySinh()));
        }
        statement.setNString(firstParameter + 2, patient.gioiTinh());
        statement.setString(firstParameter + 3, patient.soGiayTo());
        statement.setString(firstParameter + 4, patient.soDienThoai());
        statement.setString(firstParameter + 5, patient.email());
        statement.setNString(firstParameter + 6, patient.diaChi());
    }

    private String generatePatientId(Connection connection) throws SQLException {
        String sql = """
                SELECT COALESCE(MAX(TRY_CONVERT(BIGINT, SUBSTRING(MaBenhNhan, 3, 18))), 0)
                FROM dbo.BenhNhan WITH (UPDLOCK, HOLDLOCK)
                WHERE MaBenhNhan LIKE 'BN%'
                  AND LEN(MaBenhNhan) > 2
                  AND SUBSTRING(MaBenhNhan, 3, 18) NOT LIKE '%[^0-9]%'
                """;
        long nextId;
        try (PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            if (!result.next()) {
                throw new SQLException("Không thể xác định mã bệnh nhân tiếp theo.");
            }
            nextId = result.getLong(1) + 1;
        }
        if (nextId > 999_999_999_999_999_999L) {
            throw new SQLException("Đã vượt quá phạm vi mã bệnh nhân BN.");
        }
        return "BN" + String.format(java.util.Locale.ROOT, "%03d", nextId);
    }

    private String escapeLike(String value) {
        return value.replace("~", "~~")
                .replace("%", "~%")
                .replace("_", "~_")
                .replace("[", "~[");
    }

    private PatientDAOException databaseException(String message, SQLException exception) {
        if (exception.getErrorCode() == 2601 || exception.getErrorCode() == 2627) {
            return new PatientDAOException(
                    PatientDAOException.Reason.DUPLICATE_DOCUMENT,
                    "Số giấy tờ này đã được sử dụng cho hồ sơ bệnh nhân khác.", exception);
        }
        return new PatientDAOException(
                PatientDAOException.Reason.DATABASE_ERROR, message, exception);
    }

    private void rollback(Connection connection, SQLException originalException) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            originalException.addSuppressed(rollbackException);
        }
    }
}
