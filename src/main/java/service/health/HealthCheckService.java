package service.health;

import config.Environment;
import config.PaymentDependencies;
import config.WebClientProvider;
import dto.GatewayHealth;
import dto.GatewaySelected;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import repository.IHealthCheckRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static config.Constants.*;

public class HealthCheckService {

    private static final Logger logger = Logger.getLogger(HealthCheckService.class.getName());
    private static IHealthCheckRepository repository;
    private static final Map<String, GatewayHealth> cache = new ConcurrentHashMap<>(2);

    private HealthCheckService() {
        throw new IllegalStateException(MSG_INSTANCE);
    }

    public static void start(Vertx vertx) {
        try {
            var webClient = WebClientProvider.getInstance(vertx);
            repository = PaymentDependencies.getDatabaseHealthCheckRepository();

            vertx.setPeriodic(10000, id -> {
                try {
                    check(webClient, MSG_PROCESS_HEALTH_KEY, Environment.getEnv(PROCESSOR_DEFAULT).concat(PROCESSOR_POST_PAYMENT_URI).concat(PROCESSOR_URI_HEALTH));
                    check(webClient, MSG_PROCESS_HEALTH_FALLBACK_KEY, Environment.getEnv(PROCESSOR_FALLBACK).concat(PROCESSOR_POST_PAYMENT_URI).concat(PROCESSOR_URI_HEALTH));
                } catch (Exception e) {
                    Environment.processLogging(logger, "Error in periodic health check: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            Environment.processLogging(logger, "Failed to start HealthCheckService: " + e.getMessage());
        }
    }

    private static void check(WebClient webClient, String key, String url) {
        sholdSkipHealthCheck(key)
                .onSuccess(skip -> {
                    if (skip) return;

                    webClient.getAbs(url)
                            .send()
                            .onSuccess(res -> {
                                try {
                                    var json = res.bodyAsJsonObject();
                                    var gatewayHealth = new GatewayHealth(
                                            json.getBoolean(MSG_GATEWAY_HEALTH_FAILING),
                                            json.getInteger(MSG_GATEWAY_HEALTH_MINRESPONSE_TIME),
                                            Instant.now()
                                    );
                                    cache.put(key, gatewayHealth);

                                    repository.saveHealth(key, gatewayHealth)
                                            .onSuccess(v -> Environment.processLogging(logger, "Health saved successfully: " + key))
                                            .onFailure(ex -> Environment.processLogging(logger, "Failed to save health: ".concat(" - ").concat(ex.getMessage())));

                                    Environment.processLogging(logger, "Gateway health check status -> [OK] - [GATEWAY] -> ".concat(key).concat(" [DATA] -> ").concat(gatewayHealth.toString()));
                                } catch (Exception e) {
                                    Environment.processLogging(logger, "Error processing health check response: " + e.getMessage());
                                    handleHealthCheckFailure(key, e);
                                }
                            })
                            .onFailure(ex -> {
                                Environment.processLogging(logger, "Health check request failed for " + key + ": " + ex.getMessage());
                                handleHealthCheckFailure(key, ex);
                            });
                })
                .onFailure(ex -> {
                    Environment.processLogging(logger, "Failed to check skip condition for " + key + ": " + ex.getMessage());
                });
    }

    private static void handleHealthCheckFailure(String key, Throwable ex) {
        try {
            GatewayHealth gatewayHealth = new GatewayHealth(true, 100000000, Instant.now());
            cache.put(key, gatewayHealth);

            repository.saveHealth(key, gatewayHealth)
                    .onFailure(saveEx -> Environment.processLogging(logger, "Failed to save failed health status: " + saveEx.getMessage()));

            Environment.processLogging(logger, "Gateway health check status -> [FAILED] - [GATEWAY] -> " + key + " [DATA] -> " + gatewayHealth + " - Exception: " + ex.getMessage());
        } catch (Exception e) {
            Environment.processLogging(logger, "Critical error in failure handler: " + e.getMessage());
        }
    }

    private static Future<GatewayHealth> get(String key) {
        Promise<GatewayHealth> promise = Promise.promise();

        try {
            repository.getHealth(key)
                    .onComplete(ar -> {
                        try {
                            if (ar.succeeded()) {
                                var gatewayHealth = ar.result();
                                if (gatewayHealth != null) {
                                    cache.put(key, gatewayHealth);
                                }

                                if (gatewayHealth == null || gatewayHealth.failing()) {
                                    promise.complete(null);
                                } else {
                                    promise.complete(gatewayHealth);
                                }
                            } else {
                                Environment.processLogging(logger, "Failed to get health for " + key + ": " + ar.cause().getMessage());
                                promise.fail(ar.cause());
                            }
                        } catch (Exception e) {
                            Environment.processLogging(logger, "Error in get health completion: " + e.getMessage());
                            promise.fail(e);
                        }
                    });
        } catch (Exception e) {
            Environment.processLogging(logger, "Error initiating get health: " + e.getMessage());
            promise.fail(e);
        }

        return promise.future();
    }

    public static Future<Map<String, GatewaySelected>>
    getBestGateway() {
        Promise<Map<String, GatewaySelected>> promise = Promise.promise();

        get(MSG_PROCESS_HEALTH_KEY)
                .onSuccess(defaultHealth -> {
                    if (defaultHealth != null) {
                        Environment.processLogging(logger, "Always selected the MAIN gateway.");
                        promise.complete(Map.of(PROCESSOR_GATEWAY_SELECTED, new GatewaySelected(PROCESSOR_GATEWAY_SELECTED_DEFAULT, Environment.getEnv(PROCESSOR_DEFAULT).concat(PROCESSOR_POST_PAYMENT_URI))));
                    } else {
                        get(MSG_PROCESS_HEALTH_FALLBACK_KEY)
                                .onSuccess(fallbackHealth -> {
                                    if (fallbackHealth != null) {
                                        Environment.processLogging(logger, "Main unavailable, using FALLBACK.");
                                        promise.complete(Map.of(PROCESSOR_GATEWAY_SELECTED, new GatewaySelected(PROCESSOR_GATEWAY_SELECTED_FALLBACK, Environment.getEnv(PROCESSOR_FALLBACK).concat(PROCESSOR_POST_PAYMENT_URI))));
                                    } else {
                                        Environment.processLogging(logger, "No gateway available.");
                                        promise.complete(null);
                                    }
                                })
                                .onFailure(ex -> {
                                    Environment.processLogging(logger, "Failed to get fallback health: " + ex.getMessage());
                                    promise.fail(ex);
                                });
                    }
                })
                .onFailure(ex -> {
                    Environment.processLogging(logger, "Failed to get default health: " + ex.getMessage());
                    promise.fail(ex);
                });

        return promise.future();
    }

    public static Future<Boolean> sholdSkipHealthCheck(String key) {
        Promise<Boolean> promise = Promise.promise();

        try {
            repository.getHealth(key)
                    .onComplete(ar -> {
                        try {
                            if (ar.succeeded() && ar.result() != null) {
                                long secondsSinceLastCheck = Duration.between(ar.result().lastChecked(), Instant.now()).getSeconds();
                                if (secondsSinceLastCheck < 6) {
                                    Environment.processLogging(logger, "Healthcheck skipped: last check was less than 6 seconds ago -> [GATEWAY] -> " + key);
                                    promise.tryComplete(true);
                                    return;
                                }
                            }
                            promise.tryComplete(false);
                        } catch (Exception e) {
                            Environment.processLogging(logger, "Error in skip check completion: " + e.getMessage());
                            promise.tryComplete(false);
                        }
                    })
                    .onFailure(ex -> {
                        Environment.processLogging(logger, "Database error in skip check for " + key + ": " + ex.getMessage());
                        promise.tryComplete(false);
                    });
        } catch (Exception e) {
            Environment.processLogging(logger, "Error in sholdSkipHealthCheck: " + e.getMessage());
            promise.tryComplete(false);
        }

        return promise.future();
    }
}