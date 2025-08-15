package controller;

import dto.PaymentRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    List<String> endpoints = List.of(
            "http://servico-default/pagamento",
            "http://servico-1/pagamento",
            "http://servico-2/pagamento"
    );

    private final WebClient webClient;
    private final ReactiveValueOperations<String, PaymentRequest> valueOps;

    @Autowired
    public PaymentController(ReactiveRedisTemplate<String, PaymentRequest> redisTemplate) {
        this.webClient = WebClient.create();
        this.valueOps = redisTemplate.opsForValue();
    }

    @PostMapping
    public Mono<ResponseEntity<String>> processPayment(@RequestBody PaymentRequest paymentRequest) {
        List<String> endpoints = List.of(
                "http://payment-processor-default:8080",
                "http://payment-processor-fallback:8080"
        );
        AtomicInteger attempt = new AtomicInteger(0);

        return Mono.defer(() -> {
                    int currentAttempt = attempt.getAndIncrement();
                    String endpoint;
                    String gateway;
                    if (currentAttempt == 0) {
                        endpoint = endpoints.get(0); // default
                        gateway = "default";
                    } else {
                        endpoint = endpoints.get(1); // fallback
                        gateway = "fallback";
                    }
                    String redisKey = "payment:" + gateway + ":" + paymentRequest.getCorrelationId();
                    return webClient.post()
                            .uri(endpoint)
                            .bodyValue(paymentRequest)
                            .retrieve()
                            .toBodilessEntity()
                            .flatMap(response -> {
                                if (response.getStatusCode().is2xxSuccessful()) {
                                    return valueOps.set(redisKey, paymentRequest)
                                            .thenReturn(ResponseEntity.ok("Pagamento processado e salvo."));
                                } else {
                                    return Mono.error(new RuntimeException("Erro ao processar pagamento."));
                                }
                            });
                }).retry(2) // 1 tentativa no default, 2 no fallback
                .onErrorResume(e -> Mono.just(ResponseEntity.status(502).body("Erro ao processar pagamento.")));
    }
}

