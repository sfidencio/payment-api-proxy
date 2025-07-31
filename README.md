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

### HttpVerb.java
- Enum para os Verbos HTTP suportados.
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


## Executando a aplicação com Docker 🐳 sem docker-compose

```bash
docker build -t sfidencio/payment-api-proxy:latest .
docker run --name payment-api-proxy --rm -p 8080:8080 sfidencio/payment-api-proxy:latest
```

## Parando a aplicação
```bash
docker stop payment-api-proxy
```


## Executando o processador de pagamentos(proposto pela rinha) com Docker Compose 🐳

```bash
docker compose -f docker-compose-processor.yaml up --build -d
```

## Executando o backend payment-api-proxy com Docker Compose 🐳

```bash
docker compose -f docker-compose.yaml up --build -d
```


## Exemplos de chamada `POST` com curl

```bash
curl -v -X POST http://localhost:8080/payments   \ 
  -H "Content-Type: application/json" \
  -d '{"correlationId": "4a7901b8-7d26-4d9d-aa19-4dc1c7cf60b3", "amount": 100.0}'

```