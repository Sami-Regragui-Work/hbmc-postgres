package org.hbmc.repository.jdbc;

import org.hbmc.db.DatabaseConnection;
import org.hbmc.model.Admin;
import org.hbmc.model.Client;
import org.hbmc.model.User;
import org.hbmc.repository.RowMapper;
import org.hbmc.repository.UserRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepositoryJDBC implements UserRepository {

    private final Connection connection;

    private final RowMapper<User> userMapper = resultSet -> {
        int id = resultSet.getInt("id");
        String fullName = resultSet.getString("full_name");
        String email = resultSet.getString("email");
        String passwordHash = resultSet.getString("password_hash");
        String salt = resultSet.getString("salt");
        String role = resultSet.getString("role");

        User user;
        if ("ADMIN".equalsIgnoreCase(role)) {
            user = new Admin(fullName, email, passwordHash, salt);
        } else {
            String phone = resultSet.getString("phone");
            user = new Client(fullName, email, passwordHash, salt, phone);
        }
        user.setId(id);
        return user;
    };

    public UserRepositoryJDBC() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public User save(User user) {
        String sql = "INSERT INTO users (full_name, email, password_hash, salt, role, phone) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getFullName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPasswordHash());
            stmt.setString(4, user.getSalt());
            stmt.setString(5, user.getRole());
            stmt.setString(6, user instanceof Client client ? client.getPhone() : null);

            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getInt(1));
                }
            }
            return user;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save user: " + e.getMessage(), e);
        }
    }

    @Override
    public User update(User user) {
        String sql = "UPDATE users SET full_name = ?, email = ?, phone = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getFullName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user instanceof Client client ? client.getPhone() : null);
            stmt.setInt(4, user.getId());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new RuntimeException("No user found with id: " + user.getId());
            }
            return user;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update user: " + e.getMessage(), e);
        }
    }

    @Override
    public void updatePassword(int userId, String newPasswordHash, String newSalt) {
        String sql = "UPDATE users SET password_hash = ?, salt = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, newPasswordHash);
            stmt.setString(2, newSalt);
            stmt.setInt(3, userId);

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new RuntimeException("No user found with id: " + userId);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update password: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(userMapper.map(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by id: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(userMapper.map(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find user by email: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        String sql = "SELECT 1 FROM users WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check user existence: " + e.getMessage(), e);
        }
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM users";
        List<User> users = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                users.add(userMapper.map(rs));
            }
            return users;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all users: " + e.getMessage(), e);
        }
    }
}