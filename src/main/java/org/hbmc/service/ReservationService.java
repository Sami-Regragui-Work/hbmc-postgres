package org.hbmc.service;

import org.hbmc.model.*;
import org.hbmc.model.enums.CancellationType;
import org.hbmc.model.enums.PaymentMethod;
import org.hbmc.model.enums.PaymentStatus;
import org.hbmc.policy.PricingStrategy;
import org.hbmc.policy.RefundPolicy;
import org.hbmc.repository.PaymentRepository;
import org.hbmc.repository.ReservationRepository;
import org.hbmc.repository.RoomRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;

public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final RoomRepository roomRepository;
    private final Connection connection;
    private final PricingStrategy pricingStrategy;
    private final RefundPolicy refundPolicy;

    public ReservationService(
            ReservationRepository reservationRepository,
            PaymentRepository paymentRepository,
            RoomRepository roomRepository,
            Connection connection,
            PricingStrategy pricingStrategy,
            RefundPolicy refundPolicy
    ) {
        this.reservationRepository = reservationRepository;
        this.paymentRepository = paymentRepository;
        this.roomRepository = roomRepository;
        this.connection = connection;
        this.pricingStrategy = pricingStrategy;
        this.refundPolicy = refundPolicy;
    }

    public Reservation bookReservation(Client client, Room room, LocalDate checkIn, LocalDate checkOut,
                                       int numberOfGuests, PaymentMethod paymentMethod) {
        if (roomRepository.hasOverlap(room.getId(), checkIn, checkOut)) {
            throw new IllegalStateException("Room " + room.getRoomNumber() + " is not available for the selected dates");
        }

        String reservationCode = generateReservationCode();
        Reservation reservation = new Reservation(client, room, reservationCode, checkIn, checkOut, numberOfGuests);
        BigDecimal price = pricingStrategy.calculatePrice(reservation);

        try {
            connection.setAutoCommit(false);

            Reservation savedReservation = reservationRepository.save(reservation);
            Payment payment = new Payment(savedReservation, price, paymentMethod, PaymentStatus.COMPLETED);
            paymentRepository.save(payment);

            connection.commit();
            return savedReservation;

        } catch (Exception e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                throw new RuntimeException("Rollback failed after booking error: " + rollbackEx.getMessage(), rollbackEx);
            }
            throw new RuntimeException("Failed to book reservation, transaction rolled back: " + e.getMessage(), e);
        } finally {
            resetAutoCommit();
        }
    }

    public CanceledReservation cancelReservation(Reservation reservation) {
        Payment payment = paymentRepository.findByReservationId(reservation.getId())
                .orElseThrow(() -> new IllegalStateException("No payment found for reservation: " + reservation.getId()));

        CancellationType type = refundPolicy.determineCancellationType(reservation.getCheckIn(), LocalDate.now());
        BigDecimal refundAmount = refundPolicy.calculateRefund(payment.getTotal(), type);

        try {
            connection.setAutoCommit(false);

            CanceledReservation canceledReservation = reservation.markAsCanceled(refundAmount, type);
            reservationRepository.update(reservation);
            CanceledReservation saved = reservationRepository.saveCancellation(canceledReservation);

            connection.commit();
            return saved;

        } catch (Exception e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                throw new RuntimeException("Rollback failed after cancellation error: " + rollbackEx.getMessage(), rollbackEx);
            }
            throw new RuntimeException("Failed to cancel reservation, transaction rolled back: " + e.getMessage(), e);
        } finally {
            resetAutoCommit();
        }
    }

    private void resetAutoCommit() {
        try {
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to reset autoCommit: " + e.getMessage(), e);
        }
    }

    private String generateReservationCode() {
        return "RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}