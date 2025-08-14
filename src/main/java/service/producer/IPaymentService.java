package service.producer;

import dto.PaymentRequest;
import dto.PaymentSummaryByGatewayResponse;
import io.vertx.core.Future;

import java.time.Instant;

public interface IPaymentService {
    Future<Boolean> process(PaymentRequest request);

    Future<Boolean> enqueue(PaymentRequest request);

    Future<PaymentSummaryByGatewayResponse> getPaymentSummary(Instant from, Instant to);
}
