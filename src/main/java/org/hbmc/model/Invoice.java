package org.hbmc.model;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class Invoice {
    private UUID id;
    private final Payment payment;
    private final BigDecimal offTax;
    private final BigDecimal tax;

    public Invoice(@NotNull Payment payment, BigDecimal offTax, BigDecimal tax) {
        this.payment = payment;
        this.offTax = offTax;
        this.tax = tax;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Payment getPayment() {
        return payment;
    }

    public BigDecimal getOffTax() {
        return offTax;
    }

    public BigDecimal getTax() {
        return tax;
    }
}
