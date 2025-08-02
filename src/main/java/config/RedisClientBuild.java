package config;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import static config.Constants.*;

public class RedisClientBuild {

    private static final JedisPool pool;


    static {
        var config = new JedisPoolConfig();
        config.setMaxTotal(10);
        config.setMaxIdle(2);
        config.setMinIdle(1);
        config.setTestOnBorrow(false);
        config.setTestOnReturn(false);
        config.setTestWhileIdle(false);

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
