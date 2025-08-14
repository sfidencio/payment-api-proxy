package repository;

import dto.PaymentMessage;
import dto.PaymentRequest;
import dto.PaymentSummaryByGatewayResponse;
import io.vertx.core.Future;

import java.time.Instant;
import java.util.List;

public interface IPaymentRepository {
    Future<Boolean> save(PaymentRequest request);

    Future<PaymentSummaryByGatewayResponse> get(Instant from, Instant to);

    Future<List<PaymentMessage>> consumeBatch(int batchSize, String consumerName);

    Future<Void> ack(long messageId);

    Future<Boolean> enqueue(PaymentRequest request);

    //Future<Boolean> checkDuplicateCorrelationId(String correlationId);

    Future<Integer> incrementRetryCount(long id);
}
