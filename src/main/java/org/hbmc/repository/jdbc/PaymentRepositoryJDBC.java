package org.hbmc.repository.jdbc;

import org.hbmc.db.DatabaseConnection;
import org.hbmc.model.Payment;
import org.hbmc.model.Reservation;
import org.hbmc.model.enums.PaymentMethod;
import org.hbmc.model.enums.PaymentStatus;
import org.hbmc.repository.PaymentRepository;
import org.hbmc.repository.RowMapper;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public class PaymentRepositoryJDBC implements PaymentRepository {

    private final JdbcHelper jdbcHelper;
    private final ReservationRepositoryJDBC reservationRepository;
    private final RowMapper<Payment> paymentMapper;

    public PaymentRepositoryJDBC() {
        this.jdbcHelper = new JdbcHelper(DatabaseConnection.getInstance().getConnection());
        this.reservationRepository = new ReservationRepositoryJDBC();

        this.paymentMapper = rs -> {
            int reservationId = rs.getInt("reservation_id");
            Reservation reservation = this.reservationRepository.findById(reservationId)
                    .orElseThrow(() -> new RuntimeException("Referenced reservation not found: " + reservationId));

            Payment payment = new Payment(
                    reservation,
                    rs.getBigDecimal("total"),
                    rs.getDate("payment_date").toLocalDate(),
                    PaymentMethod.valueOf(rs.getString("method")),
                    PaymentStatus.valueOf(rs.getString("status"))
            );
            payment.setId(rs.getInt("id"));
            return payment;
        };
    }

    @Override
    public Payment save(Payment payment) {
        return this.save(payment, this.jdbcHelper);
    }

    @Override
    public Payment save(Payment payment, Connection transactionConnection) {
        return this.save(payment, new JdbcHelper(transactionConnection));
    }

    private Payment save(Payment payment, JdbcHelper helper) {
        String sql = "INSERT INTO payments (reservation_id, total, payment_date, method, status) VALUES (?, ?, ?, ?, ?)";
        int id = helper.insertReturningId(
                sql,
                payment.getReservation().getId(),
                payment.getTotal(),
                payment.getPaymentDate(),
                payment.getMethod(),
                payment.getStatus()
        );
        payment.setId(id);
        return payment;
    }

    @Override
    public Payment update(Payment payment) {
        return this.update(payment, this.jdbcHelper);
    }

    @Override
    public Payment update(Payment payment, Connection transactionConnection) {
        return this.update(payment, new JdbcHelper(transactionConnection));
    }

    private Payment update(Payment payment, JdbcHelper helper) {
        String sql = "UPDATE payments SET total = ?, method = ?, status = ? WHERE id = ?";
        int rowsAffected = helper.update(
                sql,
                payment.getTotal(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getId()
        );
        if (rowsAffected == 0) {
            throw new RuntimeException("No payment found with id: " + payment.getId());
        }
        return payment;
    }

    @Override
    public Optional<Payment> findById(int id) {
        String sql = "SELECT * FROM payments WHERE id = ?";
        return this.jdbcHelper.queryOne(sql, this.paymentMapper, id);
    }

    @Override
    public Optional<Payment> findByReservationId(int reservationId) {
        String sql = "SELECT * FROM payments WHERE reservation_id = ?";
        return this.jdbcHelper.queryOne(sql, this.paymentMapper, reservationId);
    }

    @Override
    public List<Payment> findByStatus(PaymentStatus status) {
        String sql = "SELECT * FROM payments WHERE status = ?";
        return this.jdbcHelper.queryList(sql, this.paymentMapper, status);
    }

    @Override
    public List<Payment> findAll() {
        String sql = "SELECT * FROM payments";
        return this.jdbcHelper.queryList(sql, this.paymentMapper);
    }
}
