package config;

import io.vertx.core.Vertx;
import repository.IHealthCheckRepository;
import repository.IPaymentRepository;
import repository.impl.HealthCheckRepositoryImpl;
import repository.impl.PaymentRepositoryImpl;
import service.IPaymentService;
import service.PaymentServiceImpl;
import service.processor.IPaymentProcess;
import service.processor.impl.PaymentProcessImpl;

public class PaymentDependencies {

    private static PaymentDependencies instance;

    private final IPaymentProcess paymentProcess;
    private final IPaymentService paymentService;
    private final IPaymentRepository paymentRepository;
    private final IHealthCheckRepository redisHealthCheckRepository;

    private PaymentDependencies(Vertx vertx) {
        this.paymentRepository = new PaymentRepositoryImpl(vertx);
        this.redisHealthCheckRepository = new HealthCheckRepositoryImpl(vertx);
        this.paymentProcess = new PaymentProcessImpl(vertx);
        this.paymentService = new PaymentServiceImpl(vertx, paymentRepository, paymentProcess);
    }


    public static synchronized void initialize(Vertx vertx) {
        if (instance == null) {
            instance = new PaymentDependencies(vertx);
        }
    }

    public static PaymentDependencies getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PaymentDependencies not initialized. Call initialize(Vertx) first.");
        }
        return instance;
    }

    public static IPaymentProcess getPaymentProcess() {
        return getInstance().paymentProcess;
    }

    public static IPaymentRepository getPaymentRepository() {
        return getInstance().paymentRepository;
    }

    public static IHealthCheckRepository getRedisHealthCheckRepository() {
        return getInstance().redisHealthCheckRepository;
    }

    public static IPaymentService getPaymentService() {
        return getInstance().paymentService;
    }
}