package repository.impl;

import config.DatabaseProvider;
import config.Environment;
import dto.PaymentMessage;
import dto.PaymentRequest;
import dto.PaymentSummary;
import dto.PaymentSummaryByGatewayResponse;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;
import repository.IPaymentRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class PaymentRepositoryImpl implements IPaymentRepository {

    private final Pool pool;

    private static final Logger logger = Logger.getLogger(PaymentRepositoryImpl.class.getName());

    public PaymentRepositoryImpl(Vertx vertx) {
        this.pool = DatabaseProvider.getInstance(vertx);
    }


    /**
     * Salva um novo pagamento na tabela de pagamentos.
     * <p>
     * Insere um novo registro na tabela 'payments' com os dados do pagamento recebido.
     * Retorna um Future indicando se a operação foi bem-sucedida (true) ou não (false).
     * </p>
     *
     * @param request objeto PaymentRequest contendo os dados do pagamento
     * @return Future<Boolean> indicando sucesso (true) ou falha (false) na inserção
     */
    @Override
    public Future<Boolean> save(PaymentRequest request) {

        var sql = "INSERT INTO payments (correlation_id, amount, gateway_type, status_code) VALUES ($1, $2, $3, $4)";

        Promise<Boolean> promise = Promise.promise();
        pool.preparedQuery(sql)
                .execute(Tuple.of(
                        request.getCorrelationId(),
                        request.getAmount(),
                        request.getGatewayType(),
                        request.getStatusCode()
                ), ar -> {
                    if (ar.succeeded()) {
                        boolean inserted = ar.result().rowCount() > 0;
                        promise.complete(inserted);
                    } else {
                        promise.fail(ar.cause());
                    }
                });
        return promise.future();

    }

    /**
     * Recupera um resumo dos pagamentos agrupados por gateway em um intervalo de tempo.
     * <p>
     * Executa uma consulta SQL para somar os valores e contar os pagamentos por gateway,
     * considerando apenas os registros criados entre 'from' e 'to'.
     * </p>
     *
     * @param from data/hora inicial do intervalo (pode ser null)
     * @param to   data/hora final do intervalo (pode ser null)
     * @return Future com o resumo dos pagamentos por gateway
     */
    @Override
    public Future<PaymentSummaryByGatewayResponse> get(Instant from, Instant to) {
        List<String> gateways = List.of("default", "fallback");


        // Exemplo: sumariza pagamentos por gateway nesse intervalo
        var sql = new StringBuilder(
                "SELECT gateway_type, " +
                        "SUM(amount) AS total_amount, " +
                        "COUNT(*) AS total_payments " +
                        "FROM payments "
        );

        List<Object> params = new ArrayList<>();

        List<String> where = new ArrayList<>();

        if (from != null) {
            where.add("created_at >= $1");
            params.add(from.atOffset(ZoneOffset.UTC));
        }
        if (to != null) {
            where.add("created_at <= $" + (params.size() + 1));
            params.add(to.atOffset(ZoneOffset.UTC));
        }
        if (!where.isEmpty()) {
            sql.append("WHERE ").append(String.join(" AND ", where)).append(" ");
        }
        sql.append("GROUP BY gateway_type");

        return this.pool.preparedQuery(sql.toString())
                .execute(Tuple.tuple(params))
                .map(rows -> {
                    Map<String, PaymentSummary> summary = new LinkedHashMap<>();
                    rows.forEach(row -> {
                        String gateway = row.getString("gateway_type");
                        BigDecimal totalAmount = row.getBigDecimal("total_amount").movePointLeft(2);
                        Integer totalPayments = row.getInteger("total_payments");
                        summary.put(gateway, new PaymentSummary(totalPayments, totalAmount));
                    });


//                    if (summary.isEmpty()) {
//                        for (String gateway : gateways) {
//                            summary.put(gateway, new PaymentSummary(0, BigDecimal.ZERO));
//                        }
//                    }

                    for (String gateway : gateways) {
                        summary.putIfAbsent(gateway, new PaymentSummary(0, BigDecimal.ZERO));
                    }
                    return new PaymentSummaryByGatewayResponse(summary);
                });
    }

    /**
     * Consome um lote de mensagens da fila de pagamentos de forma segura e concorrente.
     * <p>
     * Este método utiliza um UPDATE atômico com SELECT ... FOR UPDATE SKIP LOCKED para garantir que
     * cada mensagem seja reservada para apenas um consumidor (verticle) por vez, evitando duplicidade
     * de processamento em ambientes concorrentes.
     * </p>
     *
     * @param batchSize    quantidade máxima de mensagens a consumir no lote
     * @param consumerName nome do consumidor/verticle
     * @return Future com a lista de mensagens reservadas para processamento
     */
    @Override
    public Future<List<PaymentMessage>> consumeBatch(int batchSize, String consumerName) {
        var sql = "UPDATE payment_queue " +
                "SET status = 'PROCESSING', consumer_name = $2 " +
                "WHERE id IN ( " +
                "  SELECT id FROM payment_queue " +
                "  WHERE status = 'PENDING' " +
                "  ORDER BY created_at " +
                "  LIMIT $1 FOR UPDATE SKIP LOCKED " +
                ") RETURNING *";

        return pool.preparedQuery(sql)
                .execute(Tuple.of(batchSize, consumerName))
                .map(rows -> {
                    List<PaymentMessage> messages = new ArrayList<>();
                    rows.forEach(row -> {
                        long id = row.getLong("id");
                        String correlationId = row.getString("correlation_id");
                        BigDecimal amount = row.getBigDecimal("amount");
                        String gatewayType = row.getString("gateway_type");
                        int statusCode = row.getInteger("status_code");
                        Instant createdAt = row.getOffsetDateTime("created_at").toInstant();
                        int retryCount = row.getInteger("retry_count");
                        String status = row.getString("status");
                        PaymentMessage message = new PaymentMessage(id,
                                new PaymentRequest(
                                        correlationId,
                                        amount,
                                        gatewayType,
                                        createdAt,
                                        statusCode,
                                        retryCount,
                                        status
                                )
                        );
                        messages.add(message);
                    });
                    return messages;
                });
    }

    /**
     * Remove (acknowledge) uma mensagem da fila de pagamentos pelo seu ID.
     * <p>
     * Executa um DELETE na tabela 'payment_queue' para remover a mensagem processada.
     * O Future retornado indica sucesso ou falha da operação.
     * </p>
     *
     * @param messageId identificador da mensagem a ser removida
     * @return Future<Void> indicando sucesso ou falha do ack
     */
    @Override
    public Future<Void> ack(long messageId) {
        var sql = """
                DELETE FROM payment_queue WHERE id = $1
                """;

        return this.pool.preparedQuery(sql)
                .execute(Tuple.of(messageId))
                .mapEmpty();
    }

    /**
     * Enfileira uma nova mensagem de pagamento na tabela de fila.
     * <p>
     * Insere um novo registro na tabela 'payment_queue' com os dados do pagamento.
     * Retorna um Future<Boolean> indicando se a operação foi bem-sucedida.
     * </p>
     *
     * @param request objeto PaymentRequest a ser enfileirado
     * @return Future<Boolean> indicando sucesso (true) ou falha (false) na operação
     */
    @Override
    public Future<Boolean> enqueue(PaymentRequest request) {
        var sql = """
                INSERT INTO payment_queue (correlation_id, amount, gateway_type, status_code)
                VALUES ($1, $2, $3, $4)
                """;

        Promise<Boolean> promise = Promise.promise();
        this.pool.preparedQuery(sql)
                .execute(Tuple.of(
                        request.getCorrelationId(),
                        request.getAmount(),
                        request.getGatewayType(),
                        request.getStatusCode()
                ), ar -> {
                    if (ar.succeeded()) {
                        Environment.processLogging(
                                logger,
                                "Payment successfully enqueued for correlation ID: " + request.getCorrelationId()
                        );
                        promise.complete(true);
                    } else {
                        Environment.processLogging(
                                logger,
                                "Failed to enqueue payment for correlation ID: " + request.getCorrelationId() +
                                        " - Error: " + ar.cause().getMessage()
                        );
                        promise.complete(false);
                    }
                });
        return promise.future();
    }

    /**
     * Incrementa o contador de tentativas (retry_count) de uma mensagem na fila e a marca como 'PENDING'.
     * <p>
     * Atualiza o campo retry_count da mensagem com o ID informado, permitindo o reprocessamento.
     * Retorna o novo valor do contador de tentativas.
     * </p>
     *
     * @param id identificador da mensagem na fila
     * @return Future<Integer> com o novo valor de retry_count
     */
    @Override
    public Future<Integer> incrementRetryCount(long id) {
        var sql = "UPDATE payment_queue SET retry_count = retry_count + 1, status='PENDING' WHERE id = $1 RETURNING retry_count";

        Promise<Integer> promise = Promise.promise();

        pool.preparedQuery(sql)
                .execute(Tuple.of(id), ar -> {
                    if (ar.succeeded()) {
                        var row = ar.result().iterator().next();
                        int newRetryCount = row.getInteger("retry_count");
                        Environment.processLogging(
                                logger,
                                "Incremented retry count for message ID: " + id + " - New count: " + newRetryCount
                        );
                        promise.complete(newRetryCount);
                    } else {
                        Environment.processLogging(
                                logger,
                                "Failed to increment retry count for message ID: " + id + " - Error: " + ar.cause().getMessage()
                        );
                        promise.fail(ar.cause());
                    }
                });
        return promise.future();
    }
}