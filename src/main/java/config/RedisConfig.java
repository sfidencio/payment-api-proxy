package config;

import dto.PaymentRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
public class RedisConfig {
    @Bean
    public ReactiveRedisTemplate<String, PaymentRequest> reactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {
        return new ReactiveRedisTemplate<>(
                factory,
                RedisSerializationContext
                        .<String, PaymentRequest>newSerializationContext(new GenericJackson2JsonRedisSerializer())
                        .key((RedisSerializationContext.SerializationPair<String>) new GenericJackson2JsonRedisSerializer())
                        .value((RedisSerializationContext.SerializationPair<PaymentRequest>) new GenericJackson2JsonRedisSerializer())
                        .build()
        );
    }
}