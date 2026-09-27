package org.hbmc.util;

import org.hbmc.exception.InvalidReservationException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class DateUtils {

    private DateUtils() {}

    public static boolean periodsOverlap(LocalDate reservedCheckIn, LocalDate reservedCheckOut, LocalDate newCheckIn, LocalDate newCheckOut) {
        return newCheckIn.isBefore(reservedCheckOut) && newCheckOut.isAfter(reservedCheckIn);
    }

    public static long nightsBetween(LocalDate checkIn, LocalDate checkOut) {
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public static void validateReservationPeriod(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) throw new InvalidReservationException("Dates are required");
        if (checkIn.isBefore(LocalDate.now())) throw new InvalidReservationException("Check-in date must be today or later");
        if (!checkIn.isBefore(checkOut)) throw new InvalidReservationException("Check-out date must be after check-in date");
    }
}