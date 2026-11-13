package com.vinicius.scene_api.ratelimit;

import java.time.Duration;

/**
 * Algoritmo token bucket para um cliente.
 * <p>
 * O bucket começa cheio. Cada requisição consome um token, e os tokens voltam aos poucos, numa taxa fixa,
 * até o limite da capacidade. Com o bucket vazio a requisição é recusada. Assim o cliente pode fazer
 * um burst do tamanho da capacidade, mas na média não passa da taxa de reposição.
 * <p>
 * O horário é sempre recebido por parâmetro (em nanossegundos), o que deixa a classe fácil de testar.
 */
public class TokenBucket {

    private static final double NANOS_PER_SECOND = 1_000_000_000.0;

    private final long capacity;
    private final double nanosPerToken;

    private double tokens;
    private long lastRefillNanos;

    public TokenBucket(long capacity, long refillTokens, Duration refillPeriod, long nowNanos) {
        this.capacity = capacity;
        this.nanosPerToken = (double) refillPeriod.toNanos() / refillTokens;
        this.tokens = capacity;
        this.lastRefillNanos = nowNanos;
    }

    /** Repõe os tokens do tempo que passou e tenta consumir um. */
    public synchronized RateLimitResult tryConsume(long nowNanos) {
        refill(nowNanos);
        if (tokens >= 1.0) {
            tokens -= 1.0;
            return new RateLimitResult(true, capacity, (long) tokens, secondsUntilFull(), 0);
        }
        long retryAfterSeconds = Math.max(1, toSecondsRoundingUp((1.0 - tokens) * nanosPerToken));
        return new RateLimitResult(false, capacity, 0, secondsUntilFull(), retryAfterSeconds);
    }

    /** Bucket cheio é igual a bucket novo: pode ser descartado sem mudar o resultado para o cliente. */
    public synchronized boolean isFull(long nowNanos) {
        refill(nowNanos);
        return tokens >= capacity;
    }

    private void refill(long nowNanos) {
        long elapsedNanos = Math.max(0, nowNanos - lastRefillNanos);
        tokens = Math.min(capacity, tokens + elapsedNanos / nanosPerToken);
        lastRefillNanos = nowNanos;
    }

    private long secondsUntilFull() {
        return toSecondsRoundingUp((capacity - tokens) * nanosPerToken);
    }

    /** Arredonda para cima, com uma folga mínima para um erro de ponto flutuante não virar um segundo a mais. */
    private static long toSecondsRoundingUp(double nanos) {
        return (long) Math.ceil(nanos / NANOS_PER_SECOND - 1e-9);
    }
}
