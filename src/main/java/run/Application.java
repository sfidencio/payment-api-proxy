package run;

import config.Environment;
import config.PaymentDependencies;
import config.RedisClientProvider;
import config.WebClientProvider;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import service.HealthCheckService;
import service.IPaymentService;

import java.io.IOException;
import java.time.Instant;
import java.util.logging.Logger;

import static config.Constants.*;


public class Application {

    private static final Logger logger = Logger.getLogger(Application.class.getName());


    public static void main(String[] args) throws IOException {


        var vertx = Vertx.vertx();


        setupGlobalExceptionHandler(vertx);

        PaymentDependencies.initialize(vertx);

        configureLogging(vertx);

        Instant start = Instant.now();

        HealthCheckService.start(vertx);

        startServer(vertx);

        Instant end = Instant.now();

        long elapsedTime = end.toEpochMilli() - start.toEpochMilli();

        var msg = "Server started on port ".concat(Environment.getEnv(APP_PORT)).concat(" Elapsed time ").concat(String.valueOf(elapsedTime)).concat(" ms");

        Environment.processLogging(
                logger,
                msg
        );
    }

    private static void startServer(Vertx vertx) {
        warmup(vertx);

        Router router = Router.router(vertx);


        IPaymentService paymentService = PaymentDependencies.getInstance().getPaymentService();

        router.post(PROCESSOR_POST_PAYMENT_URI
        ).handler(
                new handler.PaymentHandler(
                        paymentService
                )
        );

        router.get(PROCESSOR_GET_PAYMENT_URI)
                .handler(
                        new handler.PaymentSummaryHandler(
                                paymentService
                        )
                );

        // Configurações do servidor HTTP (entrada de requests)
        HttpServerOptions serverOptions = new HttpServerOptions()
                .setTcpQuickAck(true)
                .setTcpNoDelay(true)
                .setReusePort(true)
                .setAcceptBacklog(1024);

        vertx.createHttpServer(serverOptions)
                .requestHandler(router)
                .listen(
                        Integer.parseInt(Environment.getEnv(APP_PORT))
                );
    }

    public static void configureLogging(Vertx vertx) {
        Environment.vertx = vertx;
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT [%3$s] %5$s%n");
    }

    private static void warmup(Vertx vertx) {
        // Warm up the Redis client by establishing a connection
        RedisClientProvider.getInstance(
                vertx
        ).connect().onSuccess(
                connection -> Environment.processLogging(
                        logger,
                        "Redis Client connection status: Success"
                )
        ).onFailure(
                throwable -> Environment.processLogging(
                        logger,
                        "Redis Client connection status: Failed - ".concat(throwable.getMessage())
                )
        );

        // Warm up the HTTP client by making a health check request
        var webClient = WebClientProvider.getInstance(vertx);
        var healthCheckUrl = Environment.getEnv(PROCESSOR_DEFAULT)
                .concat(PROCESSOR_POST_PAYMENT_URI)
                .concat(PROCESSOR_URI_HEALTH);
        webClient.getAbs(healthCheckUrl)
                .send()
                .onSuccess(response -> Environment.processLogging(
                        logger,
                        "WebClient connection status: Success - Health check URL: ".concat(healthCheckUrl)
                ))
                .onFailure(throwable -> Environment.processLogging(
                        logger,
                        "WebClient connection status: Failed - ".concat(throwable.getMessage())
                ));

    }

    private static void setupGlobalExceptionHandler(Vertx vertx) {
        vertx.exceptionHandler(throwable -> {
            try {
                System.err.println("=== UNHANDLED EXCEPTION CAPTURED ===");
                System.err.println("Timestamp: " + Instant.now());
                System.err.println("Thread: " + Thread.currentThread().getName());
                System.err.println("Exception Type: " + throwable.getClass().getSimpleName());
                System.err.println("Message: " + (throwable.getMessage() != null ? throwable.getMessage() : "No message"));

                if (throwable.getCause() != null) {
                    System.err.println("Root Cause: " + throwable.getCause().getClass().getSimpleName());
                    System.err.println("Cause Message: " + throwable.getCause().getMessage());
                }

                // Log to application logger as well
                Environment.processLogging(logger, "Unhandled exception: " + throwable.getClass().getSimpleName() + " - " + throwable.getMessage());

                // Print stack trace
                throwable.printStackTrace();
                System.err.println("=====================================");

            } catch (Exception e) {
                // Fallback in case the exception handler itself fails
                System.err.println("Error in exception handler: " + e.getMessage());
                throwable.printStackTrace();
            }
        });
    }
}
