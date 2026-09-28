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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ReservationRepositoryJDBC implements ReservationRepository {

    private final JdbcHelper jdbcHelper;
    private final UserRepositoryJDBC userRepository;
    private final RoomRepositoryJDBC roomRepository;
    private final RowMapper<Reservation> reservationMapper;

    public ReservationRepositoryJDBC() {
        this.jdbcHelper = new JdbcHelper(DatabaseConnection.getInstance().getConnection());
        this.userRepository = new UserRepositoryJDBC();
        this.roomRepository = new RoomRepositoryJDBC();

        this.reservationMapper = rs -> {
            int clientId = rs.getInt("client_id");
            int roomId = rs.getInt("room_id");

            Client client = (Client) this.userRepository.findById(clientId)
                    .orElseThrow(() -> new RuntimeException("Referenced client not found: " + clientId));
            Room room = this.roomRepository.findById(roomId)
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
            reservation.setStatus(ReservationStatus.valueOf(rs.getString("status")));

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
        int id = this.jdbcHelper.insertReturningId(
                sql,
                reservation.getClient().getId(),
                reservation.getRoom().getId(),
                reservation.getReservationCode(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getNumberOfGuests(),
                reservation.getStatus(),
                reservation.getCreatedAt()
        );
        reservation.setId(id);
        return reservation;
    }

    @Override
    public Reservation update(Reservation reservation) {
        String sql = """
            UPDATE reservations
            SET room_id = ?, check_in = ?, check_out = ?, number_of_guests = ?, status = ?
            WHERE id = ?
        """;
        int rowsAffected = this.jdbcHelper.update(
                sql,
                reservation.getRoom().getId(),
                reservation.getCheckIn(),
                reservation.getCheckOut(),
                reservation.getNumberOfGuests(),
                reservation.getStatus(),
                reservation.getId()
        );
        if (rowsAffected == 0) {
            throw new RuntimeException("No reservation found with id: " + reservation.getId());
        }
        return reservation;
    }

    @Override
    public Optional<Reservation> findById(int id) {
        String sql = "SELECT * FROM reservations WHERE id = ?";
        return this.jdbcHelper.queryOne(sql, this.reservationMapper, id);
    }

    @Override
    public Optional<Reservation> findByReservationCode(String code) {
        String sql = "SELECT * FROM reservations WHERE reservation_code = ?";
        return this.jdbcHelper.queryOne(sql, this.reservationMapper, code);
    }

    @Override
    public List<Reservation> findByClientId(int clientId) {
        String sql = "SELECT * FROM reservations WHERE client_id = ?";
        return this.jdbcHelper.queryList(sql, this.reservationMapper, clientId);
    }

    @Override
    public List<Reservation> findByRoomId(int roomId) {
        String sql = "SELECT * FROM reservations WHERE room_id = ?";
        return this.jdbcHelper.queryList(sql, this.reservationMapper, roomId);
    }

    @Override
    public List<Reservation> findByStatus(ReservationStatus status) {
        String sql = "SELECT * FROM reservations WHERE status = ?";
        return this.jdbcHelper.queryList(sql, this.reservationMapper, status);
    }

    @Override
    public List<Reservation> findAll() {
        String sql = "SELECT * FROM reservations";
        return this.jdbcHelper.queryList(sql, this.reservationMapper);
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
    public boolean hasOverlap(int roomId, LocalDate checkIn, LocalDate checkOut, int excludeReservationId) {
        String sql = """
        SELECT 1 FROM reservations
        WHERE room_id = ?
        AND id != ?
        AND status != 'CANCELLED'
        AND check_in < ?
        AND check_out > ?
    """;
        return this.jdbcHelper.exists(sql, roomId, excludeReservationId, checkOut, checkIn);
    }

    @Override
    public CanceledReservation saveCancellation(CanceledReservation canceledReservation) {
        String sql = """
            INSERT INTO canceled_reservations (reservation_id, canceled_at, refund_amount, type)
            VALUES (?, ?, ?, ?)
        """;
        int id = this.jdbcHelper.insertReturningId(
                sql,
                canceledReservation.getReservation().getId(),
                canceledReservation.getCanceledAt(),
                canceledReservation.getRefundAmount(),
                canceledReservation.getType()
        );
        canceledReservation.setId(id);
        return canceledReservation;
    }

    @Override
    public Optional<CanceledReservation> findCancellationByReservationId(int reservationId) {
        String sql = "SELECT * FROM canceled_reservations WHERE reservation_id = ?";
        return this.jdbcHelper.queryOne(sql, rs -> {
            Reservation reservation = this.findById(reservationId)
                    .orElseThrow(() -> new RuntimeException("Reservation not found: " + reservationId));
            CanceledReservation canceled = new CanceledReservation(
                    reservation,
                    rs.getBigDecimal("refund_amount"),
                    CancellationType.valueOf(rs.getString("type"))
            );
            canceled.setId(rs.getInt("id"));
            return canceled;
        }, reservationId);
    }
}
