package config;

import redis.clients.jedis.Jedis;

import static config.Constants.MSG_INSTANCE;

public class RedisClientHelper {
    private static final Jedis INSTANCE = new Jedis("localhost", 6379);

    private RedisClientHelper() {
        throw new IllegalStateException(MSG_INSTANCE);
    }

    public static Jedis getInstance() {
        return INSTANCE;
    }
}
