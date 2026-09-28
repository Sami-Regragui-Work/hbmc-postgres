package org.hbmc.model;

import org.hbmc.model.enums.PaymentMethod;
import org.hbmc.model.enums.PaymentStatus;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class Payment {
    private int id;
    private final Reservation reservation;
    private BigDecimal total;
    private final LocalDate paymentDate;
    private PaymentMethod method;
    private PaymentStatus status;

    public Payment(@NotNull Reservation reservation, BigDecimal total, PaymentMethod method) {
        this(reservation, total, method, PaymentStatus.COMPLETED);
    }

    public Payment(@NotNull Reservation reservation, BigDecimal total, PaymentMethod method, PaymentStatus status) {
        this(reservation, total, LocalDate.now(), method, status);
    }

    public Payment(@NotNull Reservation reservation, BigDecimal total, LocalDate paymentDate, PaymentMethod method, PaymentStatus status) {
        this.reservation = Objects.requireNonNull(reservation, "Can't make a Payment without Reservation");
        this.total = total;
        this.paymentDate = paymentDate;
        this.method = method;
        this.status = status;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Reservation getReservation() {
        return this.reservation;
    }

    public BigDecimal getTotal() {
        return this.total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public LocalDate getPaymentDate() {
        return this.paymentDate;
    }

    public PaymentMethod getMethod() {
        return this.method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public PaymentStatus getStatus() {
        return this.status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }
}
