package org.hbmc.service;

import org.hbmc.exception.*;
import org.hbmc.model.*;
import org.hbmc.model.enums.*;
import org.hbmc.policy.PricingStrategy;
import org.hbmc.policy.RefundPolicy;
import org.hbmc.repository.InvoiceRepository;
import org.hbmc.repository.PaymentRepository;
import org.hbmc.repository.ReservationRepository;
import org.hbmc.repository.RoomRepository;
import org.hbmc.util.ValidationUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public class ReservationService {

    private static final BigDecimal TVA_RATE = new BigDecimal("0.20");
    private static final DateTimeFormatter RESERVATION_CODE_FORMAT = DateTimeFormatter.ofPattern("'RES-'yyyyMMdd-HHmmssSS");

    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final RoomRepository roomRepository;
    private final Connection connection;
    private final PricingStrategy pricingStrategy;
    private final RefundPolicy refundPolicy;

    private String lastReservationCode = null;
    private LocalDateTime lastReservationTimestamp = null;

    public ReservationService(
            ReservationRepository reservationRepository,
            PaymentRepository paymentRepository,
            InvoiceRepository invoiceRepository,
            RoomRepository roomRepository,
            Connection connection,
            PricingStrategy pricingStrategy,
            RefundPolicy refundPolicy
    ) {
        this.reservationRepository = reservationRepository;
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.roomRepository = roomRepository;
        this.connection = connection;
        this.pricingStrategy = pricingStrategy;
        this.refundPolicy = refundPolicy;
    }

    public Reservation bookReservation(Client client, Room room, LocalDate checkIn, LocalDate checkOut,
                                       int numberOfGuests, PaymentMethod paymentMethod) {
        if (!checkIn.isBefore(checkOut)) {
            throw new InvalidReservationException("Check-in date must be before check-out date");
        }
        ValidationUtils.validatePositiveGuestCount(numberOfGuests);

        if (room.getStatus() == RoomStatus.MAINTENANCE) {
            throw new RoomNotAvailableException("Room " + room.getRoomNumber() + " is under maintenance");
        }
        if (numberOfGuests > room.getCapacity()) {
            throw new RoomCapacityExceededException(
                    "Room " + room.getRoomNumber() + " capacity is " + room.getCapacity() + ", requested " + numberOfGuests
            );
        }
        if (roomRepository.hasOverlap(room.getId(), checkIn, checkOut)) {
            throw new RoomNotAvailableException("Room " + room.getRoomNumber() + " is not available for the selected dates");
        }

        String reservationCode = generateReservationCode();
        Reservation reservation = new Reservation(client, room, reservationCode, checkIn, checkOut, numberOfGuests);
        BigDecimal totalWithTax = pricingStrategy.calculatePrice(room, checkIn, checkOut);

        try {
            connection.setAutoCommit(false);

            Reservation savedReservation = reservationRepository.save(reservation);

            Payment payment = new Payment(savedReservation, totalWithTax, paymentMethod, PaymentStatus.COMPLETED);
            Payment savedPayment = paymentRepository.save(payment, connection);

            BigDecimal offTax = totalWithTax.divide(BigDecimal.ONE.add(TVA_RATE), 2, RoundingMode.HALF_UP);
            BigDecimal tax = totalWithTax.subtract(offTax);
            Invoice invoice = new Invoice(savedPayment, offTax, tax);
            invoiceRepository.save(invoice, connection);

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

    public Reservation updateReservation(Client client, String reservationCode, LocalDate newCheckIn, LocalDate newCheckOut, int newNumberOfGuests) {
        Reservation reservation = getOwnedActiveReservation(client, reservationCode);

        if (!newCheckIn.isBefore(newCheckOut)) {
            throw new InvalidReservationException("Check-in date must be before check-out date");
        }
        ValidationUtils.validatePositiveGuestCount(newNumberOfGuests);

        Room room = reservation.getRoom();
        if (room.getStatus() == RoomStatus.MAINTENANCE) {
            throw new RoomNotAvailableException("Room " + room.getRoomNumber() + " is under maintenance");
        }
        if (newNumberOfGuests > room.getCapacity()) {
            throw new RoomCapacityExceededException(
                    "Room " + room.getRoomNumber() + " capacity is " + room.getCapacity() + ", requested " + newNumberOfGuests
            );
        }
        if (reservationRepository.hasOverlap(room.getId(), newCheckIn, newCheckOut, reservation.getId())) {
            throw new RoomNotAvailableException("Room " + room.getRoomNumber() + " is not available for the selected dates");
        }

        BigDecimal newTotalWithTax = pricingStrategy.calculatePrice(room, newCheckIn, newCheckOut);

        try {
            connection.setAutoCommit(false);

            reservation.setCheckIn(newCheckIn);
            reservation.setCheckOut(newCheckOut);
            reservation.setNumberOfGuests(newNumberOfGuests);
            reservationRepository.update(reservation);

            Payment payment = paymentRepository.findByReservationId(reservation.getId())
                    .orElseThrow(() -> new InvalidReservationException("No payment found for reservation: " + reservation.getId()));
            payment.setTotal(newTotalWithTax);
            Payment updatedPayment = paymentRepository.update(payment, connection);

            BigDecimal offTax = newTotalWithTax.divide(BigDecimal.ONE.add(TVA_RATE), 2, RoundingMode.HALF_UP);
            BigDecimal tax = newTotalWithTax.subtract(offTax);
            Invoice invoice = invoiceRepository.findByPaymentId(updatedPayment.getId())
                    .orElseThrow(() -> new InvalidReservationException("No invoice found for payment: " + updatedPayment.getId()));
            invoice.setOffTax(offTax);
            invoice.setTax(tax);
            invoiceRepository.update(invoice, connection);

            connection.commit();
            return reservation;

        } catch (Exception e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                throw new RuntimeException("Rollback failed after update error: " + rollbackEx.getMessage(), rollbackEx);
            }
            throw new RuntimeException("Failed to update reservation, transaction rolled back: " + e.getMessage(), e);
        } finally {
            resetAutoCommit();
        }
    }

    public List<Reservation> getReservationsForClient(Client client) {
        return reservationRepository.findByClientId(client.getId()).stream()
                .sorted(Comparator.comparing(Reservation::getCreatedAt).reversed())
                .toList();
    }

    public CanceledReservation cancelReservation(Client client, String reservationCode) {
        Reservation reservation = getOwnedActiveReservation(client, reservationCode);

        Payment payment = paymentRepository.findByReservationId(reservation.getId())
                .orElseThrow(() -> new InvalidReservationException("No payment found for reservation: " + reservation.getId()));

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

    private Reservation getOwnedActiveReservation(Client client, String reservationCode) {
        Reservation reservation = reservationRepository.findByReservationCode(reservationCode)
                .orElseThrow(() -> new ReservationNotFoundException("No reservation found with code: " + reservationCode));

        if (reservation.getClient().getId() != client.getId()) {
            throw new UnauthorizedReservationAccessException("You are not allowed to access this reservation");
        }
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ReservationAlreadyCancelledException("Reservation " + reservationCode + " is already " + reservation.getStatus());
        }
        return reservation;
    }

    private void resetAutoCommit() {
        try {
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to reset autoCommit: " + e.getMessage(), e);
        }
    }

    private String generateReservationCode() {
        LocalDateTime timestamp = LocalDateTime.now();
        String code = timestamp.format(RESERVATION_CODE_FORMAT);

        if (code.equals(lastReservationCode)) {
            timestamp = lastReservationTimestamp.plusNanos(10_000_000);
            code = timestamp.format(RESERVATION_CODE_FORMAT);
        }

        lastReservationCode = code;
        lastReservationTimestamp = timestamp;
        return code;
    }
}