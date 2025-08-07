package service;

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
    /**
     * Maximum number of retry attempts per gateway (primary and contingency).
     * Each gateway will be attempted up to this number of times before switching or failing.
     */
    private static final int MAX_RETRIES = 2;

    /**
     * Constructs a new PaymentServiceImpl with required dependencies.
     *
     * @param vertx the Vert.x instance for asynchronous operations
     */
    public PaymentServiceImpl(Vertx vertx, IPaymentRepository paymentRepository, IPaymentProcess paymentGateway) {
        this.vertx = vertx;
        this.paymentGateway = paymentGateway;
        this.paymentRepository = paymentRepository;
    }


    /**
     * Processes a payment request with retry and fallback mechanism.
     *
     * <p>Processing Flow:
     * <ol>
     *   <li>Set payment gateway to primary mode (contingency = false)</li>
     *   <li>Check if payment with same correlation ID already exists</li>
     *   <li>If exists, return false (duplicate prevention)</li>
     *   <li>If not exists, start payment attempt process</li>
     * </ol>
     *
     * @param request the payment request containing correlation ID, amount, and timestamp
     * @return Future&lt;Boolean&gt; true if payment processed successfully, false if failed or duplicate
     * @throws RuntimeException if repository lookup fails
     * @see #attemptPayment(PaymentRequest, int, boolean, Promise)
     */
    @Override
    public Future<Boolean> process(PaymentRequest request) {


        this.paymentGateway.setContingency(false);

        Promise<Boolean> promise = Promise.promise();
//
//        this.paymentRepository.getByCorrelationId(request.getCorrelationId()).onComplete(ar -> {
//            if (ar.failed()) {
//                Environment.processLogging(
//                        logger,
//                        "Failed to retrieve payment by correlation ID: " + request.getCorrelationId() + ". Error: " + ar.cause().getMessage()
//                );
//                promise.fail(ar.cause());
//                return;
//            }
//            if (ar.result()) {
//                Environment.processLogging(
//                        logger,
//                        "Payment with correlation ID: " + request.getCorrelationId() + " already exists."
//                );
//                promise.complete(false);
//                return;
//            }
//
//        });

        this.attemptPayment(request, 1, false, promise);
        return promise.future();
    }


    /**
     * Attempts payment processing with retry and fallback logic.
     *
     * <p>This method implements the core retry and fallback strategy:
     *
     * <h4>Success Path:</h4>
     * <ul>
     *   <li>Payment gateway returns successful result</li>
     *   <li>Save payment record to repository</li>
     *   <li>Complete promise with true on successful save</li>
     *   <li>Fail promise if save operation fails</li>
     * </ul>
     *
     * <h4>Failure Path - Retry Logic:</h4>
     * <ol>
     *   <li><strong>Primary Gateway Retries:</strong> If attempt &lt; MAX_RETRIES AND not using contingency,
     *       retry with primary gateway (increment attempt counter)</li>
     *   <li><strong>Switch to Contingency:</strong> If primary gateway exhausted all retries,
     *       switch to contingency gateway (reset attempt counter to 1, set contingency = true)</li>
     *   <li><strong>Contingency Retries:</strong> If using contingency AND attempt &lt; MAX_RETRIES,
     *       retry with contingency gateway (increment attempt counter)</li>
     *   <li><strong>Complete Failure:</strong> If both gateways exhausted all retries,
     *       complete promise with false</li>
     * </ol>
     *
     * <h4>Retry Scenarios:</h4>
     * <pre>
     * Primary Gateway: Attempt 1 (FAIL) → Attempt 2 (FAIL)
     *                     ↓
     * Contingency Gateway: Attempt 1 (FAIL) → Attempt 2 (FAIL)
     *                     ↓
     * Complete Failure (return false)
     * </pre>
     *
     * @param request        the payment request to process
     * @param attempt        current attempt number (1-based, resets when switching gateways)
     * @param useContingency flag indicating whether to use contingency gateway
     * @param promise        promise to complete with processing result
     * @implNote This method uses recursive calls for retry logic, making it stack-safe
     * due to Vert.x's event loop model
     */
    private void attemptPayment(PaymentRequest request, int attempt, boolean useContingency, Promise<Boolean> promise) {
        this.paymentGateway.setContingency(useContingency);

        Environment.processLogging(
                logger,
                "Attempting payment for correlation ID: " + request.getCorrelationId() + ", attempt: " + attempt + ", contingency: " + useContingency
        );

        this.paymentGateway.process(request).onComplete(paymentAr -> {
            if (paymentAr.succeeded() && paymentAr.result() != null) {

                this.paymentRepository.save(
                        request.getCorrelationId(),
                        paymentAr.result().gatewayType(),
                        request.getAmountInCents(),
                        request.getRequestAt()
                ).onComplete(saveAr -> {
                    if (saveAr.succeeded()) {
                        Environment.processLogging(
                                logger,
                                "Payment saved successfully for correlation ID: " + request.getCorrelationId() + ", amount: " + request.getAmountInCents() + " cents"
                        );
                        promise.complete(true);
                    } else {
                        Environment.processLogging(
                                logger,
                                "Failed to save payment for correlation ID: " + request.getCorrelationId() + ". Error: " + saveAr.cause().getMessage()
                        );
                        promise.fail(saveAr.cause());
                    }
                });
            } else {
                // Payment failed
                Environment.processLogging(
                        logger,
                        "Payment processing failed for correlation ID: " + request.getCorrelationId() +
                                " - Attempt: " + attempt + " - Error: " +
                                (paymentAr.cause() != null ? paymentAr.cause().getMessage() : "Unknown error")
                );

                if (attempt < MAX_RETRIES && !useContingency) {
                    attemptPayment(request, attempt + 1, false, promise);
                } else if (!useContingency) {
                    Environment.processLogging(
                            logger,
                            "Switching to contingency gateway for correlation ID: " + request.getCorrelationId()
                    );
                    this.attemptPayment(request, 1, true, promise);
                } else {
                    Environment.processLogging(
                            logger,
                            "All payment attempts failed for correlation ID: " + request.getCorrelationId()
                    );
                    promise.complete(false);
                }
            }
        });
    }


    /**
     * Retrieves payment summary grouped by gateway type for a specified date range.
     *
     * <p>This method queries the payment repository to get aggregated payment data
     * within the specified time window.
     *
     * <h4>Return Scenarios:</h4>
     * <ul>
     *   <li><strong>Success:</strong> Returns PaymentSummaryByGatewayResponse with aggregated data</li>
     *   <li><strong>No Data:</strong> Returns null if no records found in date range</li>
     *   <li><strong>Error:</strong> Promise fails with repository error</li>
     * </ul>
     *
     * @param from start date (inclusive) for the summary period
     * @param to   end date (inclusive) for the summary period
     * @return Future&lt;PaymentSummaryByGatewayResponse&gt; payment summary or null if no data found
     * @throws RuntimeException if repository query fails
     * @apiNote Returns null instead of empty response when no records found
     * @since 1.0
     */

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
