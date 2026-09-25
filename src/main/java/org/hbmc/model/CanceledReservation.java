package org.hbmc.model;

import org.hbmc.model.enums.CancellationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CanceledReservation {
    private int id;
    private final Reservation reservation;
    private final LocalDateTime canceledAt;
    private final BigDecimal refundAmount;
    private final CancellationType type;

    public CanceledReservation(Reservation reservation, BigDecimal refundAmount, CancellationType type) {
        this.reservation = reservation;
        this.canceledAt = LocalDateTime.now();
        this.refundAmount = refundAmount;
        this.type = type;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public LocalDateTime getCanceledAt() {
        return canceledAt;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public CancellationType getType() {
        return type;
    }
}
