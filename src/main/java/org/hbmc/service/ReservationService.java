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
        if (this.roomRepository.hasOverlap(room.getId(), checkIn, checkOut)) {
            throw new RoomNotAvailableException("Room " + room.getRoomNumber() + " is not available for the selected dates");
        }

        String reservationCode = this.generateReservationCode();
        Reservation reservation = new Reservation(client, room, reservationCode, checkIn, checkOut, numberOfGuests);
        BigDecimal offTax = this.pricingStrategy.calculatePrice(room, checkIn, checkOut);

        try {
            this.connection.setAutoCommit(false);

            Reservation savedReservation = this.reservationRepository.save(reservation);

            Payment payment = new Payment(savedReservation, offTax, paymentMethod, PaymentStatus.COMPLETED);
            Payment savedPayment = this.paymentRepository.save(payment, this.connection);

            BigDecimal tax = this.calculateTax(offTax);
            Invoice invoice = new Invoice(savedPayment, offTax, tax);
            this.invoiceRepository.save(invoice, this.connection);

            this.connection.commit();
            return savedReservation;

        } catch (Exception e) {
            try {
                this.connection.rollback();
            } catch (SQLException rollbackEx) {
                throw new RuntimeException("Rollback failed after booking error: " + rollbackEx.getMessage(), rollbackEx);
            }
            throw new RuntimeException("Failed to book reservation, transaction rolled back: " + e.getMessage(), e);
        } finally {
            this.resetAutoCommit();
        }
    }

    public Reservation updateReservation(Client client, String reservationCode, LocalDate newCheckIn, LocalDate newCheckOut, int newNumberOfGuests) {
        Reservation reservation = this.getOwnedActiveReservation(client, reservationCode);

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
        if (this.reservationRepository.hasOverlap(room.getId(), newCheckIn, newCheckOut, reservation.getId())) {
            throw new RoomNotAvailableException("Room " + room.getRoomNumber() + " is not available for the selected dates");
        }

        BigDecimal newOffTax = this.pricingStrategy.calculatePrice(room, newCheckIn, newCheckOut);
        BigDecimal newTax = this.calculateTax(newOffTax);

        Payment payment = this.paymentRepository.findByReservationId(reservation.getId())
                .orElseThrow(() -> new InvalidReservationException("No payment found for reservation: " + reservation.getId()));
        Invoice invoice = this.invoiceRepository.findByPaymentId(payment.getId())
                .orElseThrow(() -> new InvalidReservationException("No invoice found for payment: " + payment.getId()));

        try {
            this.connection.setAutoCommit(false);

            reservation.setCheckIn(newCheckIn);
            reservation.setCheckOut(newCheckOut);
            reservation.setNumberOfGuests(newNumberOfGuests);
            this.reservationRepository.update(reservation);

            payment.setTotal(newOffTax);
            this.paymentRepository.update(payment, this.connection);

            invoice.setOffTax(newOffTax);
            invoice.setTax(newTax);
            this.invoiceRepository.update(invoice, this.connection);

            this.connection.commit();
            return reservation;

        } catch (Exception e) {
            try {
                this.connection.rollback();
            } catch (SQLException rollbackEx) {
                throw new RuntimeException("Rollback failed after update error: " + rollbackEx.getMessage(), rollbackEx);
            }
            throw new RuntimeException("Failed to update reservation, transaction rolled back: " + e.getMessage(), e);
        } finally {
            this.resetAutoCommit();
        }
    }

    public List<Reservation> getReservationsForClient(Client client) {
        return this.reservationRepository.findByClientId(client.getId()).stream()
                .sorted(Comparator.comparing(Reservation::getCreatedAt).reversed())
                .toList();
    }

    public List<Reservation> getAllReservations() {
        return this.reservationRepository.findAll();
    }

    public CanceledReservation cancelReservation(Client client, String reservationCode) {
        Reservation reservation = this.getOwnedActiveReservation(client, reservationCode);

        Payment payment = this.paymentRepository.findByReservationId(reservation.getId())
                .orElseThrow(() -> new InvalidReservationException("No payment found for reservation: " + reservation.getId()));

        CancellationType type = this.refundPolicy.determineCancellationType(reservation.getCheckIn(), LocalDate.now());
        BigDecimal refundAmount = this.refundPolicy.calculateRefund(payment.getTotal(), type);

        try {
            this.connection.setAutoCommit(false);

            CanceledReservation canceledReservation = reservation.markAsCanceled(refundAmount, type);
            this.reservationRepository.update(reservation);
            CanceledReservation saved = this.reservationRepository.saveCancellation(canceledReservation);

            this.connection.commit();
            return saved;

        } catch (Exception e) {
            try {
                this.connection.rollback();
            } catch (SQLException rollbackEx) {
                throw new RuntimeException("Rollback failed after cancellation error: " + rollbackEx.getMessage(), rollbackEx);
            }
            throw new RuntimeException("Failed to cancel reservation, transaction rolled back: " + e.getMessage(), e);
        } finally {
            this.resetAutoCommit();
        }
    }

    private Reservation getOwnedActiveReservation(Client client, String reservationCode) {
        Reservation reservation = this.reservationRepository.findByReservationCode(reservationCode)
                .orElseThrow(() -> new ReservationNotFoundException("No reservation found with code: " + reservationCode));

        if (reservation.getClient().getId() != client.getId()) {
            throw new UnauthorizedReservationAccessException("You are not allowed to access this reservation");
        }
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ReservationAlreadyCancelledException("Reservation " + reservationCode + " is already " + reservation.getStatus());
        }
        return reservation;
    }

    // offTax is the strategy output (HT). tax holds only the TVA delta, TTC is derived as offTax + tax.
    private BigDecimal calculateTax(BigDecimal offTax) {
        return offTax.multiply(ReservationService.TVA_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    private void resetAutoCommit() {
        try {
            this.connection.setAutoCommit(true);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to reset autoCommit: " + e.getMessage(), e);
        }
    }

    private String generateReservationCode() {
        LocalDateTime timestamp = LocalDateTime.now();
        String code = timestamp.format(ReservationService.RESERVATION_CODE_FORMAT);

        if (code.equals(this.lastReservationCode)) {
            timestamp = this.lastReservationTimestamp.plusNanos(10_000_000);
            code = timestamp.format(ReservationService.RESERVATION_CODE_FORMAT);
        }

        this.lastReservationCode = code;
        this.lastReservationTimestamp = timestamp;
        return code;
    }
}
