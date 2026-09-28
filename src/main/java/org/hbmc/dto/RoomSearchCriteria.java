package org.hbmc.dto;

import org.hbmc.model.enums.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RoomSearchCriteria {

    private LocalDate checkIn;
    private LocalDate checkOut;
    private RoomType type;
    private Integer minCapacity;
    private BigDecimal maxPricePerNight;

    public RoomSearchCriteria(LocalDate checkIn, LocalDate checkOut) {
        this.checkIn = checkIn;
        this.checkOut = checkOut;
    }

    public LocalDate getCheckIn() {
        return this.checkIn;
    }

    public LocalDate getCheckOut() {
        return this.checkOut;
    }

    public RoomType getType() {
        return this.type;
    }

    public void setType(RoomType type) {
        this.type = type;
    }

    public Integer getMinCapacity() {
        return this.minCapacity;
    }

    public void setMinCapacity(Integer minCapacity) {
        this.minCapacity = minCapacity;
    }

    public BigDecimal getMaxPricePerNight() {
        return this.maxPricePerNight;
    }

    public void setMaxPricePerNight(BigDecimal maxPricePerNight) {
        this.maxPricePerNight = maxPricePerNight;
    }
}
