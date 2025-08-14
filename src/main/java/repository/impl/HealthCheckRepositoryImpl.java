package repository.impl;

import config.DatabaseProvider;
import dto.GatewayHealth;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;
import repository.IHealthCheckRepository;

import java.time.Instant;
import java.util.logging.Logger;


public class HealthCheckRepositoryImpl implements IHealthCheckRepository {

    private static final Logger logger = Logger.getLogger(HealthCheckRepositoryImpl.class.getName());
    private final Pool pool;

    /**
     * Construtor que inicializa o cliente Redis.
     *
     * @param vertx Instância do Vertx.
     */
    public HealthCheckRepositoryImpl(Vertx vertx) {
        this.pool = DatabaseProvider.getInstance(vertx);
    }

    /**
     * Salva o status de health check no Redis.
     *
     * @param key    Chave do registro.
     * @param health Dados de health check.
     * @return Future indicando sucesso ou falha.
     */
    @Override
    public Future<Void> saveHealth(String key, GatewayHealth health) {
        var sql = "INSERT INTO gateway_health (key, failing, min_response_time, timestamp) VALUES ($1, $2, $3, $4) " +
                "ON CONFLICT (key) DO UPDATE SET failing = $2, min_response_time = $3, timestamp = $4";
        var offsetDateTime = health.lastChecked() != null ? health.lastChecked().atOffset(java.time.ZoneOffset.UTC) : null;
        return this.pool.preparedQuery(sql)
                .execute(Tuple.of(
                        key,
                        health.failing(),
                        health.minResponseTime(),
                        offsetDateTime
                )).mapEmpty();
    }

    /**
     * Recupera o status de health check do Banco.
     *
     * @param key Chave do registro.
     * @return Future com os dados de health check ou null se não encontrado.
     */
    @Override
    public Future<GatewayHealth> getHealth(String key) {
        var sql = "SELECT failing, min_response_time, timestamp FROM gateway_health WHERE key = $1";
        return this.pool.preparedQuery(sql)
                .execute(
                        Tuple.of(key)
                ).map(rowSet -> {
                            if (rowSet.rowCount() == 0) {
                                return null; // Não encontrado
                            }
                            var row = rowSet.iterator().next();
                            var offsetDateTime = row.getOffsetDateTime("timestamp");
                            Instant instant = offsetDateTime != null ? offsetDateTime.toInstant() : null;
                            return new GatewayHealth(
                                    row.getBoolean("failing"),
                                    row.getLong("min_response_time"),
                                    instant
                            );
                        }
                );
    }
}
