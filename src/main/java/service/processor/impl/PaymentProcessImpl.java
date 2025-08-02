package service.processor.impl;

import config.Environment;
import config.HttpClientBuild;
import dto.GatewaySelected;
import dto.PaymentProcessorResponse;
import dto.PaymentRequest;
import org.json.JSONObject;
import service.HealthCheckService;
import service.processor.IPaymentProcess;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import static config.Constants.*;

public class PaymentProcessImpl implements IPaymentProcess {

    private static final Logger logger = Logger.getLogger(PaymentProcessImpl.class.getName());

    private boolean fallback = false;

    @Override
    public PaymentProcessorResponse process(PaymentRequest paymentRequest) throws IOException, InterruptedException {
        Map<String, GatewaySelected> gatewaySelected = fallback
                ? Map.of(PROCESSOR_GATEWAY_SELECTED, new GatewaySelected(PROCESSOR_GATEWAY_SELECTED_FALLBACK, Environment.getEnv(PROCESSOR_FALLBACK).concat(PROCESSOR_POST_PAYMENT_URI)))
                : HealthCheckService.getBestGateway();

        var requestGateway = createRequest(
                Objects.requireNonNull(gatewaySelected).get(PROCESSOR_GATEWAY_SELECTED).uri(),
                paymentRequest
        );
        var responseGateway = HttpClientBuild.getInstance().send(requestGateway, HttpResponse.BodyHandlers.ofString());

        Environment.processLogging(logger, "HTTP_STATUS: ".concat(String.valueOf(responseGateway.statusCode()).concat("- Gateway ->  ").concat(gatewaySelected.get(PROCESSOR_GATEWAY_SELECTED).uri().concat(" Body: ").concat(responseGateway.body()))));

        if (responseGateway.statusCode() == 422) {
            Environment.processLogging(
                    logger,
                    "Unprocessable Entity: (Duplicity) ".concat("- Gateway ->  ").concat(gatewaySelected.get(PROCESSOR_GATEWAY_SELECTED).uri())
            );
            var respostaJson4xx = new JSONObject(responseGateway.body());
            return new PaymentProcessorResponse(
                    respostaJson4xx.getString(MSG_RESPONSE_PAYMENT),
                    gatewaySelected.get(PROCESSOR_GATEWAY_SELECTED).name(),
                    responseGateway.statusCode()
            );
        }

        if (responseGateway.statusCode() >= 200 && responseGateway.statusCode() < 300) {
            var respostaJson2xx = new JSONObject(responseGateway.body());
            var responseMessage = new PaymentProcessorResponse(
                    respostaJson2xx.getString(MSG_RESPONSE_PAYMENT),
                    gatewaySelected.get(PROCESSOR_GATEWAY_SELECTED).name(),
                    responseGateway.statusCode()
            );
            Environment.processLogging(
                    logger,
                    responseMessage.message().concat(" [ Gateway: ").concat(gatewaySelected.get(PROCESSOR_GATEWAY_SELECTED).uri().concat(" Type: ").concat(gatewaySelected.get(PROCESSOR_GATEWAY_SELECTED).name().concat(", DATA: ".concat(paymentRequest.toString()).concat("]"))))
            );
            return responseMessage;
        }


        Environment.processLogging(
                logger,
                "Falha: ".concat("- Gateway ->  ").concat(gatewaySelected.get(PROCESSOR_GATEWAY_SELECTED).uri())
        );
        return null;
    }

    @Override
    public void setContingency(boolean isFallback) {
        this.fallback = isFallback;
    }
}
