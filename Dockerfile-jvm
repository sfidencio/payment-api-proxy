FROM eclipse-temurin:24-jre-alpine

WORKDIR /app

COPY target/payment-api-proxy.jar /app/payment-api-proxy.jar

ENV JAVAARGS=""

EXPOSE 8080

CMD sh -c "java $JAVAARGS -jar /app/payment-api-proxy.jar"
