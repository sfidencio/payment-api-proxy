package handler;

import config.Environment;
import io.vertx.ext.web.RoutingContext;
import service.IPaymentService;

import java.time.Instant;
import java.util.logging.Logger;

public class PaymentSummaryHandler implements io.vertx.core.Handler<RoutingContext> {
    private static final Logger logger = Logger.getLogger(PaymentSummaryHandler.class.getName());

    private final IPaymentService paymentService;

    public PaymentSummaryHandler(IPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public void handle(RoutingContext ctx) {
        String from = ctx.request().getParam("from");
        String to = ctx.request().getParam("to");
        try {
            Instant fromInstant = from == null ? null : Instant.parse(from);
            Instant toInstant = to == null ? null : Instant.parse(to);
            this.paymentService.getPaymentSummary(
                            fromInstant,
                            toInstant
                    ).onSuccess(summary -> {
                        if (summary == null || summary.paymentSummaryByGateway().isEmpty()) {
                            Environment.processLogging(
                                    logger,
                                    "No payment records found for the date range: from " + from + " to " + to
                            );
                        } else {
                            Environment.processLogging(
                                    logger,
                                    "Payment summary retrieved successfully for the date range: from " + from + " to " + to
                            );
                        }
                        ctx.response().setStatusCode(200).end(summary.toJson());
                    })
                    .onFailure(throwable -> {
                        ctx.response().setStatusCode(500).end("Internal server error");
                        Environment.processLogging(
                                logger,
                                "Failed to retrieve payment summary: " + throwable.getMessage()
                        );
                    });
        } catch (Exception e) {
            ctx.response().setStatusCode(400).end("Invalid request parameters");
            Environment.processLogging(
                    logger,
                    "Failed to process payment summary request: " + e.getMessage()
            );
        }
    }
}
