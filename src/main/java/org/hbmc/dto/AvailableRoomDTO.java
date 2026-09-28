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
        return this.roomId;
    }

    public String getRoomNumber() {
        return this.roomNumber;
    }

    public RoomType getType() {
        return this.type;
    }

    public int getCapacity() {
        return this.capacity;
    }

    public BigDecimal getEstimatedTotalPrice() {
        return this.estimatedTotalPrice;
    }
}
