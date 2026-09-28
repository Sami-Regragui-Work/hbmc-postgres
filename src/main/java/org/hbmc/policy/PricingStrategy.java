package org.hbmc.policy;

import org.hbmc.model.Room;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface PricingStrategy {
    BigDecimal calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut);
}
