package com.vinicius.scene_api.config;

import com.vinicius.scene_api.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.DefaultCorsProcessor;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

/**
 * O processador de CORS do Spring responde a uma requisição não permitida com 403 e um texto solto
 * ("Invalid CORS request"). Esta versão mantém toda a checagem do Spring e só troca o corpo dessa
 * resposta pelo formato padrão de erro da API ({@link ApiError}), dizendo o que foi recusado.
 */
public class JsonCorsProcessor extends DefaultCorsProcessor {

    private final JsonMapper jsonMapper;

    public JsonCorsProcessor(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public boolean processRequest(CorsConfiguration config, HttpServletRequest request,
                                  HttpServletResponse response) throws IOException {
        boolean allowed = super.processRequest(config, request, response);
        if (!allowed) {
            writeForbidden(config, request, response);
        }
        return allowed;
    }

    /** O Spring chama este método ao recusar a requisição. Aqui fica só o status; o corpo vem em seguida. */
    @Override
    protected void rejectRequest(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.FORBIDDEN);
    }

    private void writeForbidden(CorsConfiguration config, HttpServletRequest request,
                                HttpServletResponse response) throws IOException {
        String message = "Requisição bloqueada pela política de CORS: " + reason(config, request);
        ApiError body = new ApiError(HttpStatus.FORBIDDEN.value(), HttpStatus.FORBIDDEN.getReasonPhrase(), message,
                request.getRequestURI(), LocalDateTime.now(), List.of());

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getOutputStream().write(jsonMapper.writeValueAsBytes(body));
        response.flushBuffer();
    }

    /** Descobre qual das três checagens do CORS falhou: a origem, o método ou os headers pedidos. */
    private static String reason(CorsConfiguration config, HttpServletRequest request) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        if (config.checkOrigin(origin) == null) {
            return "a origem '" + origin + "' não está na lista de origens permitidas";
        }
        // No preflight o método que interessa é o que o navegador pretende usar depois
        String method = isPreflight(request)
                ? request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD)
                : request.getMethod();
        if (!CorsConfig.ALLOWED_METHODS.contains(method)) {
            return "o método " + method + " não é aceito em requisições vindas de outra origem";
        }
        return "a requisição pede um header que não é aceito em requisições vindas de outra origem";
    }

    private static boolean isPreflight(HttpServletRequest request) {
        return HttpMethod.OPTIONS.matches(request.getMethod())
                && request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD) != null;
    }
}
