package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import config.Environment;
import config.HttpResponseUtililty;
import dto.PaymentRequest;
import org.json.JSONObject;
import service.PaymentService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.logging.Logger;

import static config.Constants.*;

public class PaymentHandler implements HttpHandler {
    private static final Logger logger = Logger.getLogger(PaymentHandler.class.getName());

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (HttpVerb.isValid(exchange.getRequestMethod()) &&
                exchange.getRequestMethod().equals(HttpVerb.POST.name())) {
            this.processPost(exchange);
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }

    private void processPost(HttpExchange exchange) throws IOException {
        Environment
                .processLogging(logger, MSG_PROCESS_POST_REQUEST.concat(" - ").concat(exchange.getRequestURI().toString()));
        try (InputStream is = exchange.getRequestBody()) {
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            var requestJson = new JSONObject(body);
            var request = new PaymentRequest(
                    requestJson.getString(CORRELATION_ID),
                    requestJson.getBigDecimal(AMOUNT)
            );

            Environment
                    .processLogging(logger, MSG_PROCESS_POST_REQUEST.concat(" - ").concat(request.toString()));

            if (!PaymentService.processPayment(request)) {
                HttpResponseUtililty.sendJson(exchange, 502, MSG_PAYMENT_FAILED);
                return;
            }

            HttpResponseUtililty.sendJson(exchange, 200, Objects.requireNonNull(MSG_PAYMENT_SUCCESS));
        } catch (Exception e) {
            Environment
                    .processLogging(logger, MSG_UNEXPECTED_ERROR.concat(" - ").concat(e.getMessage()));
            HttpResponseUtililty.sendJson(exchange, 500, MSG_INTERNAL_SERVER_ERROR);
        }
    }
}
