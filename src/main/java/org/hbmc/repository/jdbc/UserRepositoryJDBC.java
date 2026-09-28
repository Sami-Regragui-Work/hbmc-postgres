package org.hbmc.repository.jdbc;

import org.hbmc.db.DatabaseConnection;
import org.hbmc.model.Admin;
import org.hbmc.model.Client;
import org.hbmc.model.User;
import org.hbmc.repository.RowMapper;
import org.hbmc.repository.UserRepository;

import java.util.List;
import java.util.Optional;

public class UserRepositoryJDBC implements UserRepository {

    private final JdbcHelper jdbcHelper;

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
        this.jdbcHelper = new JdbcHelper(DatabaseConnection.getInstance().getConnection());
    }

    @Override
    public User save(User user) {
        String sql = "INSERT INTO users (full_name, email, password_hash, salt, role, phone) VALUES (?, ?, ?, ?, ?, ?)";
        int id = this.jdbcHelper.insertReturningId(
                sql,
                user.getFullName(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getSalt(),
                user.getRole(),
                user instanceof Client client ? client.getPhone() : null
        );
        user.setId(id);
        return user;
    }

    @Override
    public User update(User user) {
        String sql = "UPDATE users SET full_name = ?, email = ?, phone = ? WHERE id = ?";
        int rowsAffected = this.jdbcHelper.update(
                sql,
                user.getFullName(),
                user.getEmail(),
                user instanceof Client client ? client.getPhone() : null,
                user.getId()
        );
        if (rowsAffected == 0) {
            throw new RuntimeException("No user found with id: " + user.getId());
        }
        return user;
    }

    @Override
    public void updatePassword(int userId, String newPasswordHash, String newSalt) {
        String sql = "UPDATE users SET password_hash = ?, salt = ? WHERE id = ?";
        int rowsAffected = this.jdbcHelper.update(sql, newPasswordHash, newSalt, userId);
        if (rowsAffected == 0) {
            throw new RuntimeException("No user found with id: " + userId);
        }
    }

    @Override
    public Optional<User> findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        return this.jdbcHelper.queryOne(sql, this.userMapper, id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        return this.jdbcHelper.queryOne(sql, this.userMapper, email);
    }

    @Override
    public boolean existsByEmail(String email) {
        String sql = "SELECT 1 FROM users WHERE email = ?";
        return this.jdbcHelper.exists(sql, email);
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM users";
        return this.jdbcHelper.queryList(sql, this.userMapper);
    }
}
