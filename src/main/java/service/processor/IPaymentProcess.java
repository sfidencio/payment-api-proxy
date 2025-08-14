package service.processor;

import dto.PaymentProcessorResponse;
import dto.PaymentRequest;
import io.vertx.core.Future;

public interface IPaymentProcess {
    Future<PaymentProcessorResponse> process(PaymentRequest paymentRequest);
}
