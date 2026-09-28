package org.hbmc.model;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

public class Invoice {
    private int id;
    private final Payment payment;
    private BigDecimal offTax;
    private BigDecimal tax;

    public Invoice(@NotNull Payment payment, BigDecimal offTax, BigDecimal tax) {
        this.payment = payment;
        this.offTax = offTax;
        this.tax = tax;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Payment getPayment() {
        return this.payment;
    }

    public BigDecimal getOffTax() {
        return this.offTax;
    }

    public void setOffTax(BigDecimal offTax) {
        this.offTax = offTax;
    }

    public BigDecimal getTax() {
        return this.tax;
    }

    public void setTax(BigDecimal tax) {
        this.tax = tax;
    }
}
