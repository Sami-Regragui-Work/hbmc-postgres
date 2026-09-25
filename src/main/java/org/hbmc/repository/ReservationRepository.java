package org.hbmc.repository;

import org.hbmc.model.CanceledReservation;
import org.hbmc.model.Reservation;
import org.hbmc.model.enums.ReservationStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {
    Reservation save(Reservation reservation);
    Reservation update(Reservation reservation);
    Optional<Reservation> findById(int id);
    Optional<Reservation> findByReservationCode(String code);
    List<Reservation> findByClientId(int clientId);
    List<Reservation> findByRoomId(int roomId);
    List<Reservation> findByStatus(ReservationStatus status);
    List<Reservation> findAll();
    boolean hasOverlap(int roomId, LocalDate checkIn, LocalDate checkOut);
    CanceledReservation saveCancellation(CanceledReservation canceledReservation);
    Optional<CanceledReservation> findCancellationByReservationId(int reservationId);
}