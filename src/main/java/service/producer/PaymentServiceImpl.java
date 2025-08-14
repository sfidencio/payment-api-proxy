package service.producer;

import config.Environment;
import dto.PaymentRequest;
import dto.PaymentSummaryByGatewayResponse;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import repository.IPaymentRepository;
import service.processor.IPaymentProcess;

import java.time.Instant;
import java.util.logging.Logger;

public class PaymentServiceImpl implements IPaymentService {

    private static final Logger logger = Logger.getLogger(PaymentServiceImpl.class.getName());
    private final Vertx vertx;
    private final IPaymentProcess paymentGateway;
    private final IPaymentRepository paymentRepository;

    public PaymentServiceImpl(Vertx vertx, IPaymentRepository paymentRepository, IPaymentProcess paymentGateway) {
        this.vertx = vertx;
        this.paymentGateway = paymentGateway;
        this.paymentRepository = paymentRepository;
    }

    @Override
    public Future<Boolean> process(PaymentRequest request) {
        Promise<Boolean> promise = Promise.promise();

        this.attemptPayment(request, promise);

        return promise.future();
    }


    private void attemptPayment(PaymentRequest request, Promise<Boolean> promise) {
        Environment.processLogging(
                logger,
                "Attempting payment for correlation ID: " + request.getCorrelationId() + ", amount: " + request.getAmount()
        );

        this.paymentGateway.process(request).onComplete(paymentAr -> {
            if (paymentAr.succeeded() && paymentAr.result() != null) {
                var result = paymentAr.result();
                // Fluxo normal (sucesso)
                request.setGatewayType(result.gatewayType());
                request.setStatusCode(result.statusCode());
                this.paymentRepository.save(request).onComplete(saveAr -> {
                    if (saveAr.succeeded()) {
                        Environment.processLogging(
                                logger,
                                "Payment saved in database successfully for correlation ID: " + request.getCorrelationId()
                        );
                        promise.complete(true);
                    } else {
                        Environment.processLogging(
                                logger,
                                "Failed to save payment in database for correlation ID: " + request.getCorrelationId()
                        );
                        promise.fail(saveAr.cause());
                    }
                });
            } else {
                // Se for erro 422, apenas salva o status e não reenfileira
                if (paymentAr.result() != null && paymentAr.result().statusCode() == 422) {
                    request.setStatusCode(422);
                    this.paymentRepository.save(request).onComplete(saveAr -> {
                        if (saveAr.succeeded()) {
                            Environment.processLogging(
                                    logger,
                                    "Payment with 422 saved in database for correlation ID: " + request.getCorrelationId()
                            );
                            promise.complete(true);
                        } else {
                            Environment.processLogging(
                                    logger,
                                    "Failed to save payment with 422 for correlation ID: " + request.getCorrelationId()
                            );
                            promise.fail(saveAr.cause());
                        }
                    });
                }
            }
        });
    }


    @Override
    public Future<Boolean> enqueue(PaymentRequest request) {
        return this.paymentRepository.enqueue(request)
                .onComplete(ar -> {
                    if (ar.succeeded() && ar.result()) {
                        Environment.processLogging(
                                logger,
                                "Payment request enqueued successfully for correlation ID: " + request.getCorrelationId()
                        );
                    } else {
                        Environment.processLogging(
                                logger,
                                "Failed to enqueue payment request for correlation ID: " + request.getCorrelationId()
                        );
                    }
                });
    }


    @Override
    public Future<PaymentSummaryByGatewayResponse> getPaymentSummary(Instant from, Instant to) {
        Promise<PaymentSummaryByGatewayResponse> promise = Promise.promise();
        paymentRepository.get(from, to).onComplete(ar -> {
            if (ar.failed() || ar.result() == null || ar.result().paymentSummaryByGateway().isEmpty()) {
                Environment.processLogging(logger, "No records found for the given date range.");
                promise.complete(null);
                return;
            }
            Environment.processLogging(logger, "Payment summary retrieved successfully.");
            promise.complete(ar.result());
        });
        return promise.future();
    }
}
