package config;

public class Constants {

    private Constants() {
        throw new IllegalStateException(MSG_INSTANCE);
    }

    public static final String MSG_INSTANCE = "DynamicThreadPool cannot be instantiated";
    public static final String MSG_GATEWAY_PROCESSED_SUCCESSFULY = "Default gateway processed successfully.";
    public static final String MSG_CORRELATION_ID = "correlationId";
    public static final String MSG_PROCESS_POST_REQUEST = "Processing POST request";
    public static final String MSG_PROCESS_HEALTH_KEY = "payment-processor";
    public static final String MSG_PROCESS_HEALTH_FALLBACK_KEY = "payment-processor-fallback";
    public static final String MSG_PROCESS_HEALTH_URL = "http://payment-processor/health";
    public static final String MSG_PROCESS_HEALTH_FALLBACK_URL = "http://payment-processor-fallback/health";


    public static final String MSG_GATEWAY_FIELD_REDIS = "gateway";
    public static final String MSG_AMOUNT_FIELD_REDIS = "amount";
    public static final String MSG_TIMESTAMP_FIELD_REDIS = "timestamp";
    public static final String MSG_FEE_REDIS = "fee";

    public static final String DEFAULT_GATEWAY_URL = "https://payment-processor/payments";
    public static final String FALLBACK_GATEWAY_URL = "https://payment-processor-fallback/payments";

    public static final String MSG_DEFAULT_GATEWAY_FAILED = "Default gateway failed: ";

    public static final String MSG_PROCESSING_PAYMENT_PAYLOAD = "Processing payment with payload: ";
}
