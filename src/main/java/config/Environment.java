package config;

import io.vertx.core.Vertx;

import java.util.logging.Logger;

import static config.Constants.ENABLE_LOGGING;
import static config.Constants.MSG_INSTANCE;


public class Environment {

    public static Vertx vertx;

    private Environment() {
        throw new IllegalStateException(MSG_INSTANCE);
    }

    public static String getEnv(String key) {
        return System.getenv(key);
    }


    public static void processLogging(Logger logger, String message) {
        boolean active = Boolean.parseBoolean(getEnv(ENABLE_LOGGING));
        if (active) {
            vertx.runOnContext(
                    v -> logger.info(message)
            );
        }
    }
}
