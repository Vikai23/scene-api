package com.vinicius.scene_api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.JsonNode;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rate limiting de ponta a ponta. A aplicação sobe com um balde pequeno (3 fichas) que quase não repõe
 * (1 ficha por hora), então dá para estourar o limite em poucas chamadas e de forma previsível.
 */
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:rate-limit-tests",
        "scene.rate-limit.capacity=3",
        "scene.rate-limit.refill-tokens=1",
        "scene.rate-limit.refill-period=1h"
})
class RateLimitIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("Depois de gastar as fichas do balde, a API responde 429 Too Many Requests com Retry-After")
    void blocksWhenTheBucketIsEmpty() {
        HttpResponse<String> first = get("/artists");
        HttpResponse<String> second = get("/artists");
        HttpResponse<String> third = get("/artists");
        HttpResponse<String> fourth = get("/artists");

        assertEquals(200, first.statusCode());
        assertEquals("3", header(first, "X-RateLimit-Limit"));
        assertEquals("2", header(first, "X-RateLimit-Remaining"));
        assertEquals("1", header(second, "X-RateLimit-Remaining"));
        assertEquals(200, third.statusCode());
        assertEquals("0", header(third, "X-RateLimit-Remaining"));

        JsonNode error = json(fourth);
        assertEquals(429, fourth.statusCode());
        assertEquals("Too Many Requests", error.get("error").asString());
        assertEquals("0", header(fourth, "X-RateLimit-Remaining"));
        assertTrue(Long.parseLong(header(fourth, "Retry-After")) > 0, "Retry-After diz quantos segundos esperar");
    }

    @Test
    @DisplayName("A documentação (Swagger) fica fora do limite")
    void documentationIsNotRateLimited() {
        for (int i = 0; i < 5; i++) {
            assertEquals(200, get("/v3/api-docs").statusCode());
        }
    }
}
