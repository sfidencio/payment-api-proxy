package dto;

public record PaymentSummary(
        int totalRequests,
        double totalAmount
) {
}
