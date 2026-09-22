package org.hbmc.model;

import org.hbmc.model.enums.CancellationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class CanceledReservation {
    private UUID id;
    private final Reservation reservation;
    private final LocalDateTime canceledAt;
    private final BigDecimal refundAmount;
    private final BigDecimal penalityAmount;
    private final CancellationType type;

    public CanceledReservation(Reservation reservation, BigDecimal refundAmount, BigDecimal penalityAmount, CancellationType type) {
        this.reservation = reservation;
        this.canceledAt = LocalDateTime.now();
        this.refundAmount = refundAmount;
        this.penalityAmount = penalityAmount;
        this.type = type;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
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

    public BigDecimal getPenalityAmount() {
        return penalityAmount;
    }

    public CancellationType getType() {
        return type;
    }
}
