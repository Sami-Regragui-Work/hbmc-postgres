package org.hbmc.service;

import org.hbmc.model.Payment;
import org.hbmc.model.enums.PaymentStatus;
import org.hbmc.repository.PaymentRepository;

import java.util.List;
import java.util.Optional;

public class PaymentService {

    private final PaymentRepository paymentRepository;

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    public Optional<Payment> findById(int id) {
        return this.paymentRepository.findById(id);
    }

    public Optional<Payment> findByReservationId(int reservationId) {
        return this.paymentRepository.findByReservationId(reservationId);
    }

    public List<Payment> findByStatus(PaymentStatus status) {
        return this.paymentRepository.findByStatus(status);
    }

    public List<Payment> findAll() {
        return this.paymentRepository.findAll();
    }
}
