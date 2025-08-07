package dto;

import java.time.Instant;

public record GatewayHealth(
        boolean failing,
        long minResponseTime,
        Instant lastChecked
) {
}
