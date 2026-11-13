package com.vinicius.scene_api.config;

import com.vinicius.scene_api.apikey.ApiKeyInterceptor;
import com.vinicius.scene_api.ratelimit.RateLimitInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** Rotas da API. Swagger UI, console do H2 e arquivos estáticos ficam de fora dos interceptors. */
    private static final String[] API_PATHS = {
            "/artists", "/artists/**",
            "/artist-profiles", "/artist-profiles/**",
            "/albums", "/albums/**",
            "/tracks", "/tracks/**",
            "/playlists", "/playlists/**",
            "/api-keys", "/api-keys/**"
    };

    private final RateLimitInterceptor rateLimitInterceptor;
    private final ApiKeyInterceptor apiKeyInterceptor;
    private final AcceptHeaderInterceptor acceptHeaderInterceptor;
    private final ApiVersionHeaderInterceptor apiVersionHeaderInterceptor;

    public WebConfig(RateLimitInterceptor rateLimitInterceptor,
                     ApiKeyInterceptor apiKeyInterceptor,
                     AcceptHeaderInterceptor acceptHeaderInterceptor,
                     ApiVersionHeaderInterceptor apiVersionHeaderInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.apiKeyInterceptor = apiKeyInterceptor;
        this.acceptHeaderInterceptor = acceptHeaderInterceptor;
        this.apiVersionHeaderInterceptor = apiVersionHeaderInterceptor;
    }

    /** Abrir http://localhost:8080/ já leva direto para a documentação (Swagger UI). */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/", "/swagger-ui.html");
    }

    /**
     * Interceptors que rodam antes dos controllers, nesta ordem: o rate limiting (requisição barrada nem
     * chega ao controller, e toda tentativa gasta um token, até as sem chave), a chave de API (401 nas rotas
     * protegidas), a checagem do header Accept (406 antes de executar qualquer coisa) e, por último, o que
     * devolve a versão da API usada.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor).addPathPatterns(API_PATHS);
        registry.addInterceptor(apiKeyInterceptor).addPathPatterns(API_PATHS);
        registry.addInterceptor(acceptHeaderInterceptor).addPathPatterns(API_PATHS);
        registry.addInterceptor(apiVersionHeaderInterceptor).addPathPatterns(API_PATHS);
    }
}
