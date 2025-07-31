package dto;

import java.math.BigDecimal;
import java.time.Instant;

public class PaymentRequest {
    private String correlationId;
    private BigDecimal amount;
    private Instant requestAt = Instant.now();

    public PaymentRequest(String correlationId, BigDecimal amount) {
        this.correlationId = correlationId;
        this.amount = amount;
    }

    public Instant getRequestAt() {
        return requestAt;
    }

    public void setRequestAt(Instant requestAt) {
        this.requestAt = requestAt;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    @Override
    public String toString() {
        return "PaymentRequest{" +
                "correlationId='" + correlationId + '\'' +
                ", amount=" + amount +
                ", requestAt=" + requestAt +
                '}';
    }
}

