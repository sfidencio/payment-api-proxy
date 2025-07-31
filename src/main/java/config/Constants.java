package config;

public class Constants {

    private Constants() {
        throw new IllegalStateException(MSG_INSTANCE);
    }

    public static final String MSG_INSTANCE = "DynamicThreadPool cannot be instantiated";
    public static final String MSG_PAYMENT_SUCCESS = "Payment processed successfully.";
    public static final String MSG_PAYMENT_FAILED = "Payment processing failed.";
    public static final String MSG_INTERNAL_SERVER_ERROR = "Internal server error.";
    public static final String MSG_UNEXPECTED_ERROR = "Unexpected error occurred while processing payment: ";

    public static final String MSG_PAYMENT_FAILED_DEFAULT_AND_FALLBACK_GATEWAY = "Payment failed with both primary and fallback gateways.";

    public static final String CORRELATION_ID = "correlationId";
    public static final String AMOUNT = "amount";
    public static final String REQUESTED_AT = "requestedAt";
    public static final String MSG_RESPONSE_PAYMENT = "message";

    public static final String MSG_INVALID_QUERY_PARAMS = "Invalid query parameters. Please provide 'from' and 'to' parameters in the format: ?from=YYYY-MM-DD&to=YYYY-MM-DD";
    public static final String MSG_PROCESS_POST_REQUEST = "Processing POST request";
    public static final String MSG_PROCESS_GET_REQUEST = "Processing GET request";
    public static final String MSG_PROCESS_HEALTH_KEY = "payment-processor";
    public static final String MSG_PROCESS_HEALTH_FALLBACK_KEY = "payment-processor-fallback";

    public static final String MSG_GATEWAY_HEALTH_MINRESPONSE_TIME = "minResponseTime";
    public static final String MSG_GATEWAY_HEALTH_FAILING = "failing";

    public static final String PROCESSOR_DEFAULT = "PROCESSOR_DEFAULT";
    public static final String PROCESSOR_FALLBACK = "PROCESSOR_FALLBACK";

    public static final String REDIS_HOST = "REDIS_HOST";
    public static final String REDIS_PORT = "REDIS_PORT";


    public static final String PROCESSOR_GATEWAY_SELECTED = "selectedGateway";
    public static final String PROCESSOR_GATEWAY_SELECTED_FALLBACK = "fallback";
    public static final String PROCESSOR_GATEWAY_SELECTED_DEFAULT = "default";

    public static final String ENABLE_LOGGING = "ENABLE_LOGGING";

    public static final String PROCESSOR_POST_PAYMENT_URI = "/payments";
    public static final String PROCESSOR_GET_PAYMENT_URI = "/payments-summary";
    public static final String PROCESSOR_URI_HEALTH = "/service-health";
    public static final String APP_PORT = "APP_PORT";

    //MSG_NO_RECORDS_FOUND
    public static final String MSG_NO_RECORDS_FOUND = "No records found for the given date range.";
}

