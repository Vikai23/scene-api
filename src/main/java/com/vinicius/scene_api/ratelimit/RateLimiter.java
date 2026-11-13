package com.vinicius.scene_api.ratelimit;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Guarda um {@link TokenBucket} por cliente (IP) e decide se cada requisição pode passar.
 * Os buckets ficam em memória, então os contadores zeram quando a aplicação reinicia.
 */
@Component
public class RateLimiter {

    private static final long PURGE_INTERVAL_NANOS = TimeUnit.MINUTES.toNanos(1);

    private final RateLimitProperties properties;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();
    private volatile long lastPurgeNanos = System.nanoTime();

    public RateLimiter(RateLimitProperties properties) {
        this.properties = properties;
    }

    /** Consome um token do bucket do cliente, criando o bucket (cheio) na primeira requisição dele. */
    public RateLimitResult tryConsume(String clientKey) {
        long now = System.nanoTime();
        TokenBucket bucket = buckets.computeIfAbsent(clientKey, key -> new TokenBucket(
                properties.capacity(), properties.refillTokens(), properties.refillPeriod(), now));
        RateLimitResult result = bucket.tryConsume(now);
        purgeFullBuckets(now);
        return result;
    }

    /**
     * No máximo uma vez por minuto, descarta os buckets que já encheram de novo.
     * Sem isso o mapa cresceria para sempre, com um bucket para cada IP que já chamou a API.
     */
    private void purgeFullBuckets(long now) {
        if (now - lastPurgeNanos < PURGE_INTERVAL_NANOS) {
            return;
        }
        lastPurgeNanos = now;
        buckets.values().removeIf(bucket -> bucket.isFull(now));
    }
}
