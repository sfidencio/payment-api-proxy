# payment-api-proxy 🇧🇷💳

Um servidor HTTP Java simples para fazer proxy de requisições de pagamento para gateways externos.

## Visão Geral ✨

- Expõe o endpoint `/payment` na porta 8080.
- Aceita apenas requisições POST para processamento de pagamentos.
- Faz o parsing do payload JSON recebido e processa pagamentos via camada de serviço.
- Projetado para ser extensível para múltiplos gateways de pagamento e health checks.

## Sem Frameworks 🚫🛠️

Este projeto **não utiliza nenhum framework** (como Spring, Quarkus, Micronaut, etc). Toda a implementação é feita apenas com a biblioteca padrão do Java (`com.sun.net.httpserver.HttpServer`).

### Vantagens para Baixa Latência ⚡

- **Menor overhead:** Sem camadas extras de abstração, o processamento das requisições é mais direto e rápido.
- **Inicialização instantânea:** O servidor sobe quase instantaneamente, ideal para cenários serverless ou de alta disponibilidade.
- **Menor consumo de memória:** Sem dependências externas, o uso de recursos é mínimo.
- **Controle total:** Permite otimizações específicas para cenários de alta performance e baixa latência.

Essa abordagem é indicada para soluções onde cada milissegundo conta, como sistemas financeiros, proxies de API e gateways de pagamento.

## Principais Componentes 🧩

### run.Application.java
- Ponto de entrada. Inicializa o `PaymentService` e inicia o servidor HTTP.

### PaymentHandler.java
- Manipula requisições HTTP para `/payment`.
- Aceita apenas requisições POST.
- Lê o corpo da requisição, delega o processamento ao `PaymentService` e retorna uma resposta JSON.

### PaymentService.java
- Contém a lógica de negócio para processar pagamentos.
- Atualmente faz o parsing do valor do pagamento do corpo da requisição.
- Estrutura pronta para integração com gateways de pagamento.

### Verb.java
- Enum para os verbos HTTP suportados.
- Método utilitário para validar métodos HTTP.

### GatewayHealth.java
- Record para status de saúde dos gateways de pagamento.

## Como Executar 🚀

1. Faça o build do projeto com Maven.
2. Execute o `run.Application.java`.
3. Envie requisições POST para `http://localhost:8080/payment` com um corpo JSON.

## Exemplo de Requisição 📦

```json
POST /payment
Content-Type: application/json

{
  "amount": 100.0
}
```

## Exemplos de chamada com curl 🖥��

```bash
curl -X POST http://localhost:8080/payment \
  -H "Content-Type: application/json" \
  -d '{"amount": 100.0}'

curl -X POST http://localhost:8080/payment \
  -H "Content-Type: application/json" \
  -d '{"amount": 250.5}'
```
