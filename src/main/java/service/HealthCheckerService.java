package service;

import config.HttpClientHelper;
import dto.GatewayHealth;
import org.json.JSONObject;

import java.io.IOException;
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

public class HealthCheckerService {

    private static final Logger logger = Logger.getLogger(HealthCheckerService.class.getName());

    private HealthCheckerService() {
        throw new IllegalStateException(MSG_INSTANCE);
    }

    private static final Map<String, GatewayHealth> cache = new ConcurrentHashMap<>();

    public static void start() {
        ScheduledExecutorService scheduler = createScheduler();
        scheduler.scheduleAtFixedRate(() -> check(MSG_PROCESS_HEALTH_KEY, MSG_PROCESS_HEALTH_URL), 0, 5, TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(() -> check(MSG_PROCESS_HEALTH_FALLBACK_KEY, MSG_PROCESS_HEALTH_FALLBACK_URL), 0, 5, TimeUnit.SECONDS);
    }

    private static void check(String key, String url) {
        var msg = "Checking health for key: ".concat(key).concat(" with URL: ").concat(url);
        logger.info(msg);
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(Duration.ofMillis(200)).build();
        try {
            HttpResponse<String> response = HttpClientHelper.getInstance().send(request, HttpResponse.BodyHandlers.ofString());

            var gatewayHealth = new GatewayHealth(
                    response.statusCode() == 200,
                    new JSONObject(response.body()).getLong("minResponseTimeMs"),
                    Instant.now()
            );

            cache.put(key, gatewayHealth);

        } catch (IOException | InterruptedException _) {
            var gatewayHealth = new GatewayHealth(
                    false,
                    -1,
                    Instant.now()
            );
            cache.put(key, gatewayHealth);
            Thread.currentThread().interrupt();
        }
    }

    public static GatewayHealth get(String key) {
        var gatewayHealth = cache.get(key);
        if (Objects.isNull(gatewayHealth) || gatewayHealth.lastChecked().isBefore(Instant.now().minusSeconds(6)))
            return null;
        return gatewayHealth;
    }
}
