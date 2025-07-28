package handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import service.PaymentService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.logging.Logger;

import static config.Constants.MSG_PROCESS_POST_REQUEST;

public class PaymentHandler implements HttpHandler {
   static final Logger logger = Logger.getLogger(PaymentHandler.class.getName());

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (VerbHttp.isValid(exchange.getRequestMethod()) && exchange.getRequestMethod().equals(VerbHttp.POST.name()))
            this.processPost(exchange);
    }

    private void processPost(HttpExchange exchange) throws IOException {

        logger.info(MSG_PROCESS_POST_REQUEST);
        InputStream is = exchange.getRequestBody();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        String response = PaymentService.process(body);

        byte[] responseBytes = Objects.requireNonNull(response).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, responseBytes.length);

        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();

    }
}
