package org.hbmc.repository;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @param <T> T are models
 */
@FunctionalInterface
public interface RowMapper<T> {
    T map(ResultSet resultSet) throws SQLException;
}