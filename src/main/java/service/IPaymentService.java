package service;

import dto.PaymentRequest;
import dto.PaymentSummaryByGatewayResponse;
import io.vertx.core.Future;

import java.time.Instant;

public interface IPaymentService {
    Future<Boolean> process(PaymentRequest request);

    Future<PaymentSummaryByGatewayResponse> getPaymentSummary(Instant from, Instant to);
}
