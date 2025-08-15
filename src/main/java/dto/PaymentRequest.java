package dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequest {
    private String correlationId;
    private BigDecimal amount;
}

