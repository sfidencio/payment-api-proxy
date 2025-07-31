package dto;

public record PaymentProcessorResponse(
        String message,
        String gatewayType,
        int statusCode
) {
}
