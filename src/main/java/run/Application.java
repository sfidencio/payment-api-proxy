package run;

import com.sun.net.httpserver.HttpServer;
import config.DynamicThreadPool;
import config.Environment;
import config.HttpClientBuild;
import config.RedisClientBuild;
import handler.PaymentHandler;
import handler.PaymentSummaryHandler;
import service.HealthCheckService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.logging.Logger;

import static config.Constants.*;


public class Application {

    private static final Logger logger = Logger.getLogger(Application.class.getName());


    public static void main(String[] args) throws IOException {
        Instant start = Instant.now();

        var msgSensorRedis = RedisClientBuild.getInstance().ping();
        Environment
                .processLogging(logger,
                        "Redis connection status: ".concat(msgSensorRedis)
                );

        var http = HttpClientBuild.getInstance();
        Environment
                .processLogging(logger,
                        "HTTP Client connection status: ".concat(http != null ? "Success" : "Failed")
                );

        HealthCheckService.start();
        startServer();


        Instant end = Instant.now();

        long elapsedTime = end.toEpochMilli() - start.toEpochMilli();
        var msg = "Server started on port ".concat(Environment.getEnv(APP_PORT)).concat(" Elapsed time ").concat(String.valueOf(elapsedTime)).concat(" ms");

        Environment
                .processLogging(logger, msg);
    }

    private static void startServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(Integer.parseInt(Environment.getEnv(APP_PORT))), 0);
        server.createContext(PROCESSOR_GET_PAYMENT_URI, new PaymentSummaryHandler());
        server.createContext(PROCESSOR_POST_PAYMENT_URI, new PaymentHandler());
        server.setExecutor(DynamicThreadPool.createExecutor());
        server.start();
    }
}
