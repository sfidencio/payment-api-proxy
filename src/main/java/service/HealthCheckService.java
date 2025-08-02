package service;

import config.Environment;
import config.HttpClientBuild;
import dto.GatewayHealth;
import dto.GatewaySelected;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static config.Constants.*;
import static config.DynamicThreadPool.createScheduler;

public class HealthCheckService {

    private static final Logger logger = Logger.getLogger(HealthCheckService.class.getName());

    private HealthCheckService() {
        throw new IllegalStateException(MSG_INSTANCE);
    }

    private static final Map<String, GatewayHealth> cache = new ConcurrentHashMap<>(2);

    public static void start() {
        ScheduledExecutorService scheduler = createScheduler();
        scheduler.scheduleAtFixedRate(() -> check(MSG_PROCESS_HEALTH_KEY, Environment.getEnv(PROCESSOR_DEFAULT).concat(PROCESSOR_POST_PAYMENT_URI).concat(PROCESSOR_URI_HEALTH)), 3, 7, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(() -> check(MSG_PROCESS_HEALTH_FALLBACK_KEY, Environment.getEnv(PROCESSOR_FALLBACK).concat(PROCESSOR_POST_PAYMENT_URI).concat(PROCESSOR_URI_HEALTH)), 3, 7, TimeUnit.SECONDS);
    }

    private static void check(String key, String url) {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofMillis(200)).build();
        try {
            HttpResponse<String> responseBody = HttpClientBuild.getInstance().send(request, HttpResponse.BodyHandlers.ofString());

            var response = new JSONObject(responseBody.body());

            var gatewayHealth = new GatewayHealth(
                    response.getBoolean(MSG_GATEWAY_HEALTH_FAILING),
                    response.getInt(MSG_GATEWAY_HEALTH_MINRESPONSE_TIME),
                    Instant.now()
            );
            cache.put(key, gatewayHealth);
            Environment.processLogging(
                    logger,
                    "Gateway health check status -> [OK] - [GATEWAY] -> ".concat(key).concat(" [DATA] -> ").concat(gatewayHealth.toString())
            );
        } catch (Exception ex) {
            var gatewayHealth = new GatewayHealth(
                    true,
                    100000000,
                    Instant.now()
            );
            cache.put(key, gatewayHealth);

            Environment.processLogging(
                    logger,
                    "Gateway health check status -> [FAILED] - [GATEWAY] -> ".concat(key).concat(" [DATA] -> ").concat(gatewayHealth.toString().concat(" - Exception: ").concat(ex.getMessage()))
            );
        }
    }

    private static GatewayHealth get(String key) {
        var gatewayHealth = cache.get(key);
        if (Objects.isNull(gatewayHealth) || gatewayHealth.lastChecked().isBefore(Instant.now().minusSeconds(6)))
            return null;
        return gatewayHealth;
    }

    public static Map<String, GatewaySelected> getBestGateway() {
        var defaultHealth = get(MSG_PROCESS_HEALTH_KEY);
        var fallbackHealth = get(MSG_PROCESS_HEALTH_FALLBACK_KEY);

        Environment.processLogging(
                logger,
                "Health Check - Default: ".concat(defaultHealth != null ? defaultHealth.toString() : "null")
                        .concat(" | Fallback: ").concat(fallbackHealth != null ? fallbackHealth.toString() : "null")
        );


        if (defaultHealth == null && fallbackHealth == null) {
            Environment
                    .processLogging(logger, "No gateway is available for processing payments.");
            return null;
        }

        if (defaultHealth != null && (fallbackHealth == null || defaultHealth.minResponseTime() <= fallbackHealth.minResponseTime())) {
            Environment
                    .processLogging(logger, "Selected DEFAULT gateway with minResponseTime: ".concat(String.valueOf(defaultHealth.minResponseTime())));
            return Map.of(PROCESSOR_GATEWAY_SELECTED, new GatewaySelected(PROCESSOR_GATEWAY_SELECTED_DEFAULT, Environment.getEnv(PROCESSOR_DEFAULT).concat(PROCESSOR_POST_PAYMENT_URI)));
        } else {
            Environment
                    .processLogging(logger, "Selected FALLBACK gateway with minResponseTime: ".concat(String.valueOf(fallbackHealth.minResponseTime())));
            return Map.of(PROCESSOR_GATEWAY_SELECTED, new GatewaySelected(PROCESSOR_GATEWAY_SELECTED_FALLBACK, Environment.getEnv(PROCESSOR_FALLBACK).concat(PROCESSOR_POST_PAYMENT_URI)));
        }
    }


}
