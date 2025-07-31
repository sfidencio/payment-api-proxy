package config;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import static config.Constants.*;

public class RedisClientBuild {

    private static final JedisPool pool;


    static {
        var config = new JedisPoolConfig();
        config.setMaxTotal(30);
        config.setMaxIdle(10);
        config.setMinIdle(2);
        config.setTestOnBorrow(true);

        var host = Environment.getEnv(REDIS_HOST);
        var port = Integer.parseInt(Environment.getEnv(REDIS_PORT));
        pool = new JedisPool(config, host, port);
    }

    public static Jedis getInstance() {
        return pool.getResource();
    }

    private RedisClientBuild() {
        throw new IllegalStateException(MSG_INSTANCE);
    }
}
