package repository.impl;

import config.RedisClientProvider;
import dto.GatewayHealth;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.redis.client.Command;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.Request;
import repository.IHealthCheckRepository;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static config.Constants.*;

public class HealthCheckRepositoryImpl implements IHealthCheckRepository {

    private final Redis redis;

    public HealthCheckRepositoryImpl(Vertx vertx) {
        this.redis = RedisClientProvider.getInstance(vertx);
    }

    @Override
    public Future<Void> saveHealth(String key,
                                   GatewayHealth health) {
        Promise<Void> promisse = Promise.promise();

        this.redis.send(Request.cmd(Command.HSET)
                        .arg(key)
                        .arg(MSG_GATEWAY_HEALTH_FAILING).arg(String.valueOf(health.failing()))
                        .arg(MSG_GATEWAY_HEALTH_MINRESPONSE_TIME).arg(String.valueOf(health.minResponseTime()))
                        .arg(MSG_GATEWAY_LAST_HEALTH_CHECK).arg(String.valueOf(health.lastChecked().toEpochMilli())),
                res -> {
                    if (res.succeeded()) {
                        promisse.complete();
                    } else {
                        promisse.fail(res.cause());
                    }
                });
        return promisse.future();
    }

    @Override
    public Future<GatewayHealth> getHealth(String key) {
        Promise<GatewayHealth> promise = Promise.promise();

        try {
            redis.send(Request.cmd(Command.HGETALL).arg(key))
                    .onSuccess(response -> {
                        try {
                            if (response != null && response.size() > 0) {

                                Map<String, String> healthData = response.getKeys().stream()
                                        .collect(HashMap::new,
                                                (map, key1) -> map.put(key1, response.get(key1).toString()),
                                                HashMap::putAll);

                                // Validate required fields exist
                                if (!healthData.containsKey(MSG_GATEWAY_HEALTH_FAILING) ||
                                        !healthData.containsKey(MSG_GATEWAY_HEALTH_MINRESPONSE_TIME) ||
                                        !healthData.containsKey(MSG_GATEWAY_LAST_HEALTH_CHECK)) {
                                    promise.complete(null);
                                    return;
                                }

                                boolean failing = Boolean.parseBoolean(healthData.get(MSG_GATEWAY_HEALTH_FAILING));
                                long minResponseTime = Long.parseLong(healthData.get(MSG_GATEWAY_HEALTH_MINRESPONSE_TIME));
                                Instant lastChecked = Instant.ofEpochMilli(Long.parseLong(healthData.get(MSG_GATEWAY_LAST_HEALTH_CHECK)));

                                promise.complete(new GatewayHealth(failing, minResponseTime, lastChecked));
                            } else {
                                promise.complete(null);
                            }
                        } catch (Exception e) {
                            System.err.println("Error parsing Redis response for key " + key + ": " + e.getMessage());
                            e.printStackTrace();
                            promise.fail(e);
                        }
                    })
                    .onFailure(ex -> {
                        System.err.println("Redis HGETALL failed for key " + key + ": " + ex.getMessage());
                        ex.printStackTrace();
                        promise.fail(ex);
                    });
        } catch (Exception e) {
            System.err.println("Error in getHealth for key " + key + ": " + e.getMessage());
            e.printStackTrace();
            promise.fail(e);
        }

        return promise.future();
    }
}
