package dto;

import com.google.gson.annotations.Expose;

import java.time.Instant;

public record GatewayHealth(
        @Expose
        boolean failing,
        @Expose
        long minResponseTime,
        Instant lastChecked
) {
}
