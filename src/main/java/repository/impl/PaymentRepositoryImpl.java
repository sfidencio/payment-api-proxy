package repository.impl;

import config.RedisClientProvider;
import dto.PaymentSummary;
import dto.PaymentSummaryByGatewayResponse;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.impl.future.CompositeFutureImpl;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import repository.IPaymentRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static config.Constants.PROCESSOR_GATEWAY_SELECTED_DEFAULT;
import static config.Constants.PROCESSOR_GATEWAY_SELECTED_FALLBACK;


public class PaymentRepositoryImpl implements IPaymentRepository {


    private final RedisAPI redisAPI;

    public PaymentRepositoryImpl(Vertx vertx) {
        Redis redisClient = RedisClientProvider.getInstance(vertx);
        this.redisAPI = RedisAPI.api(redisClient);
    }

    @Override
    public Future<Void> save(String correlationId,
                             String gatewayType,
                             long amountInCents,
                             Instant timestamp) {
        String zsetKey = "payments:" + gatewayType;
        String idSetKey = zsetKey + ":correlationIds";
        String member = amountInCents + ":" + correlationId;
        String score = String.valueOf(timestamp.toEpochMilli());

        Promise<Void> promise = Promise.promise();

        // Step 1: Try to add correlationId to the set (returns 1 if new, 0 if duplicate)
        this.redisAPI.sadd(Arrays.asList(idSetKey, correlationId)).onSuccess(res -> {
            if (res != null && "1".equals(res.toString())) {
                // Step 2: Add payment to ZSET
                this.redisAPI.zadd(Arrays.asList(zsetKey, score, member))
                        .onSuccess(r -> {
                            String utcDate = DateTimeFormatter.ISO_INSTANT.format(timestamp);
                            promise.complete();
                        })
                        .onFailure(promise::fail);
            } else {
                promise.fail("Duplicate correlationId for this gateway");
            }
        }).onFailure(promise::fail);

        return promise.future();
    }


    public Future<PaymentSummaryByGatewayResponse> get(Instant from, Instant to) {
        Promise<PaymentSummaryByGatewayResponse> promise = Promise.promise();
        List<String> gateways = List.of(PROCESSOR_GATEWAY_SELECTED_DEFAULT, PROCESSOR_GATEWAY_SELECTED_FALLBACK);

        long fromM = from == null ? 0L : from.toEpochMilli();
        long toM = to == null ? Long.MAX_VALUE : to.toEpochMilli();

        List<Future> futures = new ArrayList<>();
        for (String gateway : gateways) {
            String zsetKey = "payments:" + gateway;
            futures.add(this.redisAPI.zrangebyscore(Arrays.asList(zsetKey, String.valueOf(fromM), String.valueOf(toM))));
        }

        CompositeFutureImpl.all(futures.toArray(new Future[0])).onComplete(ar -> {
            if (ar.succeeded()) {
                Map<String, PaymentSummary> summaryByGateway = new LinkedHashMap<>();
                for (int i = 0; i < gateways.size(); i++) {
                    String gateway = gateways.get(i);
                    // Use Response type, not List
                    io.vertx.redis.client.Response response = (io.vertx.redis.client.Response) ar.result().resultAt(i);
                    long totalAmount = 0;
                    int count = 0;
                    if (response != null && response.size() > 0) {
                        for (int j = 0; j < response.size(); j++) {
                            String member = response.get(j).toString();
                            String[] parts = member.split(":");
                            if (parts.length >= 1) {
                                totalAmount += Long.parseLong(parts[0]);
                                count++;
                            }
                        }
                    }
                    double totalFormatted = BigDecimal.valueOf(totalAmount).movePointLeft(2)
                            .setScale(2, RoundingMode.HALF_UP).doubleValue();
                    summaryByGateway.put(gateway, new PaymentSummary(count, totalFormatted));
                }
                promise.complete(new PaymentSummaryByGatewayResponse(summaryByGateway));
            } else {
                promise.fail(ar.cause());
            }
        });
        return promise.future();
    }

}