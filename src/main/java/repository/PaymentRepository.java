package repository;

import dto.PaymentSummaryByGatewayResponse;

import java.math.BigDecimal;
import java.time.Instant;

public interface PaymentRepository {
    void save(String correlationId, String gatewayType, BigDecimal amount, Instant createdAt);

    boolean getByCorrelationId(String correlationId);

    PaymentSummaryByGatewayResponse get(Instant from, Instant to);
}
