package com.vinicius.scene_api.config;

import com.vinicius.scene_api.idempotency.IdempotencyService;
import com.vinicius.scene_api.ratelimit.RateLimitInterceptor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;

/**
 * Política de CORS (Cross-Origin Resource Sharing).
 * <p>
 * Por padrão o navegador não deixa o JavaScript de um site ler respostas de outra origem (outro endereço ou
 * outra porta). Aqui a API declara quem pode: só as origens listadas em {@code scene.cors.allowed-origins},
 * com os métodos e headers abaixo. Qualquer outra origem recebe 403 Forbidden.
 * Chamadas sem o header Origin (Postman, curl, o próprio Swagger UI) não passam por essa checagem.
 */
@Configuration
public class CorsConfig {

    /** Métodos HTTP aceitos em requisições vindas de outra origem. */
    public static final List<String> ALLOWED_METHODS = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");

    /** Headers que o cliente de outra origem pode enviar. */
    public static final List<String> ALLOWED_HEADERS = List.of(
            HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT, ApiVersionConfig.HEADER, IdempotencyService.HEADER);

    /** Headers de resposta que o JavaScript do cliente consegue ler (os demais o navegador esconde). */
    public static final List<String> EXPOSED_HEADERS = List.of(
            HttpHeaders.LOCATION, HttpHeaders.RETRY_AFTER, ApiVersionConfig.HEADER, IdempotencyService.REPLAYED_HEADER,
            RateLimitInterceptor.LIMIT_HEADER, RateLimitInterceptor.REMAINING_HEADER, RateLimitInterceptor.RESET_HEADER);

    /** Por quanto tempo o navegador pode guardar a resposta do preflight (método OPTIONS). */
    public static final Duration PREFLIGHT_MAX_AGE = Duration.ofHours(1);

    /**
     * Registra o filtro de CORS como o primeiro da fila, para que a checagem aconteça antes de qualquer
     * outra coisa e os headers de CORS saiam em todas as respostas, inclusive nas de erro.
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter(CorsProperties properties, JsonMapper jsonMapper) {
        CorsConfiguration policy = new CorsConfiguration();
        policy.setAllowedOrigins(properties.allowedOrigins());
        policy.setAllowedMethods(ALLOWED_METHODS);
        policy.setAllowedHeaders(ALLOWED_HEADERS);
        policy.setExposedHeaders(EXPOSED_HEADERS);
        policy.setMaxAge(PREFLIGHT_MAX_AGE);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", policy);

        CorsFilter filter = new CorsFilter(source);
        filter.setCorsProcessor(new JsonCorsProcessor(jsonMapper));

        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
