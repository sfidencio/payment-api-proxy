package config;

import repository.PaymentRepository;
import repository.impl.RedisPaymentRepository;
import service.processor.IPaymentProcess;
import service.processor.impl.PaymentProcessImpl;

public class PaymentDependencies {

    private static final PaymentDependencies INSTANCE = new PaymentDependencies();

    private final IPaymentProcess paymentProcess;
    private final PaymentRepository repository;

    private PaymentDependencies() {
        this.paymentProcess = new PaymentProcessImpl();
        this.repository = new RedisPaymentRepository();
    }

    public static PaymentDependencies getInstance() {
        return INSTANCE;
    }

    public IPaymentProcess getPaymentProcess() {
        return paymentProcess;
    }

    public PaymentRepository getRepository() {
        return repository;
    }
}
