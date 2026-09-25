package org.hbmc.policy;

import org.hbmc.model.Reservation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;

public class StandardPricingStrategy implements PricingStrategy {

    private static final BigDecimal HIGH_SEASON_MULTIPLIER = new BigDecimal("1.30");
    private static final BigDecimal LOW_SEASON_MULTIPLIER = new BigDecimal("0.85");
    private static final BigDecimal WEEKEND_MULTIPLIER = new BigDecimal("1.15");
    private static final BigDecimal LONG_STAY_TIER1_MULTIPLIER = new BigDecimal("0.90"); // >= 7 nights
    private static final BigDecimal LONG_STAY_TIER2_MULTIPLIER = new BigDecimal("0.85"); // >= 14 nights
    private static final BigDecimal EARLY_BOOKING_MULTIPLIER = new BigDecimal("0.95");   // >= 30 days ahead
    private static final BigDecimal LAST_MINUTE_MULTIPLIER = new BigDecimal("1.10");     // <= 3 days ahead

    @Override
    public BigDecimal calculatePrice(Reservation reservation) {
        LocalDate checkIn = reservation.getCheckIn();
        LocalDate checkOut = reservation.getCheckOut();
        BigDecimal basePrice = reservation.getRoom().getPricePerNight();
        long totalNights = ChronoUnit.DAYS.between(checkIn, checkOut);

        BigDecimal nightsTotal = BigDecimal.ZERO;
        for (LocalDate night = checkIn; night.isBefore(checkOut); night = night.plusDays(1)) {
            nightsTotal = nightsTotal.add(calculateNightPrice(basePrice, night));
        }

        nightsTotal = applyLongStayDiscount(nightsTotal, totalNights);
        nightsTotal = applyLeadTimeAdjustment(nightsTotal, checkIn);

        return nightsTotal.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateNightPrice(BigDecimal basePrice, LocalDate night) {
        BigDecimal nightPrice = basePrice;

        Month month = night.getMonth();
        if (month == Month.JULY || month == Month.AUGUST) {
            nightPrice = nightPrice.multiply(HIGH_SEASON_MULTIPLIER);
        } else if (isLowSeason(month)) {
            nightPrice = nightPrice.multiply(LOW_SEASON_MULTIPLIER);
        }

        DayOfWeek dayOfWeek = night.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.FRIDAY || dayOfWeek == DayOfWeek.SATURDAY) {
            nightPrice = nightPrice.multiply(WEEKEND_MULTIPLIER);
        }

        return nightPrice;
    }

    private BigDecimal applyLongStayDiscount(BigDecimal nightsTotal, long totalNights) {
        if (totalNights >= 14) {
            return nightsTotal.multiply(LONG_STAY_TIER2_MULTIPLIER);
        } else if (totalNights >= 7) {
            return nightsTotal.multiply(LONG_STAY_TIER1_MULTIPLIER);
        }
        return nightsTotal;
    }

    private BigDecimal applyLeadTimeAdjustment(BigDecimal nightsTotal, LocalDate checkIn) {
        long daysUntilCheckIn = ChronoUnit.DAYS.between(LocalDate.now(), checkIn);
        if (daysUntilCheckIn >= 30) {
            return nightsTotal.multiply(EARLY_BOOKING_MULTIPLIER);
        } else if (daysUntilCheckIn <= 3) {
            return nightsTotal.multiply(LAST_MINUTE_MULTIPLIER);
        }
        return nightsTotal;
    }

    private boolean isLowSeason(Month month) {
        return month == Month.NOVEMBER || month == Month.DECEMBER
                || month == Month.JANUARY || month == Month.FEBRUARY;
    }
}