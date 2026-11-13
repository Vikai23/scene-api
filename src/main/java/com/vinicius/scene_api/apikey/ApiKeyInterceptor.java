package com.vinicius.scene_api.apikey;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Protege as rotas marcadas com {@link RequiresApiKey}: antes de o controller rodar, confere o header
 * {@code X-API-Key}. Sem uma chave válida, a requisição para aqui com 401 Unauthorized (ver ApiKeyService),
 * antes de validar o corpo ou gravar qualquer coisa.
 * <p>
 * As outras rotas passam direto. O preflight do CORS (OPTIONS) nem chega aqui: o filtro de CORS responde antes.
 */
@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    private final ApiKeyService apiKeyService;

    public ApiKeyInterceptor(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (handler instanceof HandlerMethod method && method.hasMethodAnnotation(RequiresApiKey.class)) {
            ApiKey key = apiKeyService.authenticate(request.getHeader(ApiKeyService.HEADER));
            request.setAttribute(ApiKeyService.REQUEST_ATTRIBUTE, key);
        }
        return true;
    }
}
