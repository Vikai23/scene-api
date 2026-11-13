package com.vinicius.scene_api.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.accept.ApiVersionHolder;
import org.springframework.web.accept.SemanticApiVersionParser;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Devolve no header {@code X-API-Version} a versão que atendeu a requisição:
 * a pedida pelo cliente ou a padrão (1), quando ele não manda o header.
 */
@Component
public class ApiVersionHeaderInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // O Spring guarda nesse atributo a versão já lida e validada do header
        if (request.getAttribute(HandlerMapping.API_VERSION_ATTRIBUTE) instanceof ApiVersionHolder holder
                && holder.getVersionIfPresent() instanceof SemanticApiVersionParser.Version version) {
            response.setHeader(ApiVersionConfig.HEADER, String.valueOf(version.getMajor()));
        }
        return true;
    }
}
