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
        var jedis = RedisClientBuild.getInstance();
        long amountInCents = amount.movePointRight(2).longValueExact();
        var value = String.format("%s:%s", correlationId, amountInCents);
        jedis.zadd("payments:".concat(gatewayType), createdAt.toEpochMilli(), value);
        jedis.hset("payments-query:index", correlationId, String.format(
                "%s", correlationId
        ));
    }

    @Override
    public boolean getByCorrelationId(String correlationId) {
        var jedis = RedisClientBuild.getInstance();
        var keyFound = jedis.hget("payments-query:index", correlationId);
        if (keyFound == null) {
            return false;
        }
        return keyFound.trim().equals(correlationId.trim());
    }

    @Override
    public PaymentSummaryByGatewayResponse get(Instant from, Instant to) {

        var jedis = RedisClientBuild.getInstance();

        Set<String> keys = jedis.keys("payments:*");

        BigDecimal total = BigDecimal.ZERO;

        int paymentCount = 0;

        long fromM = from == null ? 0L : from.toEpochMilli();
        long toM = to == null ? Long.MAX_VALUE : to.toEpochMilli();

        Map<String, PaymentSummary> summaryByGateway = new LinkedHashMap<>();

        for (var key : keys) {
            String gateway = key.replace("payments:", "");
            List<String> entries = jedis.zrangeByScore(key, fromM, toM);

            total = total.add(
                    entries.stream()
                            .map(entry -> entry.split(":")[1])
                            .map(BigDecimal::new)
                            .reduce(BigDecimal.ZERO, BigDecimal::add)
                            .movePointLeft(2)
            );
            paymentCount = entries.size();

            summaryByGateway.put(gateway, new PaymentSummary(
                    paymentCount,
                    total
            ));
            total = BigDecimal.ZERO;
        }

        return new PaymentSummaryByGatewayResponse(summaryByGateway);
    }

}
