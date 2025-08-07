package handler;

import config.Environment;
import dto.PaymentRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import service.IPaymentService;

import java.math.BigDecimal;
import java.util.logging.Logger;

public class PaymentHandler implements io.vertx.core.Handler<RoutingContext> {
    private static final Logger logger = Logger.getLogger(PaymentHandler.class.getName());

    private final IPaymentService paymentService;

    public PaymentHandler(IPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @Override
    public void handle(RoutingContext ctx) {
        Environment.processLogging(
                logger,
                "Received payment processing request"
        );
        ctx.request().body().onComplete(ar -> {
            if (ar.succeeded()) {
                try {
                    var json = new JsonObject(ar.result().toString());
                    var request = new PaymentRequest(
                            json.getString("correlationId"),
                            BigDecimal.valueOf(json.getDouble("amount")).movePointRight(2).longValue()
                    );
                    this.paymentService.process(request)
                            .onSuccess(success -> {
                                ctx.response()
                                        .setStatusCode(success ? 200 : 502)
                                        .end(success ? "Payment processed successfully" : "Payment processing failed");
                            }).onFailure(
                                    throwable -> {
                                        ctx.response()
                                                .setStatusCode(502)
                                                .end("Payment processing failed: " + throwable.getMessage());
                                        Environment.processLogging(
                                                logger,
                                                "Failed to process payment request: " + throwable.getMessage()
                                        );
                                    }
                            );

                } catch (Exception e) {
                    ctx.response().setStatusCode(400).end("Invalid request body");
                    Environment.processLogging(
                            logger,
                            "Failed to process payment request: " + e.getMessage()
                    );
                }

            } else {
                ctx.response().setStatusCode(400).end("Invalid request body");
                Environment.processLogging(
                        logger,
                        "Failed to process payment request: " + ar.cause().getMessage()
                );
            }
        });
    }
}
