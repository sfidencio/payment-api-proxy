# payment-api-proxy 🇧🇷💳

Um servidor HTTP Java de altíssimo desempenho e baixíssima latência para proxy de requisições de pagamento para gateways externos, projetado para uso mínimo de recursos e máxima extensibilidade.

## Visão Geral ✨

- Expõe o endpoint `/payment` na porta 8080 para processamento de pagamentos (apenas POST).
- Faz o parsing do JSON recebido e processa pagamentos via camada de serviço.
- Suporta múltiplos gateways de pagamento e health checks.
- Construído para alta vazão e baixíssimo consumo de memória.

## Sem Frameworks Pesados, Só Vert.x 🚫🛠️

Este projeto é implementado **sem frameworks pesados** (nada de Spring, Quarkus, Micronaut, etc). Utiliza apenas a biblioteca padrão do Java (`com.sun.net.httpserver.HttpServer`) e o **Vert.x** como framework reativo para processamento assíncrono e concorrente.

### Por que Vert.x?
- **Reatividade real:** Processamento totalmente assíncrono e não bloqueante, ideal para alta concorrência.
- **Baixíssimo overhead:** Sem camadas extras, sem reflection, sem injeção de dependências pesada.
- **Startup instantâneo:** Perfeito para ambientes serverless e alta disponibilidade.
- **Consumo mínimo de memória:** Ideal para containers com limites rígidos de recursos.git push origin --delete feature-x
- **Controle total:** Permite otimizações customizadas para performance e latência.

## Arquitetura 🏗️

- **Camada HTTP:** Recebe requisições, faz parsing do JSON e delega para a camada de serviço.
- **Camada de Serviço:** Contém a lógica de negócio para processamento de pagamentos, seleção de gateway e health checks.
- **Camada de Repositório:** Gerencia persistência (PostgreSQL) e enfileiramento das mensagens de pagamento.
- **Health Checks:** Verifica periodicamente a saúde dos gateways e atualiza o status.
- **Endpoint de Sumário:** Fornece estatísticas em tempo real dos pagamentos e checagem de consistência.

## Principais Funcionalidades 🚀

- **Processamento em Lote:** Consome e processa mensagens de pagamento em lotes, de forma eficiente.
- **Operações Atômicas na Fila:** Usa SQL com `FOR UPDATE SKIP LOCKED` para processamento concorrente seguro.
- **Controle Manual do Consumer:** Verticles fazem polling e ack das mensagens, garantindo confiabilidade.
- **Ack Resiliente:** Sempre faz ack das mensagens, com reinfileiramento seguro em caso de falha.
- **Suporte Extensível a Gateways:** Fácil adicionar novos gateways ou lógica de fallback.

## Deploy & Tuning 🐳

- **Pronto para Docker:** Otimizado para ambientes containerizados com baixo uso de CPU/memória.
- **PostgreSQL:** Configuração mínima, pode rodar com apenas 90MB de RAM.
- **Nginx (opcional):** Para balanceamento de carga e alta concorrência.
- **Tuning de Recursos:** Exemplo: 2 containers da app (64MB RAM, 0,5 CPU cada), Nginx (50MB RAM), PostgreSQL (90MB RAM).

## Exemplo de Docker Compose

```yaml
version: '3.8'
services:
  app:
    build: .
    deploy:
      resources:
        limits:
          cpus: '0.5'
          memory: '64MB'
    depends_on:
      - pgsql
  pgsql:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: paymentdb
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: pay@@!
    command: ["postgres", "-c", "max_connections=200"]
    deploy:
      resources:
        limits:
          cpus: '0.3'
          memory: '90MB'
  nginx:
    image: nginx:alpine
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
    deploy:
      resources:
        limits:
          cpus: '0.2'
          memory: '50MB'
```

## Como Funciona ⚙️

1. **Enfileiramento:** Requisições de pagamento são enfileiradas no banco com status `PENDING`.
2. **Consumo:** Verticles do Vert.x fazem polling por mensagens `PENDING`, marcam como `PROCESSING` de forma atômica e processam em lote.
3. **Ack:** Após o processamento, as mensagens são ack (deletadas). Se falhar, só reinfileira se o ack foi sucesso.
4. **Sumário:** O endpoint `/summary` fornece estatísticas em tempo real, garantindo consistência mesmo sob alta carga.

## Dicas de Performance 💡

- **Tamanho do Lote:** Ajuste o tamanho do lote conforme sua CPU/memória (comece com 20-50 para 0,5 CPU/64MB RAM).
- **Intervalo de Polling:** Use intervalos menores (ex: 0-100ms) para maior vazão.
- **Pool de Conexões:** Limite o pool de conexões do banco para não esgotar recursos (ex: 20-50 conexões para 90MB RAM).
- **Buffers do Nginx:** Garanta pelo menos 2 proxy buffers no `nginx.conf` para estabilidade.

