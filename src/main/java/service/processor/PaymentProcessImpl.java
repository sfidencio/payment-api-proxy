package service.processor;

import config.Environment;
import config.WebClientProvider;
import dto.GatewaySelected;
import dto.PaymentProcessorResponse;
import dto.PaymentRequest;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import service.health.HealthCheckService;

import java.util.Map;
import java.util.logging.Logger;

import static config.Constants.*;

public class PaymentProcessImpl implements IPaymentProcess {

    private static final Logger logger = Logger.getLogger(PaymentProcessImpl.class.getName());
    private final WebClient webClient;

    public PaymentProcessImpl(Vertx vertx) {
        this.webClient = WebClientProvider.getInstance(vertx);
    }

    @Override
    public Future<PaymentProcessorResponse> process(PaymentRequest paymentRequest) {
        Promise<PaymentProcessorResponse> promise = Promise.promise();

        HealthCheckService.getBestGateway()
                .onSuccess(gatewaySelected -> {
                    var gateway = getGateway(gatewaySelected);
                    if (gateway == null) {
                        promise.complete(null);
                        return;
                    }
                    sendPayment(gateway, paymentRequest, promise);
                })
                .onFailure(promise::fail);

        return promise.future();
    }

    private GatewaySelected getGateway(Map<String, GatewaySelected> gatewaySelected) {
        return gatewaySelected != null ? gatewaySelected.get(PROCESSOR_GATEWAY_SELECTED) : null;
    }

    private void sendPayment(GatewaySelected gateway,
                             PaymentRequest paymentRequest,
                             Promise<PaymentProcessorResponse> promise) {
        var url = gateway.uri();
        var json = new JsonObject()
                .put(CORRELATION_ID, paymentRequest.getCorrelationId())
                .put(AMOUNT, paymentRequest.getAmount())
                .put(REQUESTED_AT, paymentRequest.getRequestAt());

        log("Processing payment request: " + json.encodePrettily() + " [ Gateway: " + url + " Type: " + gateway.name() + "]");

        webClient.postAbs(url)
                .putHeader("Content-Type", "application/json")
                .sendJsonObject(json)
                .onComplete(ar -> handleResponse(ar, gateway, paymentRequest, promise));
    }

    private void handleResponse(io.vertx.core.AsyncResult<io.vertx.ext.web.client.HttpResponse<io.vertx.core.buffer.Buffer>> ar,
                                GatewaySelected gateway,
                                PaymentRequest paymentRequest,
                                Promise<PaymentProcessorResponse> promise) {
        if (ar.failed()) {
            promise.fail(ar.cause());
            return;
        }
        var response = ar.result();

        log("Payment gateway response received. Status code: " + response.statusCode() + ", Body: " + response.bodyAsString());

        if (response.statusCode() == 200 || response.statusCode() == 422) {
            var body = response.bodyAsJsonObject();
            var resp = new PaymentProcessorResponse(
                    body.getString(MSG_RESPONSE_PAYMENT),
                    gateway.name(),
                    response.statusCode()
            );
            log(resp.message() + " [ Gateway: " + gateway.uri() + " Type: " + gateway.name() + ", DATA: " + paymentRequest + "]");
            promise.complete(resp);
        } else {
            log("Falha: - Gateway ->  " + gateway.uri());
            promise.complete(null);
        }
    }

    private void log(String msg) {
        Environment.processLogging(logger, msg);
    }
}