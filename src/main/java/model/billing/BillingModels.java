package model.billing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class BillingModels {
    private BillingModels() { }
    public record InvoiceRow(long id, String number, long feeId, String patientId,
            String patientName, LocalDateTime issuedAt, BigDecimal total,
            BigDecimal insurance, BigDecimal patientPay, BigDecimal paid,
            BigDecimal remaining, String status) { }
    public record PaymentMethod(String id, String name) {
        @Override public String toString() { return name; }
    }
    public record PaymentDraft(long invoiceId, String methodId, BigDecimal amount,
            String reference, String note) { }
    public record Summary(long total, long unpaid, long paid, BigDecimal revenue) { }
}
