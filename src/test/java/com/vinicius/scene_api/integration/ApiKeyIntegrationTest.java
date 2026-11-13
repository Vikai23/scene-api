package com.vinicius.scene_api.integration;

import com.vinicius.scene_api.apikey.ApiKeyService;
import com.vinicius.scene_api.apikey.RequiresApiKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import tools.jackson.databind.JsonNode;

import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Autenticação com chave de API (header X-API-Key): as rotas que alteram dados exigem uma chave válida, as
 * consultas continuam abertas, e a chave pode ser gerada, consultada e revogada.
 */
class ApiKeyIntegrationTest extends IntegrationTestSupport {

    private static final String ARTIST = "{\"name\": \"%s\", \"genre\": \"EMO\"}";

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    @DisplayName("Escrita sem chave devolve 401 com WWW-Authenticate, e nada é gravado")
    void writeWithoutKeyIsRejected() {
        String name = unique("Banda sem chave");
        HttpResponse<String> response = sendRaw("POST", "/artists", ARTIST.formatted(name));

        assertEquals(401, response.statusCode());
        assertEquals("Unauthorized", json(response).get("error").asString());
        assertEquals(ApiKeyService.MISSING_MESSAGE, json(response).get("message").asString());
        assertEquals(ApiKeyService.CHALLENGE, header(response, "WWW-Authenticate"));

        HttpResponse<String> search = get("/artists/search?name=" + URLEncoder.encode(name, StandardCharsets.UTF_8));
        assertEquals(0, json(search).get("page").get("totalElements").asInt(), "o artista não pode ter sido criado");
    }

    @Test
    @DisplayName("Chave que não existe devolve 401 com mensagem própria")
    void unknownKeyIsRejected() {
        HttpResponse<String> response = sendRaw("DELETE", "/artists/1", null, API_KEY_HEADER, "scn_chave-que-nao-existe");

        assertEquals(401, response.statusCode());
        assertEquals(ApiKeyService.UNKNOWN_MESSAGE, json(response).get("message").asString());
    }

    @Test
    @DisplayName("Consultas (GET) continuam abertas, sem chave")
    void readsDoNotNeedKey() {
        assertEquals(200, sendRaw("GET", "/artists?size=1", null).statusCode());
        assertEquals(200, sendRaw("GET", "/artists/1", null).statusCode());
    }

    @Test
    @DisplayName("Ciclo da chave: gerar (a chave aparece uma vez), usar, consultar e revogar")
    void keyLifecycle() {
        HttpResponse<String> created = sendRaw("POST", "/api-keys", "{\"owner\": \"Vinícius\"}");
        assertEquals(201, created.statusCode());
        JsonNode body = json(created);
        String key = body.get("key").asString();
        assertTrue(key.startsWith("scn_") && key.length() == 36, "formato scn_ + 32 caracteres: " + key);
        assertTrue(key.startsWith(body.get("prefix").asString()), "o prefixo é o começo da chave");
        assertTrue(header(created, "Location").endsWith("/api-keys/me"));

        HttpResponse<String> artist = sendRaw("POST", "/artists", ARTIST.formatted(unique("Banda com chave")),
                API_KEY_HEADER, key);
        assertEquals(201, artist.statusCode(), "com a chave, a escrita passa");

        HttpResponse<String> me = sendRaw("GET", "/api-keys/me", null, API_KEY_HEADER, key);
        assertEquals(200, me.statusCode());
        assertEquals("Vinícius", json(me).get("owner").asString());
        assertTrue(json(me).get("active").asBoolean());
        assertFalse(json(me).has("key"), "depois de gerada, a chave completa não aparece mais");

        assertEquals(204, sendRaw("DELETE", "/api-keys/me", null, API_KEY_HEADER, key).statusCode());
        HttpResponse<String> afterRevoke = sendRaw("POST", "/artists", ARTIST.formatted(unique("Banda depois")),
                API_KEY_HEADER, key);
        assertEquals(401, afterRevoke.statusCode());
        assertEquals(ApiKeyService.REVOKED_MESSAGE, json(afterRevoke).get("message").asString());
    }

    @Test
    @DisplayName("Gerar chave com dono inválido devolve 400 apontando o campo owner")
    void invalidOwnerIsRejected() {
        HttpResponse<String> response = sendRaw("POST", "/api-keys", "{\"owner\": \"x\"}");

        assertEquals(400, response.statusCode());
        assertEquals("owner", json(response).get("details").get(0).get("field").asString());
    }

    @Test
    @DisplayName("Toda rota que altera dados exige a chave (só POST /api-keys fica aberta)")
    void everyWriteRouteRequiresKey() {
        List<String> unprotected = new ArrayList<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMapping.getHandlerMethods().entrySet()) {
            HandlerMethod handler = entry.getValue();
            if (!handler.getBeanType().getPackageName().startsWith("com.vinicius.scene_api")) {
                continue;
            }
            Set<RequestMethod> methods = entry.getKey().getMethodsCondition().getMethods();
            boolean write = methods.stream().anyMatch(method -> method != RequestMethod.GET);
            Set<String> paths = entry.getKey().getPatternValues();
            boolean keyCreation = methods.contains(RequestMethod.POST) && paths.contains("/api-keys");
            if (write && !keyCreation && !handler.hasMethodAnnotation(RequiresApiKey.class)) {
                unprotected.add(methods + " " + paths);
            }
        }
        assertTrue(unprotected.isEmpty(), "rotas de escrita sem @RequiresApiKey: " + unprotected);
    }
}
