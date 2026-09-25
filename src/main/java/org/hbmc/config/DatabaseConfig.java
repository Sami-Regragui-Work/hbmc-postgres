package org.hbmc.config;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class DatabaseConfig {
    public record DbSettings(String urlPrefix, String name, String url, String user, String password) {}

    public static DbSettings resolve() {
        try {
            String dbUrlPrefix = System.getenv("DB_URL_PREFIX");
            String dbName = System.getenv("DB_NAME");
            String dbUser = System.getenv("DB_USER");

            String passwordPath = System.getenv("DB_PASSWORD_FILE");
            String dbPassword = passwordPath != null
                    ? Files.readString(Path.of(passwordPath)).trim()
                    : null;

            if (dbUrlPrefix == null || dbName == null || dbUser == null || dbPassword == null) {
                Properties properties = new Properties();
                try (InputStream input = DatabaseConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
                    properties.load(input);
                }
                dbUrlPrefix = dbUrlPrefix != null ? dbUrlPrefix : properties.getProperty("db.urlPrefix");
                dbName = dbName != null ? dbName : properties.getProperty("db.name");
                dbUser = dbUser != null ? dbUser : properties.getProperty("db.user");
                dbPassword = dbPassword != null ? dbPassword : properties.getProperty("db.password");
            }

            return new DbSettings(dbUrlPrefix, dbName, dbUrlPrefix + dbName, dbUser, dbPassword);
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve DB configuration: " + e.getMessage(), e);
        }
    }
}
