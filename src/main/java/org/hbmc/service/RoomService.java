package org.hbmc.service;

import org.hbmc.model.Room;
import org.hbmc.model.enums.RoomStatus;
import org.hbmc.repository.RoomRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public Room createRoom(String roomNumber, org.hbmc.model.enums.RoomType type, int capacity, java.math.BigDecimal pricePerNight) {
        if (roomRepository.findByRoomNumber(roomNumber).isPresent()) {
            throw new IllegalArgumentException("Room number already exists: " + roomNumber);
        }
        Room room = new Room(roomNumber, type, capacity, pricePerNight);
        return roomRepository.save(room);
    }

    public Room updateRoomStatus(int roomId, RoomStatus newStatus) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        room.setStatus(newStatus);
        return roomRepository.update(room);
    }

    public Room updateRoomPrice(int roomId, java.math.BigDecimal newPrice) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found: " + roomId));
        room.setPricePerNight(newPrice);
        return roomRepository.update(room);
    }

    public Optional<Room> findById(int roomId) {
        return roomRepository.findById(roomId);
    }

    public List<Room> findAllRooms() {
        return roomRepository.findAll();
    }

    public List<Room> findAvailableRooms(LocalDate checkIn, LocalDate checkOut) {
        if (!checkIn.isBefore(checkOut)) {
            throw new IllegalArgumentException("Check-in date must be before check-out date");
        }
        return roomRepository.findAvailableRooms(checkIn, checkOut);
    }

    public boolean isRoomAvailable(int roomId, LocalDate checkIn, LocalDate checkOut) {
        if (!checkIn.isBefore(checkOut)) {
            throw new IllegalArgumentException("Check-in date must be before check-out date");
        }
        return !roomRepository.hasOverlap(roomId, checkIn, checkOut);
    }
}