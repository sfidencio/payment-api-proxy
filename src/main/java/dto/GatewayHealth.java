package dto;

import java.time.Instant;

public record GatewayHealth(
        boolean healthy,
        long minResponseTimeMs,
        Instant lastChecked
) {
}
