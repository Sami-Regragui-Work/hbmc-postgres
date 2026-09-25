package org.hbmc.repository.jdbc;

import org.hbmc.db.DatabaseConnection;
import org.hbmc.model.Room;
import org.hbmc.model.enums.RoomStatus;
import org.hbmc.model.enums.RoomType;
import org.hbmc.repository.RoomRepository;
import org.hbmc.repository.RowMapper;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoomRepositoryJDBC implements RoomRepository {

    private final Connection connection;

    private final RowMapper<Room> roomMapper = rs -> {
        Room room = new Room(
                rs.getString("room_number"),
                RoomType.valueOf(rs.getString("type")),
                rs.getInt("capacity"),
                rs.getBigDecimal("price_per_night"),
                RoomStatus.valueOf(rs.getString("status"))
        );
        room.setId(rs.getInt("id"));
        return room;
    };

    public RoomRepositoryJDBC() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public Room save(Room room) {
        String sql = "INSERT INTO rooms (room_number, type, capacity, price_per_night, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, room.getRoomNumber());
            stmt.setString(2, room.getType().name());
            stmt.setInt(3, room.getCapacity());
            stmt.setBigDecimal(4, room.getPricePerNight());
            stmt.setString(5, room.getStatus().name());

            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    room.setId(keys.getInt(1));
                }
            }
            return room;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save room: " + e.getMessage(), e);
        }
    }

    @Override
    public Room update(Room room) {
        String sql = "UPDATE rooms SET type = ?, capacity = ?, price_per_night = ?, status = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, room.getType().name());
            stmt.setInt(2, room.getCapacity());
            stmt.setBigDecimal(3, room.getPricePerNight());
            stmt.setString(4, room.getStatus().name());
            stmt.setInt(5, room.getId());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new RuntimeException("No room found with id: " + room.getId());
            }
            return room;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update room: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Room> findById(int id) {
        String sql = "SELECT * FROM rooms WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(roomMapper.map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find room by id: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Room> findByRoomNumber(String roomNumber) {
        String sql = "SELECT * FROM rooms WHERE room_number = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, roomNumber);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(roomMapper.map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find room by number: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Room> findAll() {
        String sql = "SELECT * FROM rooms";
        List<Room> rooms = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                rooms.add(roomMapper.map(rs));
            }
            return rooms;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all rooms: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Room> findByStatus(RoomStatus status) {
        String sql = "SELECT * FROM rooms WHERE status = ?";
        List<Room> rooms = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rooms.add(roomMapper.map(rs));
                }
            }
            return rooms;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find rooms by status: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Room> findAvailableRooms(LocalDate checkIn, LocalDate checkOut) {
        String sql = """
            SELECT * FROM rooms
            WHERE status != 'MAINTENANCE'
            AND id NOT IN (
                SELECT room_id FROM reservations
                WHERE status != 'CANCELLED'
                AND check_in < ?
                AND check_out > ?
            )
        """;
        List<Room> rooms = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(checkOut));
            stmt.setDate(2, Date.valueOf(checkIn));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    rooms.add(roomMapper.map(rs));
                }
            }
            return rooms;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find available rooms: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean hasOverlap(int roomId, LocalDate checkIn, LocalDate checkOut) {
        String sql = """
            SELECT 1 FROM reservations
            WHERE room_id = ?
            AND status != 'CANCELLED'
            AND check_in < ?
            AND check_out > ?
        """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, roomId);
            stmt.setDate(2, Date.valueOf(checkOut));
            stmt.setDate(3, Date.valueOf(checkIn));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check room overlap: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM rooms WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete room: " + e.getMessage(), e);
        }
    }
}