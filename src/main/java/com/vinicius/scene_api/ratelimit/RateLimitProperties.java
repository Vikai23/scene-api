package com.vinicius.scene_api.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuração do rate limiting, lida de {@code scene.rate-limit.*} no application.properties.
 *
 * @param capacity     quantas fichas cabem no balde (tamanho máximo de uma rajada)
 * @param refillTokens quantas fichas voltam para o balde a cada {@code refillPeriod}
 * @param refillPeriod intervalo de reposição das fichas
 */
@ConfigurationProperties(prefix = "scene.rate-limit")
public record RateLimitProperties(int capacity, int refillTokens, Duration refillPeriod) {

    public RateLimitProperties {
        if (capacity < 1 || refillTokens < 1) {
            throw new IllegalArgumentException("scene.rate-limit: capacity e refill-tokens precisam ser pelo menos 1");
        }
        if (refillPeriod == null || refillPeriod.isZero() || refillPeriod.isNegative()) {
            throw new IllegalArgumentException("scene.rate-limit: refill-period precisa ser um intervalo positivo (ex.: 1m)");
        }
    }
}
