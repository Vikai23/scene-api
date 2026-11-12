package com.vinicius.scene_api.idempotency;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuração da idempotência, lida de {@code scene.idempotency.*} no application.properties.
 *
 * @param ttl por quanto tempo uma chave fica guardada depois de usada
 */
@ConfigurationProperties(prefix = "scene.idempotency")
public record IdempotencyProperties(Duration ttl) {

    public IdempotencyProperties {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("scene.idempotency: ttl precisa ser um intervalo positivo (ex.: 24h)");
        }
    }
}
