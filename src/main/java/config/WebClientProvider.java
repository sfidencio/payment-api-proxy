package config;

import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

import static config.Constants.*;

public class WebClientProvider {
    private static volatile WebClient client;

    public static WebClient getInstance(Vertx vertx) {
        if (client == null) {
            synchronized (WebClientProvider.class) {
                if (client == null) {
                    WebClientOptions options = new WebClientOptions()
                            .setConnectTimeout(Integer.parseInt(Environment.getEnv(HTTP_CLIENT_CONNECT_TIMEOUT)))
                            .setIdleTimeout(60)
                            .setMaxPoolSize(Integer.parseInt(Environment.getEnv(HTTP_CLIENT_POOL_SIZE)))
                            .setKeepAlive(true)
                            .setTcpKeepAlive(true);
                            //.setUserAgent("Vert.x-WebClient");

                    client = WebClient.create(vertx, options);
                }
            }
        }
        return client;
    }

    private WebClientProvider() {
        throw new IllegalStateException(MSG_INSTANCE);
    }
}