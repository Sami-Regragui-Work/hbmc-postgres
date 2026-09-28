package org.hbmc.repository.jdbc;

import org.hbmc.db.DatabaseConnection;
import org.hbmc.model.Payment;
import org.hbmc.model.Reservation;
import org.hbmc.model.enums.PaymentMethod;
import org.hbmc.model.enums.PaymentStatus;
import org.hbmc.repository.PaymentRepository;
import org.hbmc.repository.RowMapper;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PaymentRepositoryJDBC implements PaymentRepository {

    private final Connection connection;
    private final ReservationRepositoryJDBC reservationRepository;
    private final RowMapper<Payment> paymentMapper;

    public PaymentRepositoryJDBC() {
        this.connection = DatabaseConnection.getInstance().getConnection();
        this.reservationRepository = new ReservationRepositoryJDBC();

        this.paymentMapper = rs -> {
            int reservationId = rs.getInt("reservation_id");
            Reservation reservation = this.reservationRepository.findById(reservationId)
                    .orElseThrow(() -> new RuntimeException("Referenced reservation not found: " + reservationId));

            Payment payment = new Payment(
                    reservation,
                    rs.getBigDecimal("total"),
                    PaymentMethod.valueOf(rs.getString("method")),
                    PaymentStatus.valueOf(rs.getString("status"))
            );
            payment.setId(rs.getInt("id"));
            return payment;
        };
    }

    @Override
    public Payment save(Payment payment) {
        return this.save(payment, this.connection);
    }

    @Override
    public Payment save(Payment payment, Connection transactionConnection) {
        String sql = "INSERT INTO payments (reservation_id, total, payment_date, method, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = transactionConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, payment.getReservation().getId());
            stmt.setBigDecimal(2, payment.getTotal());
            stmt.setDate(3, Date.valueOf(payment.getPaymentDate()));
            stmt.setString(4, payment.getMethod().name());
            stmt.setString(5, payment.getStatus().name());

            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    payment.setId(keys.getInt(1));
                }
            }
            return payment;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save payment: " + e.getMessage(), e);
        }
    }

    @Override
    public Payment update(Payment payment) {
        return this.update(payment, this.connection);
    }

    @Override
    public Payment update(Payment payment, Connection transactionConnection) {
        String sql = "UPDATE payments SET total = ?, method = ?, status = ? WHERE id = ?";
        try (PreparedStatement stmt = transactionConnection.prepareStatement(sql)) {
            stmt.setBigDecimal(1, payment.getTotal());
            stmt.setString(2, payment.getMethod().name());
            stmt.setString(3, payment.getStatus().name());
            stmt.setInt(4, payment.getId());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new RuntimeException("No payment found with id: " + payment.getId());
            }
            return payment;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update payment: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Payment> findById(int id) {
        String sql = "SELECT * FROM payments WHERE id = ?";
        try (PreparedStatement stmt = this.connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(this.paymentMapper.map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find payment by id: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Payment> findByReservationId(int reservationId) {
        String sql = "SELECT * FROM payments WHERE reservation_id = ?";
        try (PreparedStatement stmt = this.connection.prepareStatement(sql)) {
            stmt.setInt(1, reservationId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(this.paymentMapper.map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find payment by reservation id: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Payment> findByStatus(PaymentStatus status) {
        String sql = "SELECT * FROM payments WHERE status = ?";
        List<Payment> payments = new ArrayList<>();
        try (PreparedStatement stmt = this.connection.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    payments.add(this.paymentMapper.map(rs));
                }
            }
            return payments;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find payments by status: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Payment> findAll() {
        String sql = "SELECT * FROM payments";
        List<Payment> payments = new ArrayList<>();
        try (PreparedStatement stmt = this.connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                payments.add(this.paymentMapper.map(rs));
            }
            return payments;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all payments: " + e.getMessage(), e);
        }
    }
}
