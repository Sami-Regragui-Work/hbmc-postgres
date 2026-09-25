package org.hbmc.policy;

import org.hbmc.model.enums.CancellationType;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface RefundPolicy {
    CancellationType determineCancellationType(LocalDate checkIn, LocalDate cancellationDate);
    BigDecimal calculateRefund(BigDecimal total, CancellationType type);
}