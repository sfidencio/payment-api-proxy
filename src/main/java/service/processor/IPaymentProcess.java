package service.processor;

import dto.PaymentProcessorResponse;
import dto.PaymentRequest;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;

import static config.Constants.*;

public interface IPaymentProcess {
    PaymentProcessorResponse process(PaymentRequest paymentRequest) throws IOException, InterruptedException;

    void setContingency(boolean isFallback);

    default HttpRequest createRequest(String url, PaymentRequest request) {
        var jsonObject = new JSONObject();
        jsonObject.put(CORRELATION_ID, request.getCorrelationId());
        jsonObject.put(AMOUNT, request.getAmount());
        jsonObject.put(REQUESTED_AT, request.getRequestAt());
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonObject.toString()))
                .build();
    }
}
