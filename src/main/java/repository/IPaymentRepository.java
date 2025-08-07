package repository;

import dto.PaymentSummaryByGatewayResponse;
import io.vertx.core.Future;

import java.time.Instant;

public interface IPaymentRepository {
    Future<Void> save(String correlationId,
                      String gatewayType,
                      long amountInCents,
                      Instant timestamp);

    Future<PaymentSummaryByGatewayResponse> get(Instant from, Instant to);
}
