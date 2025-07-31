FROM ghcr.io/graalvm/native-image-community:24 AS builder

WORKDIR /app

COPY . .

RUN native-image \
    --no-fallback \
    -jar target/payment-api-proxy.jar \
    -H:Name=payment-api-proxy-runner \
    -H:ReflectionConfigurationFiles=src/main/resources/META-INF/native-image/reflect-config.json \
    -H:+ReportUnsupportedElementsAtRuntime \
    --enable-http \
    --enable-https \
    --enable-all-security-services

FROM debian:bookworm-slim

WORKDIR /app

COPY --from=builder /app/payment-api-proxy-runner /app/payment-api-proxy-runner

RUN chmod +x /app/payment-api-proxy-runner

EXPOSE 8080

CMD ["./payment-api-proxy-runner"]
