package org.hbmc.model;

import org.hbmc.model.enums.CancellationType;
import org.hbmc.model.enums.ReservationStatus;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

public class Reservation {
    private UUID id;
    private final Client client;
    private Room room;
    private String reservationCode;

    private LocalDate checkIn;
    private LocalDate checkOut;

    private int numberOfGuests;

    private ReservationStatus status;

    private final LocalDateTime createdAt;

    public Reservation(@NotNull Client client, @NotNull Room room, String reservationCode, LocalDate checkIn, LocalDate checkOut, int numberOfGuests) {
        this.client = Objects.requireNonNull(client, "Can't create reservation without a Client");
        this.room = Objects.requireNonNull(room, "Can't create reservation without a Room");
        this.reservationCode = reservationCode;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.numberOfGuests = numberOfGuests;

        this.status = ReservationStatus.CONFIRMED;

        this.createdAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Client getClient() {
        return client;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public String getReservationCode() {
        return reservationCode;
    }

    public void setReservationCode(String reservationCode) {
        this.reservationCode = reservationCode;
    }

    public LocalDate getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(LocalDate checkIn) {
        this.checkIn = checkIn;
    }

    public LocalDate getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(LocalDate checkOut) {
        this.checkOut = checkOut;
    }

    public int getNumberOfGuests() {
        return numberOfGuests;
    }

    public void setNumberOfGuests(int numberOfGuests) {
        this.numberOfGuests = numberOfGuests;
    }

    public int calculateNumberOfNights() {
        return (int) ChronoUnit.DAYS.between(this.checkIn, this.checkOut);
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void markAsCompleted() {
        this.status = ReservationStatus.COMPLETED;
    }

    public CanceledReservation markAsCanceled(BigDecimal refundAmount, BigDecimal penalityAmount, CancellationType type) {
        this.status = ReservationStatus.CANCELLED;
        return new CanceledReservation(this, refundAmount, penalityAmount, type);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
