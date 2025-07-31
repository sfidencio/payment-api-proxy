package dto;

import java.math.BigDecimal;

public record PaymentSummary(
        int totalRequests,
        BigDecimal totalAmount
) {
}
