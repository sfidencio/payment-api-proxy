package dto;

public record PaymentMessage(long messageID
        , PaymentRequest paymentRequest) {
}
