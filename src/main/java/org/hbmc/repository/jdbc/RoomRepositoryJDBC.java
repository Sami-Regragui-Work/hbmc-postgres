package org.hbmc.repository.jdbc;

import org.hbmc.db.DatabaseConnection;
import org.hbmc.model.Room;
import org.hbmc.model.enums.RoomStatus;
import org.hbmc.model.enums.RoomType;
import org.hbmc.repository.RoomRepository;
import org.hbmc.repository.RowMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class RoomRepositoryJDBC implements RoomRepository {

    private final JdbcHelper jdbcHelper;

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
        this.jdbcHelper = new JdbcHelper(DatabaseConnection.getInstance().getConnection());
    }

    @Override
    public Room save(Room room) {
        String sql = "INSERT INTO rooms (room_number, type, capacity, price_per_night, status) VALUES (?, ?, ?, ?, ?)";
        int id = this.jdbcHelper.insertReturningId(
                sql,
                room.getRoomNumber(),
                room.getType(),
                room.getCapacity(),
                room.getPricePerNight(),
                room.getStatus()
        );
        room.setId(id);
        return room;
    }

    @Override
    public Room update(Room room) {
        String sql = "UPDATE rooms SET type = ?, capacity = ?, price_per_night = ?, status = ? WHERE id = ?";
        int rowsAffected = this.jdbcHelper.update(
                sql,
                room.getType(),
                room.getCapacity(),
                room.getPricePerNight(),
                room.getStatus(),
                room.getId()
        );
        if (rowsAffected == 0) {
            throw new RuntimeException("No room found with id: " + room.getId());
        }
        return room;
    }

    @Override
    public Optional<Room> findById(int id) {
        String sql = "SELECT * FROM rooms WHERE id = ?";
        return this.jdbcHelper.queryOne(sql, this.roomMapper, id);
    }

    @Override
    public Optional<Room> findByRoomNumber(String roomNumber) {
        String sql = "SELECT * FROM rooms WHERE room_number = ?";
        return this.jdbcHelper.queryOne(sql, this.roomMapper, roomNumber);
    }

    @Override
    public List<Room> findAll() {
        String sql = "SELECT * FROM rooms";
        return this.jdbcHelper.queryList(sql, this.roomMapper);
    }

    @Override
    public List<Room> findByStatus(RoomStatus status) {
        String sql = "SELECT * FROM rooms WHERE status = ?";
        return this.jdbcHelper.queryList(sql, this.roomMapper, status);
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
        return this.jdbcHelper.queryList(sql, this.roomMapper, checkOut, checkIn);
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
        return this.jdbcHelper.exists(sql, roomId, checkOut, checkIn);
    }

    @Override
    public void delete(int id) {
        String sql = "DELETE FROM rooms WHERE id = ?";
        this.jdbcHelper.update(sql, id);
    }
}
