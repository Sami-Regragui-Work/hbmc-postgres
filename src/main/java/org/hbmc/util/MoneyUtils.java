package org.hbmc.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoneyUtils {

    private MoneyUtils() {}

    public static BigDecimal round(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal calculateTotal(BigDecimal pricePerNight, long nights) {
        return MoneyUtils.round(pricePerNight.multiply(BigDecimal.valueOf(nights)));
    }
}
