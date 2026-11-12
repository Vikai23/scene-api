package com.vinicius.scene_api.ratelimit;

/**
 * Resultado de uma tentativa de gastar uma ficha do balde.
 *
 * @param allowed           {@code true} se havia ficha e a requisição pode seguir
 * @param limit             capacidade do balde
 * @param remaining         fichas que sobraram depois desta requisição
 * @param resetSeconds      segundos até o balde ficar cheio de novo
 * @param retryAfterSeconds segundos até ter pelo menos uma ficha (0 quando a requisição foi aceita)
 */
public record RateLimitResult(boolean allowed, long limit, long remaining, long resetSeconds, long retryAfterSeconds) {
}
