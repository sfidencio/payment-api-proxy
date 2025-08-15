package service.consumer;

import config.Environment;
import config.PaymentDependencies;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import repository.IPaymentRepository;
import service.producer.IPaymentService;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static config.Constants.MAX_RETRIES_REENQUEUE;

/**
 * Verticle responsável pelo processamento de pagamentos em lote.
 * <p>
 * Esta classe utiliza o Vert.x para agendar periodicamente a busca de mensagens de pagamento
 * pendentes em um repositório, processando cada uma delas através do serviço de pagamentos.
 * Após o processamento, cada mensagem é confirmada (acknowledged) para evitar reprocessamentos.
 * </p>
 *
 * <p>
 * Detalhes dos componentes:
 * <ul>
 *   <li><b>IPaymentRepository</b>: Interface para acesso ao repositório de pagamentos, responsável por consumir e confirmar mensagens.</li>
 *   <li><b>IPaymentService</b>: Interface do serviço de processamento de pagamentos, responsável pela lógica de negócio do processamento.</li>
 *   <li><b>PaymentDependencies</b>: Classe utilitária para injeção de dependências dos componentes acima.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Funcionamento:
 * <ol>
 *   <li>Ao iniciar, agenda a execução periódica do método {@link #poll()}.</li>
 *   <li>No método <code>poll()</code>, consome um lote de até 10 mensagens de pagamento pendentes.</li>
 *   <li>Cada mensagem é processada de forma assíncrona pelo serviço de pagamentos.</li>
 *   <li>Após o processamento (com sucesso ou falha), a mensagem é confirmada para evitar duplicidade.</li>
 * </ol>
 * </p>
 *
 * <p>
 * Observação: O intervalo de polling está configurado para 2 milissegundos, podendo ser ajustado conforme a necessidade.
 * </p>
 *
 * @author Sebastiao Fidencio
 * @since 1.0
 */
public class PaymentProcessorVerticle extends AbstractVerticle {
    private static final Logger logger = Logger.getLogger(PaymentProcessorVerticle.class.getName());


    /**
     * Repositório de pagamentos utilizado para consumir e confirmar mensagens.
     */
    private final IPaymentRepository paymentRepository;
    /**
     * Serviço responsável pelo processamento das mensagens de pagamento.
     */
    private final IPaymentService paymentService;

    private final String consumerName;

    /**
     * Construtor padrão. Inicializa as dependências de repositório e serviço de pagamento.
     */
    public PaymentProcessorVerticle(String consumerName) {
        this.consumerName = consumerName;
        paymentRepository = PaymentDependencies.getPaymentRepository();
        paymentService = PaymentDependencies.getPaymentService();
    }

    /**
     * Método chamado automaticamente pelo Vert.x ao iniciar o verticle.
     * Inicia o polling periódico para processar pagamentos.
     *
     * @param startPromise Promessa de inicialização do verticle.
     */
    @Override
    public void start(Promise<Void> startPromise) {
        poll();
        Environment.processLogging(
                logger,
                "PaymentProcessorVerticle started and polling for payment messages...".concat(Thread.currentThread().getName()));

        startPromise.complete();
    }

    private void poll() {
        // Lógica reativa: consome o próximo lote imediatamente após terminar o anterior
        processBatch();
    }

    private void processBatch() {

//        if (ConsumerControl.paused) {
//            // Se o consumidor estiver pausado, não faz nada e aguarda a próxima chamada
//            Environment.processLogging(logger, "Consumer is paused. Waiting for resumption...");
//            vertx.setTimer(100, t -> processBatch());
//            return;
//        }

        this.paymentRepository.consumeBatch(30, this.consumerName).onSuccess(reqs -> {
            if (reqs.isEmpty()) {
                // Se não há mensagens, agenda para tentar novamente em 100ms
                vertx.setTimer(100, t -> processBatch());
                return;
            }
            AtomicInteger processed = new AtomicInteger();
            for (var msg : reqs) {
                this.paymentService.process(msg.paymentRequest())
                        .onComplete(ar -> {
                            int maxRetries = Integer.parseInt(Environment.getEnv(MAX_RETRIES_REENQUEUE));
                            int retryCount = msg.paymentRequest().getRetryCount();
                            if (ar.succeeded() || retryCount >= maxRetries) {
                                this.paymentRepository.ack(msg.messageID())
                                        .onSuccess(v -> Environment.processLogging(
                                                logger,
                                                "Message with ID " + msg.messageID() + " acknowledged successfully."))
                                        .onFailure(err -> Environment.processLogging(
                                                logger,
                                                "Failed to acknowledge message with ID " + msg.messageID() + ": " + err.getMessage()));
                            } else {
                                this.paymentRepository.incrementRetryCount(msg.messageID())
                                        .onSuccess(v -> Environment.processLogging(
                                                logger,
                                                "Retry count incremented for message ID " + msg.messageID() + ". Current retry count: " + v.longValue()))
                                        .onFailure(err -> Environment.processLogging(
                                                logger,
                                                "Failed to increment retry count for message ID " + msg.messageID() + ": " + err.getMessage()));
                            }
                            // Após processar todos do lote, chama o próximo
                            if (processed.incrementAndGet() == reqs.size()) {
                                vertx.runOnContext(v -> processBatch());
                            }
                        });
            }
        }).onFailure(err -> {
            // Em caso de erro ao consumir lote, tenta novamente em 50ms
            Environment.processLogging(logger, "Failed to consume batch: " + err.getMessage());
            vertx.setTimer(50, t -> processBatch());
        });
    }

}
