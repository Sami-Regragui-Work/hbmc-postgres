package org.hbmc.repository;

import org.hbmc.model.Payment;
import org.hbmc.model.enums.PaymentStatus;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
    Payment save(Payment payment);
    Payment save(Payment payment, Connection connection);
    Optional<Payment> findById(int id);
    Optional<Payment> findByReservationId(int reservationId);
    List<Payment> findByStatus(PaymentStatus status);
    List<Payment> findAll();
}