package org.hbmc.service;

import org.hbmc.model.Invoice;
import org.hbmc.repository.InvoiceRepository;

import java.util.List;
import java.util.Optional;

public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    public InvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    public Optional<Invoice> findById(int id) {
        return this.invoiceRepository.findById(id);
    }

    public Optional<Invoice> findByPaymentId(int paymentId) {
        return this.invoiceRepository.findByPaymentId(paymentId);
    }

    public List<Invoice> findAll() {
        return this.invoiceRepository.findAll();
    }
}
