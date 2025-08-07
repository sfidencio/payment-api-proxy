package config;

import io.vertx.core.Vertx;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisOptions;

import static config.Constants.*;

public class RedisClientProvider {

    private static Redis client;


    public static Redis getInstance(Vertx vertx) {
        if (client == null) {
            RedisOptions options = new RedisOptions()
                    .setConnectionString("redis://" +
                            Environment.getEnv(REDIS_HOST) + ":" +
                            Environment.getEnv(REDIS_PORT))
                    .setMaxPoolSize(
                            Integer.parseInt(Environment.getEnv(REDIS_MAX_TOTAL)));
            client = Redis.createClient(vertx, options);
        }
        return client;
    }

    private RedisClientProvider() {
        throw new IllegalStateException(MSG_INSTANCE);
    }
}
