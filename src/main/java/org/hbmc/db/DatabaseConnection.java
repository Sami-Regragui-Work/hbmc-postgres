package org.hbmc.db;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class DatabaseConnection {
    private static volatile DatabaseConnection instance;
    private final Connection connection;

    private DatabaseConnection() {
        try {
            String dbUrl = System.getenv("DB_URL");
            String dbUser = System.getenv("DB_USER");

            String passwordPath = System.getenv("DB_PASSWORD_FILE");
            String dbPassword = passwordPath != null ? Files.readString(Path.of(passwordPath)).trim() : null;

            if (dbUrl == null || dbUser == null || dbPassword == null) {
                Properties properties = new Properties();
                try (InputStream input = this.getClass().getClassLoader().getResourceAsStream("db.properties")) {
                    properties.load(input);
                }
                dbUrl = dbUrl != null ? dbUrl : properties.getProperty("db.url");
                dbUser = dbUser != null ? dbUser : properties.getProperty("db.user");
                dbPassword = dbPassword != null ? dbPassword : properties.getProperty("db.password");
            }

            this.connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        } catch (Exception e) {
            throw new RuntimeException("Critical error of JDBC connection: " + e.getMessage(), e);
        }

    }

    public static DatabaseConnection getInstance() {
        if (DatabaseConnection.instance == null)
            synchronized (DatabaseConnection.class) {
                if (DatabaseConnection.instance == null)
                    DatabaseConnection.instance = new DatabaseConnection();
            }
        return DatabaseConnection.instance;
    }

    public Connection getConnection() {
        return connection;
    }
}
