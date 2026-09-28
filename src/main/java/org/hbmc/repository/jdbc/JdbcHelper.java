package org.hbmc.repository.jdbc;

import org.hbmc.exception.DataAccessException;
import org.hbmc.repository.RowMapper;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcHelper {

    private final Connection connection;

    public JdbcHelper(Connection connection) {
        this.connection = connection;
    }

    public <T> List<T> queryList(String sql, RowMapper<T> mapper, Object... params) {
        List<T> results = new ArrayList<>();
        try (PreparedStatement stmt = this.connection.prepareStatement(sql)) {
            this.bindParameters(stmt, params);
            try (ResultSet resultSet = stmt.executeQuery()) {
                while (resultSet.next()) {
                    results.add(mapper.map(resultSet));
                }
            }
            return results;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to run query [" + sql + "]: " + e.getMessage(), e);
        }
    }

    public <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) {
        try (PreparedStatement stmt = this.connection.prepareStatement(sql)) {
            this.bindParameters(stmt, params);
            try (ResultSet resultSet = stmt.executeQuery()) {
                return resultSet.next() ? Optional.of(mapper.map(resultSet)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to run query [" + sql + "]: " + e.getMessage(), e);
        }
    }

    public boolean exists(String sql, Object... params) {
        try (PreparedStatement stmt = this.connection.prepareStatement(sql)) {
            this.bindParameters(stmt, params);
            try (ResultSet resultSet = stmt.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to run existence check [" + sql + "]: " + e.getMessage(), e);
        }
    }

    public int update(String sql, Object... params) {
        try (PreparedStatement stmt = this.connection.prepareStatement(sql)) {
            this.bindParameters(stmt, params);
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to run update [" + sql + "]: " + e.getMessage(), e);
        }
    }

    public int insertReturningId(String sql, Object... params) {
        try (PreparedStatement stmt = this.connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            this.bindParameters(stmt, params);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to run insert [" + sql + "]: " + e.getMessage(), e);
        }
    }

    private void bindParameters(PreparedStatement stmt, Object... params) throws SQLException {
        for (int index = 0; index < params.length; index++) {
            int position = index + 1;
            Object value = params[index];
            if (value == null) {
                stmt.setNull(position, Types.NULL);
            } else if (value instanceof Integer integer) {
                stmt.setInt(position, integer);
            } else if (value instanceof Long longValue) {
                stmt.setLong(position, longValue);
            } else if (value instanceof String string) {
                stmt.setString(position, string);
            } else if (value instanceof BigDecimal decimal) {
                stmt.setBigDecimal(position, decimal);
            } else if (value instanceof Boolean flag) {
                stmt.setBoolean(position, flag);
            } else if (value instanceof LocalDate localDate) {
                stmt.setDate(position, Date.valueOf(localDate));
            } else if (value instanceof LocalDateTime localDateTime) {
                stmt.setTimestamp(position, Timestamp.valueOf(localDateTime));
            } else if (value instanceof Enum<?> enumValue) {
                stmt.setString(position, enumValue.name());
            } else {
                throw new DataAccessException(
                        "Unsupported parameter type [" + value.getClass().getName() + "] at position " + position, null);
            }
        }
    }
}
