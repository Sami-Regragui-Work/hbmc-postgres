package org.hbmc.repository;

import org.hbmc.model.Room;
import org.hbmc.model.enums.RoomStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RoomRepository {
    Room save(Room room);
    Room update(Room room);
    Optional<Room> findById(int id);
    Optional<Room> findByRoomNumber(String roomNumber);
    List<Room> findAll();
    List<Room> findByStatus(RoomStatus status);
    List<Room> findAvailableRooms(LocalDate checkIn, LocalDate checkOut);
    boolean hasOverlap(int roomId, LocalDate checkIn, LocalDate checkOut);
    void delete(int id);
}