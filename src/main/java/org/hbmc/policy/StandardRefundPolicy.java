package org.hbmc.policy;

import org.hbmc.model.enums.CancellationType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class StandardRefundPolicy implements RefundPolicy {

    @Override
    public CancellationType determineCancellationType(LocalDate checkIn, LocalDate cancellationDate) {
        long hoursUntilCheckIn = ChronoUnit.HOURS.between(
                cancellationDate.atStartOfDay(),
                checkIn.atStartOfDay()
        );
        long daysUntilCheckIn = ChronoUnit.DAYS.between(cancellationDate, checkIn);

        if (daysUntilCheckIn > 14) {
            return CancellationType.FULL_REFUND;
        } else if (daysUntilCheckIn >= 7) {
            return CancellationType.PARTIAL_7_14_REFUND_70;
        } else if (hoursUntilCheckIn >= 48) {
            return CancellationType.PARTIAL_2_7_REFUND_50;
        } else {
            return CancellationType.NO_REFUND;
        }
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal total, CancellationType type) {
        return type.calculateRefund(total);
    }
}