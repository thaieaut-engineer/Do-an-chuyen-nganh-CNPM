/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class DatabaseConnection {
    private static volatile Properties cachedProperties;
    private static volatile boolean driverLoaded;

    public static Connection getConnection() throws SQLException {
        Properties properties = loadProperties();

        String url = properties.getProperty("db.url");
        String user = properties.getProperty("db.user");
        String password = properties.getProperty("db.password");
        if (url == null || url.isBlank() || user == null || password == null) {
            throw new SQLException("Thiếu db.url, db.user hoặc db.password trong db.properties.");
        }
        if (!url.startsWith("jdbc:sqlserver:")) {
            throw new SQLException("db.url phải là JDBC URL SQL Server, bắt đầu bằng jdbc:sqlserver:.");
        }
        ensureDriverLoaded();
        return DriverManager.getConnection(url, user, password);
    }

    private static void ensureDriverLoaded() throws SQLException {
        if (driverLoaded) {
            return;
        }
        synchronized (DatabaseConnection.class) {
            if (driverLoaded) {
                return;
            }
            try {
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                driverLoaded = true;
            } catch (ClassNotFoundException e) {
                throw new SQLException("Không tìm thấy Microsoft SQL Server JDBC Driver.", e);
            }
        }
    }

    private static Properties loadProperties() throws SQLException {
        Properties properties = cachedProperties;
        if (properties != null) {
            return properties;
        }
        synchronized (DatabaseConnection.class) {
            if (cachedProperties != null) {
                return cachedProperties;
            }

            Properties loaded = new Properties();
            List<Path> candidates = new ArrayList<>();
            Path userDir = Path.of(System.getProperty("user.dir", ""));
            if (!userDir.toString().isBlank()) {
                candidates.add(userDir.resolve("db.properties"));
                candidates.add(userDir.resolve("src").resolve("main").resolve("resources").resolve("db.properties"));
            }
            Path workingDir = Path.of("").toAbsolutePath().normalize();
            candidates.add(workingDir.resolve("db.properties"));
            candidates.add(Path.of("db.properties"));

            SQLException lastError = null;
            for (Path configPath : candidates) {
                if (configPath == null) {
                    continue;
                }
                if (!Files.exists(configPath)) {
                    continue;
                }
                try (InputStream input = Files.newInputStream(configPath)) {
                    loaded.load(input);
                    cachedProperties = loaded;
                    return loaded;
                } catch (IOException e) {
                    lastError = new SQLException("Không thể đọc db.properties tại " + configPath, e);
                }
            }

            String classpathPath = "db.properties";
            try (InputStream input = DatabaseConnection.class.getClassLoader().getResourceAsStream(classpathPath)) {
                if (input != null) {
                    loaded.load(input);
                    cachedProperties = loaded;
                    return loaded;
                }
            } catch (IOException e) {
                lastError = new SQLException("Không thể đọc db.properties từ classpath.", e);
            }

            StringBuilder searched = new StringBuilder();
            for (Path configPath : candidates) {
                if (searched.length() > 0) {
                    searched.append(", ");
                }
                searched.append(configPath.toAbsolutePath().normalize());
            }
            throw new SQLException(
                    "Không tìm thấy db.properties. Hãy tạo file từ db.properties.example và đặt trong thư mục dự án. Các vị trí đã kiểm tra: "
                            + searched + ".",
                    lastError);
        }
    }
}