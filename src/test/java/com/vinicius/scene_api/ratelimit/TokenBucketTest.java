package com.vinicius.scene_api.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testa o algoritmo do balde de fichas sozinho, sem subir o Spring. O horário é passado por parâmetro,
 * então dá para "avançar o relógio" sem esperar de verdade.
 */
class TokenBucketTest {

    private static final long SECOND = 1_000_000_000L;
    private static final long START = 5 * SECOND;

    /** Balde com 3 fichas, que recebe 1 ficha por segundo. */
    private static TokenBucket bucketOfThree() {
        return new TokenBucket(3, 1, Duration.ofSeconds(1), START);
    }

    @Test
    @DisplayName("Começa cheio e deixa passar uma rajada do tamanho da capacidade")
    void startsFullAndAllowsBurst() {
        TokenBucket bucket = bucketOfThree();

        RateLimitResult first = bucket.tryConsume(START);
        RateLimitResult second = bucket.tryConsume(START);
        RateLimitResult third = bucket.tryConsume(START);

        assertTrue(first.allowed());
        assertEquals(3, first.limit());
        assertEquals(2, first.remaining());
        assertEquals(1, second.remaining());
        assertTrue(third.allowed());
        assertEquals(0, third.remaining());
        assertEquals(0, third.retryAfterSeconds());
    }

    @Test
    @DisplayName("Sem fichas, recusa a requisição e informa quanto esperar")
    void rejectsWhenEmpty() {
        TokenBucket bucket = bucketOfThree();
        for (int i = 0; i < 3; i++) {
            bucket.tryConsume(START);
        }

        RateLimitResult rejected = bucket.tryConsume(START);

        assertFalse(rejected.allowed());
        assertEquals(0, rejected.remaining());
        assertEquals(1, rejected.retryAfterSeconds(), "falta 1 segundo para a próxima ficha");
        assertEquals(3, rejected.resetSeconds(), "faltam 3 segundos para o balde encher");
    }

    @Test
    @DisplayName("As fichas voltam com o tempo, na taxa configurada")
    void refillsOverTime() {
        TokenBucket bucket = bucketOfThree();
        for (int i = 0; i < 3; i++) {
            bucket.tryConsume(START);
        }

        assertFalse(bucket.tryConsume(START + SECOND / 2).allowed(), "meio segundo ainda não rende uma ficha");
        assertTrue(bucket.tryConsume(START + SECOND).allowed(), "depois de 1 segundo volta uma ficha");
        assertFalse(bucket.tryConsume(START + SECOND).allowed(), "e ela já foi gasta");
    }

    @Test
    @DisplayName("O balde nunca passa da capacidade, por mais tempo que fique parado")
    void neverExceedsCapacity() {
        TokenBucket bucket = bucketOfThree();
        long muchLater = START + 3600 * SECOND;

        for (int i = 0; i < 3; i++) {
            assertTrue(bucket.tryConsume(muchLater).allowed());
        }
        assertFalse(bucket.tryConsume(muchLater).allowed());
    }

    @Test
    @DisplayName("Sabe dizer quando voltou a ficar cheio")
    void knowsWhenItIsFullAgain() {
        TokenBucket bucket = bucketOfThree();
        assertTrue(bucket.isFull(START));

        bucket.tryConsume(START);

        assertFalse(bucket.isFull(START));
        assertTrue(bucket.isFull(START + SECOND));
    }

    @Test
    @DisplayName("Com 100 fichas por minuto, o balde vazio devolve uma ficha a cada 0,6 segundo")
    void supportsRatesPerMinute() {
        TokenBucket bucket = new TokenBucket(100, 100, Duration.ofMinutes(1), START);
        for (int i = 0; i < 100; i++) {
            assertTrue(bucket.tryConsume(START).allowed());
        }

        RateLimitResult rejected = bucket.tryConsume(START);

        assertFalse(rejected.allowed());
        assertEquals(1, rejected.retryAfterSeconds());
        assertEquals(60, rejected.resetSeconds());
        assertTrue(bucket.tryConsume(START + 600_000_000L).allowed());
    }
}
