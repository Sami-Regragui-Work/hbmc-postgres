package org.hbmc.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseInitializer {
    public static void initialize() {
        DatabaseConfig.DbSettings config = DatabaseConfig.resolve();

        try (Connection connection = DriverManager.getConnection(config.url(), config.user(), config.password())) {
            System.out.println("Database '" + config.name() + "' already exists. Skipping creation");
        } catch (SQLException e) {
            if (!"3D000".equals(e.getSQLState())) // 3D000 code for NONEXISTENT_DB_SQLSTATE
                throw new RuntimeException("Failed to connect to database '" + config.name() + "': " + e.getMessage(), e);
            DatabaseInitializer.createDatabase(config);
        }
    }

    private static void createDatabase(DatabaseConfig.DbSettings config) {
        String maintenanceUrl = config.urlPrefix() + "postgres";

        try (Connection connection = DriverManager.getConnection(maintenanceUrl, config.user(), config.password())) {
            connection.setAutoCommit(true);
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE DATABASE " + config.name());
                System.out.println("Database '" + config.name() + "' created successfully");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create database '" + config.name() + "': " + e.getMessage(), e);
        }
    }

    private static void createTablesIfNotExist(DatabaseConfig.DbSettings config) {
        String url = config.url();
        try (Connection connection = DriverManager.getConnection(config.url(), config.user(), config.password());
            Statement statement = connection.createStatement()) {

            statement.execute("""
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    salt VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    phone VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS rooms (
    id SERIAL PRIMARY KEY,
    room_number VARCHAR(20) UNIQUE NOT NULL,
    type VARCHAR(20) NOT NULL,
    capacity INT NOT NULL,
    price_per_night NUMERIC(10,2) NOT NULL,
    status VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS reservations (
    id SERIAL PRIMARY KEY,
    client_id INT NOT NULL REFERENCES users(id),
    room_id INT NOT NULL REFERENCES rooms(id),
    reservation_code VARCHAR(50) UNIQUE NOT NULL,
    check_in DATE NOT NULL,
    check_out DATE NOT NULL,
    number_of_guests INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS canceled_reservations (
    id SERIAL PRIMARY KEY,
    reservation_id INT NOT NULL REFERENCES reservations(id),
    canceled_at TIMESTAMP NOT NULL,
    refund_amount NUMERIC(10,2) NOT NULL,
    type VARCHAR(30) NOT NULL
);

CREATE TABLE IF NOT EXISTS payments (
    id SERIAL PRIMARY KEY,
    reservation_id INT NOT NULL REFERENCES reservations(id),
    total NUMERIC(10,2) NOT NULL,
    payment_date DATE NOT NULL,
    method VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS invoices (
    id SERIAL PRIMARY KEY,
    payment_id INT NOT NULL REFERENCES payments(id),
    off_tax NUMERIC(10,2) NOT NULL,
    tax NUMERIC(10,2) NOT NULL
);
""");
            System.out.println("Tables verified/created successfully");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create tables: " + e.getMessage(), e);        }
        }
}
