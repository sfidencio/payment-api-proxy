package service.processor.impl;

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
import service.HealthCheckService;
import service.processor.IPaymentProcess;

import java.util.Map;
import java.util.logging.Logger;

import static config.Constants.*;

public class PaymentProcessImpl implements IPaymentProcess {

    private static final Logger logger = Logger.getLogger(PaymentProcessImpl.class.getName());
    private final Vertx vertx;
    private final WebClient webClient;
    private boolean fallback = false;

    public PaymentProcessImpl(Vertx vertx) {
        this.vertx = vertx;
        this.webClient = WebClientProvider.getInstance(vertx);
    }


    @Override
    public Future<PaymentProcessorResponse> process(PaymentRequest paymentRequest) {
        Promise<PaymentProcessorResponse> promise = Promise.promise();

        if (fallback) {
            var gatewaySelected = Map.of(
                    PROCESSOR_GATEWAY_SELECTED,
                    new GatewaySelected(PROCESSOR_GATEWAY_SELECTED_FALLBACK, Environment.getEnv(PROCESSOR_FALLBACK).concat(PROCESSOR_POST_PAYMENT_URI))
            );
            this.processPayment(
                    gatewaySelected,
                    paymentRequest,
                    promise
            );
        } else {
            HealthCheckService.getBestGateway().onSuccess(gatewaySelected -> {
                if (gatewaySelected == null || gatewaySelected.get(PROCESSOR_GATEWAY_SELECTED) == null) {
                    promise.complete(null);
                    return;
                }
                this.processPayment(
                        gatewaySelected,
                        paymentRequest,
                        promise
                );
            }).onFailure(promise::fail);
        }
        return promise.future();
    }

    private void processPayment(Map<String, GatewaySelected> gatewaySelected,
                                PaymentRequest paymentRequest, Promise<PaymentProcessorResponse> promise) {
        var url = gatewaySelected.get(PROCESSOR_GATEWAY_SELECTED).uri();
        var json = new JsonObject()
                .put(CORRELATION_ID, paymentRequest.getCorrelationId())
                .put(AMOUNT, paymentRequest.getAmountInCents())
                .put(REQUESTED_AT, paymentRequest.getRequestAt());

        Environment.processLogging(
                logger,
                "Processing payment request: " + json.encodePrettily() + " [ Gateway: " + url + " Type: " + gatewaySelected.get(PROCESSOR_GATEWAY_SELECTED).name() + "]"
        );

        this.webClient.postAbs(url)
                .putHeader(
                        "Content-Type",
                        "application/json"
                ).sendJsonObject(json)
                .onComplete(
                        ar -> {
                            if (ar.failed()) {
                                promise.fail(ar.cause());
                                return;
                            }
                            var response = ar.result();
                            Environment.processLogging(
                                    logger,
                                    "Payment gateway response received. Status code: " + response.statusCode() +
                                            ", Body: " + response.bodyAsString()
                            );
                            if (response.statusCode() == 422) {
                                Environment.processLogging(
                                        logger,
                                        "Payment gateway returned 422 Unprocessable Entity. Response: " + response.bodyAsString()
                                );
                                var reponseJson4xx = new JsonObject(response.bodyAsString());
                                promise.complete(
                                        new PaymentProcessorResponse(
                                                reponseJson4xx.getString(MSG_RESPONSE_PAYMENT),
                                                gatewaySelected.get(PROCESSOR_GATEWAY_SELECTED).name(),
                                                response.statusCode()
                                        )
                                );
                                return;
                            }

                            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                                var respostaJson2xx = response.bodyAsJsonObject();
                                var responseMessage = new PaymentProcessorResponse(
                                        respostaJson2xx.getString(MSG_RESPONSE_PAYMENT),
                                        gatewaySelected.get(PROCESSOR_GATEWAY_SELECTED).name(),
                                        response.statusCode()
                                );
                                Environment.processLogging(
                                        logger,
                                        responseMessage.message() + " [ Gateway: " + url + " Type: " + gatewaySelected.get(PROCESSOR_GATEWAY_SELECTED).name() + ", DATA: " + paymentRequest + "]"
                                );
                                promise.complete(responseMessage);
                                return;
                            }
                            Environment.processLogging(logger, "Falha: - Gateway ->  " + url);
                            promise.complete(null);

                        });
    }

    @Override
    public Future<Void> setContingency(boolean isFallback) {
        this.fallback = isFallback;
        return Future.succeededFuture();
    }
}
