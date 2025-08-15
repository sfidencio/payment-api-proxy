package config;

import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;

import java.util.logging.Logger;

import static config.Constants.*;

public class DatabaseProvider {

    private static final Logger logger = Logger.getLogger(DatabaseProvider.class.getName());


    private static Pool instance;

    public static Pool getInstance(Vertx vertx) {
        if (instance == null) {
            PgConnectOptions connectOptions = new PgConnectOptions()
                    .setHost(Environment.getEnv(POSTGRES_HOST))
                    .setPort(Integer.parseInt(Environment.getEnv(POSTGRES_PORT)))
                    .setDatabase(Environment.getEnv(POSTGRES_DB))
                    .setUser(Environment.getEnv(POSTGRES_USER))
                    .setPassword(Environment.getEnv(POSTGRES_PASSWORD));

            var poolOptions = new PoolOptions().setMaxSize(Integer.parseInt(Environment.getEnv(POSTGRES_MAX_POOL_SIZE)));
            instance = Pool.pool(
                    vertx,
                    connectOptions,
                    poolOptions
            );

            buildSchema();
        }

        return instance;
    }

    private static void buildSchema() {
        // Create gateway_health table if not exists
        String createGatewayHealthTable = "CREATE TABLE IF NOT EXISTS gateway_health (" +
                "key VARCHAR PRIMARY KEY, " +
                "failing BOOLEAN NOT NULL, " +
                "min_response_time INTEGER NOT NULL, " +
                "timestamp TIMESTAMPTZ NOT NULL" +
                ")";
        instance.query(createGatewayHealthTable).execute()
                .onSuccess(res -> Environment.processLogging(logger, "Gateway_health table checked/created."))
                .onFailure(err -> Environment.processLogging(logger, "Error creating gateway_health table: " + err.getMessage()));


        // Create payments table if not exists
        String createPaymentsTable = "CREATE TABLE IF NOT EXISTS payments (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "correlation_id VARCHAR NOT NULL UNIQUE, " +
                "amount DECIMAL(15,2) NOT NULL, " +
                "gateway_type VARCHAR NOT NULL, " +
                "status_code INTEGER NOT NULL, " +
                "created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP" +
                ")";
        instance.query(createPaymentsTable).execute()
                .onSuccess(res -> Environment.processLogging(logger, "Payments table checked/created."))
                .onFailure(err -> Environment.processLogging(logger, "Error creating payments table: " + err.getMessage()));

        // Create payment_queue table if not exists
        String createPaymentQueueTable = "CREATE TABLE IF NOT EXISTS payment_queue (" +
                "id BIGSERIAL PRIMARY KEY, " +
                "correlation_id VARCHAR NOT NULL UNIQUE, " +
                "amount DECIMAL(15,2) NOT NULL, " +
                "gateway_type VARCHAR NOT NULL, " +
                "status_code INTEGER NOT NULL, " +
                "created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP, " +
                "retry_count INTEGER DEFAULT 0," +
                "status VARCHAR DEFAULT 'PENDING'," +
                "consumer_name VARCHAR DEFAULT 'consumer-default'" +
                ")";
        instance.query(createPaymentQueueTable).execute()
                .onSuccess(res -> Environment.processLogging(logger, "Payment_queue table checked/created."))
                .onFailure(err -> Environment.processLogging(logger, "Error creating payment_queue table: " + err.getMessage()));
    }


}
