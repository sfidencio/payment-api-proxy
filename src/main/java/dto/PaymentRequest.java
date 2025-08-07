package dto;

import java.time.Instant;

public class PaymentRequest {
    private String correlationId;
    private long amountInCents;
    private Instant requestAt = Instant.now();

    public PaymentRequest(String correlationId, long amountInCents) {
        this.correlationId = correlationId;
        this.amountInCents = amountInCents;
    }

    public Instant getRequestAt() {
        return requestAt;
    }

    public void setRequestAt(Instant requestAt) {
        this.requestAt = requestAt;
    }

    public long getAmountInCents() {
        return amountInCents;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    @Override
    public String toString() {
        return "PaymentRequest{" +
                "correlationId='" + correlationId + '\'' +
                ", amountInCents=" + amountInCents +
                ", requestAt=" + requestAt +
                '}';
    }
}

