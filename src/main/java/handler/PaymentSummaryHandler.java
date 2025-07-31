package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import config.Environment;
import config.HttpResponseUtililty;
import service.PaymentService;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static config.Constants.*;

public class PaymentSummaryHandler implements HttpHandler {
    private static final Logger logger = Logger.getLogger(PaymentSummaryHandler.class.getName());

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (HttpVerb.isValid(exchange.getRequestMethod()) &&
                exchange.getRequestMethod().equals(HttpVerb.GET.name())) {
            this.processGet(exchange);
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }

    private void processGet(HttpExchange exchange) throws IOException {
        Environment
                .processLogging(logger, MSG_PROCESS_GET_REQUEST.concat(" - ").concat(exchange.getRequestURI().toString()));


        Map<String, String> params = exchange.getRequestURI().getQuery() == null ? Map.of() : queryToMap(exchange.getRequestURI().getQuery());


        var fromStr = params.get("from");
        var toStr = params.get("to");


        try {
            Instant from = fromStr == null ? null : Instant.parse(fromStr);
            Instant to = toStr == null ? null : Instant.parse(toStr);

            var paymentSummaryResponse = PaymentService.getPaymentSummary(from, to);

            if (paymentSummaryResponse == null || paymentSummaryResponse.paymentSummaryByGateway().isEmpty()) {
                Environment.processLogging(logger, MSG_NO_RECORDS_FOUND);
                HttpResponseUtililty.sendJson(exchange, 404, MSG_NO_RECORDS_FOUND);
                return;
            }
            HttpResponseUtililty.sendJson(exchange, 200, paymentSummaryResponse.toJson());
        } catch (Exception e) {
            Environment
                    .processLogging(logger, MSG_UNEXPECTED_ERROR.concat(" - ").concat(e.getMessage()));
            HttpResponseUtililty.sendJson(exchange, 500, MSG_INTERNAL_SERVER_ERROR);
        }
    }


    private Map<String, String> queryToMap(String query) {
        return Arrays.stream(query.split("&"))
                .map(s -> s.split("="))
                .filter(arr -> arr.length == 2)
                .collect(Collectors.toMap(a -> a[0], a -> a[1]));
    }
}
