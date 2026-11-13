package com.vinicius.scene_api.ratelimit;

/**
 * Resultado de uma tentativa de consumir um token do bucket.
 *
 * @param allowed           {@code true} se havia token e a requisição pode seguir
 * @param limit             capacidade do bucket
 * @param remaining         tokens que sobraram depois desta requisição
 * @param resetSeconds      segundos até o bucket ficar cheio de novo
 * @param retryAfterSeconds segundos até ter pelo menos um token (0 quando a requisição foi aceita)
 */
public record RateLimitResult(boolean allowed, long limit, long remaining, long resetSeconds, long retryAfterSeconds) {
}
