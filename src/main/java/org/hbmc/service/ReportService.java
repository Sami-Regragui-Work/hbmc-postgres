package org.hbmc.service;

import org.hbmc.model.Payment;
import org.hbmc.model.Reservation;
import org.hbmc.model.Room;
import org.hbmc.model.enums.PaymentStatus;
import org.hbmc.model.enums.ReservationStatus;
import org.hbmc.repository.PaymentRepository;
import org.hbmc.repository.ReservationRepository;
import org.hbmc.repository.RoomRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReportService {

    private final ReservationRepository reservationRepository;
    private final PaymentRepository paymentRepository;
    private final RoomRepository roomRepository;

    public ReportService(ReservationRepository reservationRepository, PaymentRepository paymentRepository, RoomRepository roomRepository) {
        this.reservationRepository = reservationRepository;
        this.paymentRepository = paymentRepository;
        this.roomRepository = roomRepository;
    }

    public BigDecimal totalRevenue() {
        return this.paymentRepository.findByStatus(PaymentStatus.COMPLETED).stream()
                .map(Payment::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal averageBookingValue() {
        List<Payment> completedPayments = this.paymentRepository.findByStatus(PaymentStatus.COMPLETED);
        if (completedPayments.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = completedPayments.stream()
                .map(Payment::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(completedPayments.size()), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal occupancyRate() {
        List<Room> allRooms = this.roomRepository.findAll();
        if (allRooms.isEmpty()) {
            return BigDecimal.ZERO;
        }
        long occupiedCount = this.reservationRepository.findByStatus(ReservationStatus.CONFIRMED).stream()
                .map(Reservation::getRoom)
                .distinct()
                .count();
        return BigDecimal.valueOf(occupiedCount)
                .divide(BigDecimal.valueOf(allRooms.size()), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public List<Map.Entry<Room, Long>> mostBookedRooms(int topN) {
        List<Reservation> allReservations = this.reservationRepository.findAll();

        Map<Room, Long> bookingCounts = allReservations.stream()
                .collect(Collectors.groupingBy(Reservation::getRoom, Collectors.counting()));

        return bookingCounts.entrySet().stream()
                .sorted(Map.Entry.<Room, Long>comparingByValue().reversed())
                .limit(topN)
                .collect(Collectors.toList());
    }

    public long totalReservationsCount() {
        return this.reservationRepository.findAll().size();
    }

    public long cancelledReservationsCount() {
        return this.reservationRepository.findByStatus(ReservationStatus.CANCELLED).size();
    }
}
