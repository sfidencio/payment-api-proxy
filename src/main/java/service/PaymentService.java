package service;

import config.Environment;
import config.PaymentDependencies;
import dto.PaymentProcessorResponse;
import dto.PaymentRequest;
import dto.PaymentSummaryByGatewayResponse;

import java.time.Instant;
import java.util.logging.Logger;

import static config.Constants.MSG_INSTANCE;
import static config.Constants.MSG_PAYMENT_FAILED_DEFAULT_AND_FALLBACK_GATEWAY;

public class PaymentService {

    private static final Logger logger = Logger.getLogger(PaymentService.class.getName());

    private PaymentService() {
        throw new IllegalStateException(MSG_INSTANCE);
    }

    public static boolean processPayment(PaymentRequest request) {
        var paymentGateway = PaymentDependencies.getInstance().getPaymentProcess();
        var paymentRepository = PaymentDependencies.getInstance().getRepository();

        paymentGateway.setContingency(false);

        boolean fallbackTried = false;
        int lastStatusCode = 0;
        try {

            if (paymentRepository.getByCorrelationId(request.getCorrelationId())) {
                Environment.processLogging(logger, "Record already exists in the database. Gateway: ".concat(" Data: ").concat(request.toString()));
                return false;
            }

            var response = paymentGateway.process(request);
//            var response = new PaymentProcessorResponse(
//                    "Payment processed successfully",
//                    "DefaultGateway",
//                    200
//            );
            lastStatusCode = response != null ? response.statusCode() : 0;
            if (lastStatusCode == 200) {
                paymentRepository.save(
                        request.getCorrelationId(),
                        response.gatewayType(),
                        request.getAmount(),
                        request.getRequestAt()
                );
                return true;
            }


            if (lastStatusCode == 422) {
                Environment.processLogging(logger, "Record already exists in the database. Gateway: " + response.gatewayType());
                return false;
            }

            paymentGateway.setContingency(true);
            fallbackTried = true;
            response = paymentGateway.process(request);
            lastStatusCode = response != null ? response.statusCode() : 0;
            if (lastStatusCode == 200) {
                paymentRepository.save(
                        request.getCorrelationId(),
                        response.gatewayType(),
                        request.getAmount(),
                        request.getRequestAt()
                );
                return true;
            }

            if (lastStatusCode == 422) {
                Environment.processLogging(logger, "Record already exists in the database. Gateway: " + response.gatewayType());
                return false;
            }

            Environment
                    .processLogging(logger,
                            MSG_PAYMENT_FAILED_DEFAULT_AND_FALLBACK_GATEWAY
                    );
            return false;
        } catch (Exception ex) {
            if (!fallbackTried && ((lastStatusCode != 200 && lastStatusCode != 422))) {
                try {
                    paymentGateway.setContingency(true);
                    var response = paymentGateway.process(request);
                    int status = response != null ? response.statusCode() : 0;
                    if (status == 200) {
                        paymentRepository.save(
                                request.getCorrelationId(),
                                response.gatewayType(),
                                request.getAmount(),
                                request.getRequestAt()
                        );
                        return true;
                    }
                    if (status == 422) {
                        Environment.processLogging(logger, "Record already exists in the database. Gateway: " + response.gatewayType());
                        return false;
                    }
                    Environment.processLogging(logger, MSG_PAYMENT_FAILED_DEFAULT_AND_FALLBACK_GATEWAY);
                } catch (Exception e) {
                    Environment
                            .processLogging(
                                    logger,
                                    MSG_PAYMENT_FAILED_DEFAULT_AND_FALLBACK_GATEWAY.concat(" Default: ").concat(ex.getMessage()).concat(", Fallback: ").concat(e.getMessage())
                            );
                }
            }
        }
        return false;
    }

    public static PaymentSummaryByGatewayResponse getPaymentSummary(Instant from, Instant to) {
        var paymentRepository = PaymentDependencies.getInstance().getRepository();

        var paymentSummary = paymentRepository.get(from, to);

        if (paymentSummary == null || paymentSummary.paymentSummaryByGateway().isEmpty()) {
            Environment.processLogging(logger, "No records found for the given date range.");
            return null;
        }

        Environment.processLogging(logger, "Payment summary retrieved successfully.");

        return paymentSummary;
    }
}
