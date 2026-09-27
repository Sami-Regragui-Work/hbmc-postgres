package org.hbmc.repository;

import org.hbmc.model.Invoice;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository {
    Invoice save(Invoice invoice);
    Invoice save(Invoice invoice, Connection connection);
    Invoice update(Invoice invoice);
    Invoice update(Invoice invoice, Connection connection);
    Optional<Invoice> findById(int id);
    Optional<Invoice> findByPaymentId(int paymentId);
    List<Invoice> findAll();
}