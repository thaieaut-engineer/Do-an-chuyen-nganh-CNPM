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
        if (!url.startsWith("jdbc:mysql:")) {
            throw new SQLException("db.url phải là JDBC URL MySQL, bắt đầu bằng jdbc:mysql:.");
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
                Class.forName("com.mysql.cj.jdbc.Driver");
                driverLoaded = true;
            } catch (ClassNotFoundException e) {
                throw new SQLException("Không tìm thấy MySQL JDBC Driver.", e);
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
            Path configPath = Path.of("db.properties");
            try (InputStream input = Files.newInputStream(configPath)) {
                loaded.load(input);
            } catch (IOException e) {
                throw new SQLException("Không thể đọc db.properties. Hãy tạo file từ db.properties.example.", e);
            }
            cachedProperties = loaded;
            return loaded;
        }
    }
}