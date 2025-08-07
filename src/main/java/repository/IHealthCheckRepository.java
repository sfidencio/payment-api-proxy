package repository;

import dto.GatewayHealth;
import io.vertx.core.Future;

public interface IHealthCheckRepository {
    Future<Void> saveHealth(String key, GatewayHealth health);

    Future<GatewayHealth> getHealth(String key);
}
