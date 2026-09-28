package org.hbmc.model;

import org.hbmc.model.enums.RoomStatus;
import org.hbmc.model.enums.RoomType;

import java.math.BigDecimal;
import java.util.Objects;

public class Room {
    private int id;
    private final String roomNumber;
    private RoomType type;
    private int capacity;
    private BigDecimal pricePerNight;
    private RoomStatus status;

    public Room(String roomNumber, RoomType type, int capacity, BigDecimal pricePerNight) {
        this(roomNumber, type, capacity, pricePerNight, RoomStatus.AVAILABLE);
    }

    public Room(String roomNumber, RoomType type, int capacity, BigDecimal pricePerNight, RoomStatus status) {
        this.roomNumber = roomNumber;
        this.type = type;
        this.capacity = capacity;
        this.pricePerNight = pricePerNight;
        this.status = status;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getRoomNumber() {
        return this.roomNumber;
    }

    public RoomType getType() {
        return this.type;
    }

    public void setType(RoomType type) {
        this.type = type;
    }

    public int getCapacity() {
        return this.capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public BigDecimal getPricePerNight() {
        return this.pricePerNight;
    }

    public void setPricePerNight(BigDecimal pricePerNight) {
        this.pricePerNight = pricePerNight;
    }

    public RoomStatus getStatus() {
        return this.status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Room room)) return false;
        return this.id == room.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }
}
