package org.hbmc.dto;

import org.hbmc.model.Reservation;
import org.hbmc.model.enums.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ReservationSummaryDTO {

    private final int reservationId;
    private final String reservationCode;
    private final String roomNumber;
    private final LocalDate checkIn;
    private final LocalDate checkOut;
    private final ReservationStatus status;
    private final BigDecimal totalPrice;

    public ReservationSummaryDTO(Reservation reservation, BigDecimal totalPrice) {
        this.reservationId = reservation.getId();
        this.reservationCode = reservation.getReservationCode();
        this.roomNumber = reservation.getRoom().getRoomNumber();
        this.checkIn = reservation.getCheckIn();
        this.checkOut = reservation.getCheckOut();
        this.status = reservation.getStatus();
        this.totalPrice = totalPrice;
    }

    public int getReservationId() {
        return reservationId;
    }

    public String getReservationCode() {
        return reservationCode;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public BigDecimal getTotalPrice() {
        return totalPrice;
    }
}