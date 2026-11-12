package com.vinicius.scene_api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Idempotência dos POST: com a mesma X-Idempotency-Key a operação roda uma vez só. */
class IdempotencyIntegrationTest extends IntegrationTestSupport {

    private static final String KEY_HEADER = "X-Idempotency-Key";
    private static final String REPLAYED_HEADER = "X-Idempotent-Replayed";

    @Test
    @DisplayName("Repetir o POST com a mesma chave devolve a resposta original e não cria outro registro")
    void sameKeyCreatesOnlyOnce() {
        String name = unique("Banda Idempotente");
        String body = artistJson(name);
        String key = newKey();

        HttpResponse<String> first = post("/artists", body, KEY_HEADER, key);
        HttpResponse<String> second = post("/artists", body, KEY_HEADER, key);

        assertEquals(201, first.statusCode());
        assertEquals("false", header(first, REPLAYED_HEADER));
        assertEquals(201, second.statusCode());
        assertEquals("true", header(second, REPLAYED_HEADER));
        assertEquals(json(first), json(second), "a segunda resposta é a mesma da primeira");
        assertEquals(header(first, "Location"), header(second, "Location"));
        assertEquals(1, countArtistsNamed(name), "só um artista foi criado");
    }

    @Test
    @DisplayName("A mesma chave com outro corpo é recusada com 422 Unprocessable Content")
    void sameKeyWithDifferentBodyIsRejected() {
        String key = newKey();
        post("/artists", artistJson(unique("Banda Original")), KEY_HEADER, key);

        HttpResponse<String> response = post("/artists", artistJson(unique("Banda Diferente")), KEY_HEADER, key);

        assertEquals(422, response.statusCode());
        assertEquals("Unprocessable Content", json(response).get("error").asString());
    }

    @Test
    @DisplayName("A mesma chave em outra rota também é recusada com 422")
    void sameKeyOnAnotherRouteIsRejected() {
        String key = newKey();
        post("/artists", artistJson(unique("Banda da Chave")), KEY_HEADER, key);

        HttpResponse<String> response = post("/playlists", playlistJson(unique("Playlist da Chave")), KEY_HEADER, key);

        assertEquals(422, response.statusCode());
    }

    @Test
    @DisplayName("Chave fora do formato aceito é recusada com 400 Bad Request")
    void invalidKeyIsRejected() {
        HttpResponse<String> response = post("/artists", artistJson(unique("Banda Chave Ruim")), KEY_HEADER, "chave com espaco!");

        assertEquals(400, response.statusCode());
        assertEquals("Bad Request", json(response).get("error").asString());
    }

    @Test
    @DisplayName("Se a operação falha, a chave não fica gasta: dá para corrigir e reenviar com a mesma chave")
    void failedRequestDoesNotUseUpTheKey() {
        String key = newKey();

        HttpResponse<String> invalid = post("/artists", "{\"name\": \"\", \"genre\": \"ROCK\"}", KEY_HEADER, key);
        HttpResponse<String> fixed = post("/artists", artistJson(unique("Banda Corrigida")), KEY_HEADER, key);

        assertEquals(400, invalid.statusCode());
        assertEquals(201, fixed.statusCode());
        assertEquals("false", header(fixed, REPLAYED_HEADER));
    }

    @Test
    @DisplayName("Sem a chave o POST continua como antes: o segundo igual dá 409 Conflict")
    void withoutKeyNothingChanges() {
        String body = artistJson(unique("Banda Sem Chave"));

        HttpResponse<String> first = post("/artists", body);
        HttpResponse<String> second = post("/artists", body);

        assertEquals(201, first.statusCode());
        assertNull(header(first, REPLAYED_HEADER));
        assertEquals(409, second.statusCode());
    }

    @Test
    @DisplayName("Vale também para o POST sem corpo que adiciona uma faixa à playlist")
    void worksWhenAddingTrackToPlaylist() {
        long playlistId = json(post("/playlists", playlistJson(unique("Playlist Idempotente")))).get("id").asLong();
        String route = "/playlists/" + playlistId + "/tracks/1";
        String key = newKey();

        HttpResponse<String> first = post(route, null, KEY_HEADER, key);
        HttpResponse<String> second = post(route, null, KEY_HEADER, key);
        HttpResponse<String> withoutKey = post(route, null);

        assertEquals(201, first.statusCode());
        assertEquals(201, second.statusCode());
        assertEquals("true", header(second, REPLAYED_HEADER));
        assertEquals(1, json(second).get("trackCount").asInt());
        assertEquals(409, withoutKey.statusCode(), "sem a chave, a faixa repetida é recusada");
    }

    private static String newKey() {
        return UUID.randomUUID().toString();
    }

    private static String artistJson(String name) {
        return "{\"name\": \"" + name + "\", \"genre\": \"ROCK\"}";
    }

    private static String playlistJson(String name) {
        return "{\"name\": \"" + name + "\", \"visibility\": \"PUBLIC\"}";
    }

    private int countArtistsNamed(String name) {
        String query = URLEncoder.encode(name, StandardCharsets.UTF_8);
        JsonNode page = json(get("/artists/search?name=" + query));
        return page.get("page").get("totalElements").asInt();
    }
}
