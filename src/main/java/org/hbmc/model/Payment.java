package org.hbmc.model;

import org.hbmc.model.enums.PaymentMethod;
import org.hbmc.model.enums.PaymentStatus;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class Payment {
    private UUID id;
    private final Reservation reservation;
    private final BigDecimal total;
    private final LocalDate paymentDate;
    private PaymentMethod method;
    private PaymentStatus status;

    public Payment(@NotNull Reservation reservation, BigDecimal total, PaymentMethod method) {
        this(reservation, total, method, PaymentStatus.COMPLETED);
    }

    public Payment(@NotNull Reservation reservation, BigDecimal total, PaymentMethod method, PaymentStatus status) {
        this.reservation = Objects.requireNonNull(reservation, "Can't make a Payment without Reservation");
        this.total = total;
        this.paymentDate = LocalDate.now();
        this.method = method;
        this.status = status;
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

    public BigDecimal getTotal() {
        return total;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }
}
