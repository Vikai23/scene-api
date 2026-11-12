package com.vinicius.scene_api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuração do CORS, lida de {@code scene.cors.*} no application.properties.
 *
 * @param allowedOrigins origens (esquema + host + porta) que podem chamar a API a partir do navegador
 */
@ConfigurationProperties(prefix = "scene.cors")
public record CorsProperties(List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }
}
