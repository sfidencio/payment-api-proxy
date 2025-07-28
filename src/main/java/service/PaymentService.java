package service;

import config.HttpClientHelper;
import config.RedisClientHelper;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static config.Constants.*;

public class PaymentService {


    private static final AtomicBoolean processorDefaultGatewayActive = new AtomicBoolean(true);
    private static final AtomicBoolean processorFallbackGatewayActive = new AtomicBoolean(true);
    private static final Logger logger = Logger.getLogger(PaymentService.class.getName());


    private PaymentService() {
        throw new IllegalStateException(MSG_INSTANCE);
    }

    public static String process(String payload) {
        logger.info(MSG_PROCESSING_PAYMENT_PAYLOAD.concat(payload));

        var requestGateway = createRequest(DEFAULT_GATEWAY_URL, payload);

        if (processorDefaultGatewayActive.get()) {
            try {
                var response = HttpClientHelper.getInstance().send(requestGateway, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    logger.info(MSG_GATEWAY_PROCESSED_SUCCESSFULY);


                    var jsonRedis = buildResponse(
                            new JSONObject(
                                    response.body()
                            ),
                            DEFAULT_GATEWAY_URL,
                            Instant.now(),
                            5);

                    RedisClientHelper.getInstance().set(jsonRedis.getString(MSG_CORRELATION_ID), jsonRedis.toString());

                    return response.body();
                }
            } catch (Exception e) {
                processorDefaultGatewayActive.set(false);
                logger.warning(MSG_DEFAULT_GATEWAY_FAILED.concat(e.getMessage()));
            }
        }

        var requestFallback = createRequest(FALLBACK_GATEWAY_URL, payload);
        if (processorFallbackGatewayActive.get()) {
            try {
                var response = HttpClientHelper.getInstance().send(requestFallback, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    logger.info(MSG_GATEWAY_PROCESSED_SUCCESSFULY);
                    var input = new JSONObject(response.body());
                    UUID correlationId = UUID.fromString(input.getString(MSG_CORRELATION_ID));
                    RedisClientHelper.getInstance().set(correlationId.toString(), response.body());
                    return response.body();
                }
            } catch (Exception e) {
                processorDefaultGatewayActive.set(false);
                logger.warning(MSG_DEFAULT_GATEWAY_FAILED.concat(e.getMessage()));
            }
        }

        return null;
    }

    public static HttpRequest createRequest(String url, String body) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private static JSONObject buildResponse(JSONObject jsonObject,
                                            String gateway,
                                            Instant timestamp,
                                            double fee) {
        JSONObject response = new JSONObject();
        response.put(MSG_CORRELATION_ID, jsonObject.get(MSG_CORRELATION_ID));
        response.put(MSG_GATEWAY_FIELD_REDIS, gateway);
        response.put(MSG_AMOUNT_FIELD_REDIS, jsonObject.get(MSG_AMOUNT_FIELD_REDIS));
        response.put(MSG_TIMESTAMP_FIELD_REDIS, fee);

        return response;
    }

}
