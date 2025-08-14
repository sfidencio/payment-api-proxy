package dto;

import java.math.BigDecimal;
import java.time.Instant;

public class PaymentRequest {
    private String correlationId;
    private BigDecimal amount;
    private String gatewayType = "default";
    private Instant requestAt;
    private int statusCode = 0;
    private int retryCount = 0;
    private String status = "PENDING";

    public PaymentRequest(String correlationId, BigDecimal amount) {
        this.correlationId = correlationId;
        this.amount = amount;
    }

    public PaymentRequest(String correlationId, BigDecimal amount, String gatewayType, Instant requestAt, int statusCode) {
        this.correlationId = correlationId;
        this.amount = amount;
        this.gatewayType = gatewayType;
        this.requestAt = requestAt;
        this.statusCode = statusCode;
    }

    public PaymentRequest(String correlationId, BigDecimal amount, String gatewayType, Instant requestAt, int statusCode, int retryCount, String status) {
        this.correlationId = correlationId;
        this.amount = amount;
        this.gatewayType = gatewayType;
        this.requestAt = requestAt;
        this.statusCode = statusCode;
        this.retryCount = retryCount;
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public Instant getRequestAt() {
        return requestAt;
    }

    public void setRequestAt(Instant requestAt) {
        this.requestAt = requestAt;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public BigDecimal getAmount() {
        return this.amount;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setGatewayType(String gatewayType) {
        this.gatewayType = gatewayType;
    }

    public String getGatewayType() {
        return gatewayType;
    }

    @Override
    public String toString() {
        return "PaymentRequest{" +
                "correlationId='" + this.correlationId + '\'' +
                ", amount=" + this.amount +
                ", requestAt=" + this.requestAt +
                '}';
    }
}