## Detalhes do Projeto & Insights de Performance ⚡

### Componentes Principais

- **Produtor (PaymentHandler):**
  - Recebe requisições de pagamento via HTTP POST em `/payment`.
  - Valida e enfileira cada pagamento como uma mensagem na tabela `payment_queue` com status `PENDING`.
  - Retorna HTTP 200 imediatamente após enfileirar, garantindo resposta ultra-rápida e desacoplando o recebimento do processamento.

- **Consumidor (PaymentProcessorVerticle):**
  - Faz polling periódico no banco por novas mensagens com status `PENDING`.
  - Usa operação SQL atômica com `SELECT ... FOR UPDATE SKIP LOCKED` para capturar com segurança um lote de mensagens, atualizando o status para `PROCESSING` e atribuindo o nome do consumidor.
  - Processa cada pagamento de forma assíncrona, chamando o gateway apropriado.
  - Após o processamento, sempre faz o ack (deleta) da mensagem da fila. Se o processamento falhar, só reinfilera se o ack (delete) foi bem-sucedido, evitando duplicidade.

- **Health Check & Fallback:**
  - Verifica periodicamente a saúde de cada gateway de pagamento.
  - Se um gateway estiver fora, roteia automaticamente os pagamentos para um gateway de fallback, garantindo alta disponibilidade.

- **Endpoint de Sumário:**
  - Expõe `/summary` para fornecer estatísticas em tempo real dos pagamentos processados, incluindo totais por gateway e checagens de consistência.

### Técnicas de Performance

- **Processamento em Lote:**
  - Consumidores buscam e processam mensagens em lotes (tamanho configurável), reduzindo idas ao banco e aumentando a vazão.

- **Claim Atômico da Fila:**
  - O uso de `FOR UPDATE SKIP LOCKED` no SQL garante que múltiplos consumidores possam processar a fila em paralelo sem condições de corrida ou processamento duplo.

- **Controle Manual do Consumidor:**
  - Sem auto-ack: o consumidor só remove a mensagem da fila após o processamento bem-sucedido, garantindo confiabilidade e semântica de pelo menos uma vez.

- **Uso Mínimo de Recursos:**
  - Sem frameworks pesados, sem reflection, sem containers de injeção de dependência.
  - Toda lógica é assíncrona e não bloqueante (Vert.x), permitindo milhares de requisições concorrentes com poucas threads.
  - Projetado para rodar com apenas 64MB RAM por container da aplicação e 90MB RAM para o PostgreSQL.

- **Tuning do Banco:**
  - Pool de conexões e tamanho do lote ajustados para baixa memória e alta concorrência.
  - PostgreSQL iniciado com baixo `max_connections` e buffers mínimos.

- **Tuning do Nginx (se usado):**
  - Configurado com buffers e workers mínimos para caber em 50MB RAM, suportando centenas de conexões simultâneas.

### O que pode Impactar a Performance

- **Intervalo de Polling Alto:**
  - Intervalos grandes entre polls do consumidor (ex: 2000ms) aumentam latência e reduzem vazão. Use intervalos baixos (0-100ms) para alta performance.

- **Lote Pequeno:**
  - Buscar poucas mensagens por vez aumenta carga no banco e reduz eficiência. Ajuste conforme memória e CPU disponíveis.

- **Excesso de Conexões:**
  - Pool ou `max_connections` muito alto pode esgotar memória e degradar performance, especialmente em containers enxutos.

- **Código Síncrono/Bloqueante:**
  - Qualquer operação bloqueante no produtor ou consumidor reduz a vazão. Todas as chamadas ao banco e HTTP devem ser assíncronas.

- **Mensagens não Acknowledged:**
  - Se uma mensagem não for ack (deletada) após o processamento, pode ser reprocessada, causando duplicidade e desperdício de recursos.

- **SQL Ineficiente:**
  - Evite full scan ou falta de índices. A tabela da fila deve ser indexada por `status` e `created_at` para polling rápido.

### Boas Práticas

- Sempre ajuste tamanho do lote, intervalo de polling e pool de conexões para seu cenário.
- Monitore o tamanho da fila e latência de processamento para detectar gargalos.
- Use health checks e fallback para máxima disponibilidade.
- Mantenha o código minimalista e evite dependências desnecessárias para melhor cold start e uso de memória.

## Licença 📄

Licença MIT. Veja [LICENSE](LICENSE) para detalhes.

---

*Feito com ❤️ para processamento de pagamentos de baixíssima latência em ambientes com recursos restritos, usando Vert.x como motor reativo.*
