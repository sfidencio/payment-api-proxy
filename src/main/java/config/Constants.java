package config;

public class Constants {

    private Constants() {
        throw new IllegalStateException(MSG_INSTANCE);
    }

    public static final String MSG_INSTANCE = "This class cannot be instantiated";
    public static final String CORRELATION_ID = "correlationId";
    public static final String AMOUNT = "amount";
    public static final String REQUESTED_AT = "requestedAt";
    public static final String MSG_RESPONSE_PAYMENT = "message";

    public static final String MSG_PROCESS_HEALTH_KEY = "payment-processor";
    public static final String MSG_PROCESS_HEALTH_FALLBACK_KEY = "payment-processor-fallback";

    public static final String MSG_GATEWAY_HEALTH_MINRESPONSE_TIME = "minResponseTime";
    public static final String MSG_GATEWAY_HEALTH_FAILING = "failing";

    public static final String PROCESSOR_DEFAULT = "PROCESSOR_DEFAULT";
    public static final String PROCESSOR_FALLBACK = "PROCESSOR_FALLBACK";


    public static final String PROCESSOR_GATEWAY_SELECTED = "selectedGateway";
    public static final String PROCESSOR_GATEWAY_SELECTED_FALLBACK = "fallback";
    public static final String PROCESSOR_GATEWAY_SELECTED_DEFAULT = "default";

    public static final String ENABLE_LOGGING = "ENABLE_LOGGING";

    public static final String PROCESSOR_POST_PAYMENT_URI = "/payments";
    public static final String PROCESSOR_GET_PAYMENT_URI = "/payments-summary";
    public static final String PROCESSOR_URI_HEALTH = "/service-health";
    public static final String APP_PORT = "APP_PORT";


    public static final String HTTP_CLIENT_CONNECT_TIMEOUT = "HTTP_CLIENT_CONNECT_TIMEOUT";

    public static final String HTTP_CLIENT_POOL_SIZE = "HTTP_CLIENT_POOL_SIZE";

    public static final String PROCESSOR_CONSUMER_INSTANCES = "PROCESSOR_CONSUMER_INSTANCES";


    public static final String POSTGRES_HOST = "POSTGRES_HOST";
    public static final String POSTGRES_PORT = "POSTGRES_PORT";
    public static final String POSTGRES_DB = "POSTGRES_DB";
    public static final String POSTGRES_USER = "POSTGRES_USER";
    public static final String POSTGRES_PASSWORD = "POSTGRES_PASSWORD";

    public static final String MAX_RETRIES_REENQUEUE = "MAX_RETRIES_REENQUEUE";
    public static final String POSTGRES_MAX_POOL_SIZE = "POSTGRES_MAX_POOL_SIZE";


}

