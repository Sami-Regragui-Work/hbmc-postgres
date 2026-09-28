package org.hbmc.service;

import org.hbmc.dto.AvailableRoomDTO;
import org.hbmc.model.Room;
import org.hbmc.policy.PricingStrategy;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PricingService {

    private final PricingStrategy pricingStrategy;

    public PricingService(PricingStrategy pricingStrategy) {
        this.pricingStrategy = pricingStrategy;
    }

    public BigDecimal quotePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        return this.pricingStrategy.calculatePrice(room, checkIn, checkOut);
    }

    public AvailableRoomDTO quoteForRoom(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal price = this.quotePrice(room, checkIn, checkOut);
        return new AvailableRoomDTO(room, price);
    }
}
