package org.hbmc.repository.jdbc;

import org.hbmc.db.DatabaseConnection;
import org.hbmc.model.CanceledReservation;
import org.hbmc.model.Client;
import org.hbmc.model.Reservation;
import org.hbmc.model.Room;
import org.hbmc.model.enums.CancellationType;
import org.hbmc.model.enums.ReservationStatus;
import org.hbmc.repository.ReservationRepository;
import org.hbmc.repository.RowMapper;

import java.sql.*;
import java.time.LocalDate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReservationRepositoryJDBC implements ReservationRepository {

    private final Connection connection;
    private final UserRepositoryJDBC userRepository;
    private final RoomRepositoryJDBC roomRepository;
    private final RowMapper<Reservation> reservationMapper;

    public ReservationRepositoryJDBC() {
        this.connection = DatabaseConnection.getInstance().getConnection();
        this.userRepository = new UserRepositoryJDBC();
        this.roomRepository = new RoomRepositoryJDBC();

        this.reservationMapper = rs -> {
            int clientId = rs.getInt("client_id");
            int roomId = rs.getInt("room_id");

            Client client = (Client) userRepository.findById(clientId)
                    .orElseThrow(() -> new RuntimeException("Referenced client not found: " + clientId));
            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> new RuntimeException("Referenced room not found: " + roomId));

            Reservation reservation = new Reservation(
                    client,
                    room,
                    rs.getString("reservation_code"),
                    rs.getDate("check_in").toLocalDate(),
                    rs.getDate("check_out").toLocalDate(),
                    rs.getInt("number_of_guests")
            );
            reservation.setId(rs.getInt("id"));

            String status = rs.getString("status");
            if (ReservationStatus.COMPLETED.name().equals(status)) {
                reservation.markAsCompleted();
            }

            return reservation;
        };
    }

    @Override
    public Reservation save(Reservation reservation) {
        String sql = """
            INSERT INTO reservations
                (client_id, room_id, reservation_code, check_in, check_out, number_of_guests, status, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, reservation.getClient().getId());
            stmt.setInt(2, reservation.getRoom().getId());
            stmt.setString(3, reservation.getReservationCode());
            stmt.setDate(4, Date.valueOf(reservation.getCheckIn()));
            stmt.setDate(5, Date.valueOf(reservation.getCheckOut()));
            stmt.setInt(6, reservation.getNumberOfGuests());
            stmt.setString(7, reservation.getStatus().name());
            stmt.setTimestamp(8, Timestamp.valueOf(reservation.getCreatedAt()));

            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    reservation.setId(keys.getInt(1));
                }
            }
            return reservation;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save reservation: " + e.getMessage(), e);
        }
    }

    @Override
    public Reservation update(Reservation reservation) {
        String sql = """
            UPDATE reservations
            SET room_id = ?, check_in = ?, check_out = ?, number_of_guests = ?, status = ?
            WHERE id = ?
        """;
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, reservation.getRoom().getId());
            stmt.setDate(2, Date.valueOf(reservation.getCheckIn()));
            stmt.setDate(3, Date.valueOf(reservation.getCheckOut()));
            stmt.setInt(4, reservation.getNumberOfGuests());
            stmt.setString(5, reservation.getStatus().name());
            stmt.setInt(6, reservation.getId());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new RuntimeException("No reservation found with id: " + reservation.getId());
            }
            return reservation;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update reservation: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Reservation> findById(int id) {
        String sql = "SELECT * FROM reservations WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(reservationMapper.map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservation by id: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Reservation> findByReservationCode(String code) {
        String sql = "SELECT * FROM reservations WHERE reservation_code = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, code);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(reservationMapper.map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservation by code: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Reservation> findByClientId(int clientId) {
        return queryList("SELECT * FROM reservations WHERE client_id = ?", clientId);
    }

    @Override
    public List<Reservation> findByRoomId(int roomId) {
        return queryList("SELECT * FROM reservations WHERE room_id = ?", roomId);
    }

    private List<Reservation> queryList(String sql, int param) {
        List<Reservation> reservations = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, param);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(reservationMapper.map(rs));
                }
            }
            return reservations;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to query reservations: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Reservation> findByStatus(ReservationStatus status) {
        String sql = "SELECT * FROM reservations WHERE status = ?";
        List<Reservation> reservations = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    reservations.add(reservationMapper.map(rs));
                }
            }
            return reservations;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find reservations by status: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Reservation> findAll() {
        String sql = "SELECT * FROM reservations";
        List<Reservation> reservations = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                reservations.add(reservationMapper.map(rs));
            }
            return reservations;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all reservations: " + e.getMessage(), e);
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
            throw new RuntimeException("Failed to check reservation overlap: " + e.getMessage(), e);
        }
    }

    @Override
    public CanceledReservation saveCancellation(CanceledReservation canceledReservation) {
        String sql = """
            INSERT INTO canceled_reservations (reservation_id, canceled_at, refund_amount, type)
            VALUES (?, ?, ?, ?)
        """;
        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, canceledReservation.getReservation().getId());
            stmt.setTimestamp(2, Timestamp.valueOf(canceledReservation.getCanceledAt()));
            stmt.setBigDecimal(3, canceledReservation.getRefundAmount());
            stmt.setString(4, canceledReservation.getType().name());

            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    canceledReservation.setId(keys.getInt(1));
                }
            }
            return canceledReservation;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save cancellation: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<CanceledReservation> findCancellationByReservationId(int reservationId) {
        String sql = "SELECT * FROM canceled_reservations WHERE reservation_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, reservationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Reservation reservation = findById(reservationId)
                        .orElseThrow(() -> new RuntimeException("Reservation not found: " + reservationId));
                CanceledReservation canceled = new CanceledReservation(
                        reservation,
                        rs.getBigDecimal("refund_amount"),
                        CancellationType.valueOf(rs.getString("type"))
                );
                canceled.setId(rs.getInt("id"));
                return Optional.of(canceled);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find cancellation: " + e.getMessage(), e);
        }
    }
}