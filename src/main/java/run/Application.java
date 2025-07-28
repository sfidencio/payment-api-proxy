package run;

import com.sun.net.httpserver.HttpServer;
import config.DynamicThreadPool;
import handler.PaymentHandler;
import service.HealthCheckerService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.logging.Logger;


public class Application {

    private static final Logger logger = Logger.getLogger(Application.class.getName());

    public static void main(String[] args) throws IOException {
        Instant start = Instant.now();

        HealthCheckerService.start();
        startServer();

        Instant end = Instant.now();

        long elapsedTime = end.toEpochMilli() - start.toEpochMilli();
        var msg = "Server started on port 8080. Elapsed time ".concat(String.valueOf(elapsedTime)).concat(" ms");
        logger.info(msg);
    }

    private static void startServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/payment", new PaymentHandler());
        server.setExecutor(DynamicThreadPool.createExecutor());
        server.start();
    }
}
