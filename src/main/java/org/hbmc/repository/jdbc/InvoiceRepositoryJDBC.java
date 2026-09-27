package org.hbmc.repository.jdbc;

import org.hbmc.db.DatabaseConnection;
import org.hbmc.model.Invoice;
import org.hbmc.model.Payment;
import org.hbmc.repository.InvoiceRepository;
import org.hbmc.repository.RowMapper;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InvoiceRepositoryJDBC implements InvoiceRepository {

    private final Connection connection;
    private final PaymentRepositoryJDBC paymentRepository;
    private final RowMapper<Invoice> invoiceMapper;

    public InvoiceRepositoryJDBC() {
        this.connection = DatabaseConnection.getInstance().getConnection();
        this.paymentRepository = new PaymentRepositoryJDBC();

        this.invoiceMapper = rs -> {
            int paymentId = rs.getInt("payment_id");
            Payment payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> new RuntimeException("Referenced payment not found: " + paymentId));

            Invoice invoice = new Invoice(
                    payment,
                    rs.getBigDecimal("off_tax"),
                    rs.getBigDecimal("tax")
            );
            invoice.setId(rs.getInt("id"));
            return invoice;
        };
    }

    @Override
    public Invoice save(Invoice invoice) {
        return save(invoice, connection);
    }

    @Override
    public Invoice save(Invoice invoice, Connection transactionConnection) {
        String sql = "INSERT INTO invoices (payment_id, off_tax, tax) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = transactionConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, invoice.getPayment().getId());
            stmt.setBigDecimal(2, invoice.getOffTax());
            stmt.setBigDecimal(3, invoice.getTax());

            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    invoice.setId(keys.getInt(1));
                }
            }
            return invoice;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save invoice: " + e.getMessage(), e);
        }
    }

    @Override
    public Invoice update(Invoice invoice) {
        return update(invoice, connection);
    }

    @Override
    public Invoice update(Invoice invoice, Connection transactionConnection) {
        String sql = "UPDATE invoices SET off_tax = ?, tax = ? WHERE id = ?";
        try (PreparedStatement stmt = transactionConnection.prepareStatement(sql)) {
            stmt.setBigDecimal(1, invoice.getOffTax());
            stmt.setBigDecimal(2, invoice.getTax());
            stmt.setInt(3, invoice.getId());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                throw new RuntimeException("No invoice found with id: " + invoice.getId());
            }
            return invoice;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update invoice: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Invoice> findById(int id) {
        String sql = "SELECT * FROM invoices WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(invoiceMapper.map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find invoice by id: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Invoice> findByPaymentId(int paymentId) {
        String sql = "SELECT * FROM invoices WHERE payment_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, paymentId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(invoiceMapper.map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find invoice by payment id: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Invoice> findAll() {
        String sql = "SELECT * FROM invoices";
        List<Invoice> invoices = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                invoices.add(invoiceMapper.map(rs));
            }
            return invoices;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all invoices: " + e.getMessage(), e);
        }
    }
}