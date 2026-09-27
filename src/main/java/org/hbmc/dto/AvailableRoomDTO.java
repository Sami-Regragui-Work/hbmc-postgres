package org.hbmc.dto;

import org.hbmc.model.Room;
import org.hbmc.model.enums.RoomType;

import java.math.BigDecimal;

public class AvailableRoomDTO {

    private final int roomId;
    private final String roomNumber;
    private final RoomType type;
    private final int capacity;
    private final BigDecimal estimatedTotalPrice;

    public AvailableRoomDTO(Room room, BigDecimal estimatedTotalPrice) {
        this.roomId = room.getId();
        this.roomNumber = room.getRoomNumber();
        this.type = room.getType();
        this.capacity = room.getCapacity();
        this.estimatedTotalPrice = estimatedTotalPrice;
    }

    public int getRoomId() {
        return roomId;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public RoomType getType() {
        return type;
    }

    public int getCapacity() {
        return capacity;
    }

    public BigDecimal getEstimatedTotalPrice() {
        return estimatedTotalPrice;
    }
}