package handler;

import dto.PaymentRequest;
import io.vertx.ext.web.RoutingContext;
import service.producer.IPaymentService;

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
        try {
            var json = ctx.body().asJsonObject();
            var correlationId = json.getString("correlationId");
            //var amountInCents = Long.parseLong(json.getString("amount").replace(".", ""));
            var amountInCents = new BigDecimal(json.getString("amount"));
            if (correlationId == null) {
                ctx.response().setStatusCode(400).end("Missing required fields");
                return;
            }
            var paymentRequest = new PaymentRequest(correlationId, amountInCents);


            this.paymentService.enqueue(paymentRequest)
                    .onSuccess(msg -> ctx.response().setStatusCode(200).end("Enqueued with ID: " + paymentRequest.getCorrelationId()))
                    .onFailure(err -> ctx.response().setStatusCode(500).end("Error enqueuing payment: " + err.getMessage()));
        } catch (Exception e) {
            ctx.response().setStatusCode(500)
                    .end("Error processing payment: " + e.getMessage());
        }
    }
}
