package repository.impl;

import config.RedisClientBuild;
import dto.PaymentSummary;
import dto.PaymentSummaryByGatewayResponse;
import repository.PaymentRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public class RedisPaymentRepository implements PaymentRepository {


    @Override
    public void save(String correlationId,
                     String gatewayType,
                     BigDecimal amount,
                     Instant createdAt) {
        try (var jedis = RedisClientBuild.getInstance()) {
            long amountInCents = amount.movePointRight(2).longValueExact();
            var value = String.format("%s:%s", correlationId, amountInCents);
            jedis.zadd("payments:".concat(gatewayType), createdAt.toEpochMilli(), value);
            jedis.hset("payments-query:index", correlationId, String.format(
                    "%s", correlationId
            ));
        }
    }

    @Override
    public boolean getByCorrelationId(String correlationId) {
        try (var jedis = RedisClientBuild.getInstance()) {
            var keyFound = jedis.hget("payments-query:index", correlationId);
            if (keyFound == null) {
                return false;
            }
            return keyFound.trim().equals(correlationId.trim());
        }
    }

    @Override
    public PaymentSummaryByGatewayResponse get(Instant from, Instant to) {
        try (var jedis = RedisClientBuild.getInstance()) {
            Set<String> keys = jedis.keys("payments:*");
            Map<String, PaymentSummary> summaryByGateway = new LinkedHashMap<>();

            long fromM = from == null ? 0L : from.toEpochMilli();
            long toM = to == null ? Long.MAX_VALUE : to.toEpochMilli();

            for (var key : keys) {
                String gateway = key.replace("payments:", "");
                List<String> entries = jedis.zrangeByScore(key, fromM, toM);

                BigDecimal total = entries.stream()
                        .map(entry -> entry.split(":")[1])
                        .map(BigDecimal::new)
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .movePointLeft(2);

                summaryByGateway.put(gateway, new PaymentSummary(
                        entries.size(),
                        total
                ));
            }

            return new PaymentSummaryByGatewayResponse(summaryByGateway);
        }
    }

}
