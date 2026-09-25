package org.hbmc.policy;

import org.hbmc.model.Reservation;

import java.math.BigDecimal;

public interface PricingStrategy {
    BigDecimal calculatePrice(Reservation reservation);
}