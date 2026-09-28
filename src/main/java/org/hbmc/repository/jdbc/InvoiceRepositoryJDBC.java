package org.hbmc.repository.jdbc;

import org.hbmc.db.DatabaseConnection;
import org.hbmc.model.Invoice;
import org.hbmc.model.Payment;
import org.hbmc.repository.InvoiceRepository;
import org.hbmc.repository.RowMapper;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;

public class InvoiceRepositoryJDBC implements InvoiceRepository {

    private final JdbcHelper jdbcHelper;
    private final PaymentRepositoryJDBC paymentRepository;
    private final RowMapper<Invoice> invoiceMapper;

    public InvoiceRepositoryJDBC() {
        this.jdbcHelper = new JdbcHelper(DatabaseConnection.getInstance().getConnection());
        this.paymentRepository = new PaymentRepositoryJDBC();

        this.invoiceMapper = rs -> {
            int paymentId = rs.getInt("payment_id");
            Payment payment = this.paymentRepository.findById(paymentId)
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
        return this.save(invoice, this.jdbcHelper);
    }

    @Override
    public Invoice save(Invoice invoice, Connection transactionConnection) {
        return this.save(invoice, new JdbcHelper(transactionConnection));
    }

    private Invoice save(Invoice invoice, JdbcHelper helper) {
        String sql = "INSERT INTO invoices (payment_id, off_tax, tax) VALUES (?, ?, ?)";
        int id = helper.insertReturningId(
                sql,
                invoice.getPayment().getId(),
                invoice.getOffTax(),
                invoice.getTax()
        );
        invoice.setId(id);
        return invoice;
    }

    @Override
    public Invoice update(Invoice invoice) {
        return this.update(invoice, this.jdbcHelper);
    }

    @Override
    public Invoice update(Invoice invoice, Connection transactionConnection) {
        return this.update(invoice, new JdbcHelper(transactionConnection));
    }

    private Invoice update(Invoice invoice, JdbcHelper helper) {
        String sql = "UPDATE invoices SET off_tax = ?, tax = ? WHERE id = ?";
        int rowsAffected = helper.update(
                sql,
                invoice.getOffTax(),
                invoice.getTax(),
                invoice.getId()
        );
        if (rowsAffected == 0) {
            throw new RuntimeException("No invoice found with id: " + invoice.getId());
        }
        return invoice;
    }

    @Override
    public Optional<Invoice> findById(int id) {
        String sql = "SELECT * FROM invoices WHERE id = ?";
        return this.jdbcHelper.queryOne(sql, this.invoiceMapper, id);
    }

    @Override
    public Optional<Invoice> findByPaymentId(int paymentId) {
        String sql = "SELECT * FROM invoices WHERE payment_id = ?";
        return this.jdbcHelper.queryOne(sql, this.invoiceMapper, paymentId);
    }

    @Override
    public List<Invoice> findAll() {
        String sql = "SELECT * FROM invoices";
        return this.jdbcHelper.queryList(sql, this.invoiceMapper);
    }
}
