package config;

import java.net.http.HttpClient;
import java.time.Duration;

public class HttpClientBuild {

    private HttpClientBuild() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Returns a singleton instance of HttpClient.
     *
     * @return HttpClient instance
     */
    public static final HttpClient getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Garanteeing that the HttpClient instance is created
     * only once and is thread-safe.
     */
    private static class Holder {
        private static final HttpClient INSTANCE = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(1))
                .build();
    }
}
