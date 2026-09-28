package org.hbmc.service;

import org.hbmc.exception.InvalidInputException;
import org.hbmc.exception.ResourceAlreadyExistsException;
import org.hbmc.exception.RoomNotAvailableException;
import org.hbmc.exception.RoomNotFoundException;
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
        if (this.roomRepository.findByRoomNumber(roomNumber).isPresent()) {
            throw new ResourceAlreadyExistsException("Room number already exists: " + roomNumber);
        }
        Room room = new Room(roomNumber, type, capacity, pricePerNight);
        return this.roomRepository.save(room);
    }

    public Room updateRoomStatus(int roomId, RoomStatus newStatus) {
        Room room = this.roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomId));
        room.setStatus(newStatus);
        return this.roomRepository.update(room);
    }

    public Room updateRoomPrice(int roomId, java.math.BigDecimal newPrice) {
        Room room = this.roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException("Room not found: " + roomId));
        room.setPricePerNight(newPrice);
        return this.roomRepository.update(room);
    }

    public Optional<Room> findById(int roomId) {
        return this.roomRepository.findById(roomId);
    }

    public List<Room> findAllRooms() {
        return this.roomRepository.findAll();
    }

    public List<Room> findAvailableRooms(LocalDate checkIn, LocalDate checkOut) {
        if (!checkIn.isBefore(checkOut)) {
            throw new InvalidInputException("Check-in date must be before check-out date");
        }
        return this.roomRepository.findAvailableRooms(checkIn, checkOut);
    }

    public boolean isRoomAvailable(int roomId, LocalDate checkIn, LocalDate checkOut) {
        if (!checkIn.isBefore(checkOut)) {
            throw new InvalidInputException("Check-in date must be before check-out date");
        }
        return !this.roomRepository.hasOverlap(roomId, checkIn, checkOut);
    }

    public void assertRoomAvailable(int roomId, LocalDate checkIn, LocalDate checkOut) {
        if (!this.isRoomAvailable(roomId, checkIn, checkOut)) {
            throw new RoomNotAvailableException("Room " + roomId + " is not available from " + checkIn + " to " + checkOut);
        }
    }
}
