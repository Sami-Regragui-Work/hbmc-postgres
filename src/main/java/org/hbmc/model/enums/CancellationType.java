package org.hbmc.model.enums;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum CancellationType {
    FULL_REFUND(BigDecimal.ONE),
    PARTIAL_7_14_REFUND_70(new BigDecimal("0.7")),
    PARTIAL_2_7_REFUND_50(new BigDecimal("0.5")),
    NO_REFUND(BigDecimal.ZERO);

    private final BigDecimal refundMultiplier;

    CancellationType(BigDecimal refundMultiplier) {
        this.refundMultiplier = refundMultiplier;
    }

    public BigDecimal getRefundMultiplier() {
        return this.refundMultiplier;
    }

    public BigDecimal calculateRefund(BigDecimal price) {
        return price.multiply(this.refundMultiplier).setScale(2, RoundingMode.HALF_UP);
    }
}
