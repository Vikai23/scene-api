package com.vinicius.scene_api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CORS: só as origens de scene.cors.allowed-origins podem chamar a API pelo navegador.
 * O navegador avisa de onde vem a página no header Origin; aqui o teste manda esse header à mão.
 */
class CorsIntegrationTest extends IntegrationTestSupport {

    private static final String ALLOWED_ORIGIN = "http://localhost:4200";
    private static final String UNKNOWN_ORIGIN = "http://site-nao-autorizado.com";

    @Test
    @DisplayName("Origem permitida recebe os headers de CORS na resposta")
    void allowedOriginGetsCorsHeaders() {
        HttpResponse<String> response = get("/artists", "Origin", ALLOWED_ORIGIN);

        assertEquals(200, response.statusCode());
        assertEquals(ALLOWED_ORIGIN, header(response, "Access-Control-Allow-Origin"));
        assertTrue(header(response, "Access-Control-Expose-Headers").contains("X-RateLimit-Remaining"),
                "o navegador só deixa o JavaScript ler os headers expostos aqui");
    }

    @Test
    @DisplayName("Origem fora da lista é bloqueada com 403 Forbidden no formato padrão de erro")
    void unknownOriginIsBlocked() {
        HttpResponse<String> response = get("/artists", "Origin", UNKNOWN_ORIGIN);
        JsonNode error = json(response);

        assertEquals(403, response.statusCode());
        assertEquals("Forbidden", error.get("error").asString());
        assertTrue(error.get("message").asString().contains(UNKNOWN_ORIGIN));
        assertNull(header(response, "Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("O preflight informa os métodos e headers liberados")
    void preflightAnnouncesMethodsAndHeaders() {
        HttpResponse<String> response = send("OPTIONS", "/artists", null,
                "Origin", ALLOWED_ORIGIN,
                "Access-Control-Request-Method", "POST",
                "Access-Control-Request-Headers", "content-type, x-idempotency-key, x-api-key");

        assertEquals(200, response.statusCode());
        assertEquals(ALLOWED_ORIGIN, header(response, "Access-Control-Allow-Origin"));
        assertEquals("GET,POST,PUT,DELETE,OPTIONS", header(response, "Access-Control-Allow-Methods"));
        assertTrue(header(response, "Access-Control-Allow-Headers").toLowerCase().contains("x-idempotency-key"));
        assertTrue(header(response, "Access-Control-Allow-Headers").toLowerCase().contains("x-api-key"));
    }

    @Test
    @DisplayName("Preflight pedindo um método que a API não libera é bloqueado com 403")
    void preflightWithUnlistedMethodIsBlocked() {
        HttpResponse<String> response = send("OPTIONS", "/artists", null,
                "Origin", ALLOWED_ORIGIN,
                "Access-Control-Request-Method", "PATCH");

        assertEquals(403, response.statusCode());
        assertTrue(json(response).get("message").asString().contains("PATCH"));
    }

    @Test
    @DisplayName("Requisição sem Origin (Postman, curl, o próprio Swagger UI) não passa pela checagem de CORS")
    void requestsWithoutOriginAreNotChecked() {
        HttpResponse<String> response = get("/artists");

        assertEquals(200, response.statusCode());
        assertNull(header(response, "Access-Control-Allow-Origin"));
    }
}
