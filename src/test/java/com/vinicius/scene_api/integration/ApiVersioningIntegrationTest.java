package com.vinicius.scene_api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Versionamento por cabeçalho: GET /artists/{id} tem duas versões, escolhidas por X-API-Version. */
class ApiVersioningIntegrationTest extends IntegrationTestSupport {

    private static final String VERSION_HEADER = "X-API-Version";

    @Test
    @DisplayName("Sem o cabeçalho vale a versão 1: país e ano soltos no artista")
    void defaultsToVersionOne() {
        HttpResponse<String> response = get("/artists/1");
        JsonNode artist = json(response);

        assertEquals(200, response.statusCode());
        assertEquals("1", header(response, VERSION_HEADER));
        assertTrue(artist.has("country"));
        assertTrue(artist.has("formedYear"));
        assertFalse(artist.has("origin"));
    }

    @Test
    @DisplayName("A versão 2 agrupa a origem e traz a contagem de álbuns e se há perfil")
    void versionTwoChangesTheRepresentation() {
        HttpResponse<String> response = get("/artists/1", VERSION_HEADER, "2");
        JsonNode artist = json(response);

        assertEquals(200, response.statusCode());
        assertEquals("2", header(response, VERSION_HEADER));
        assertFalse(artist.has("country"), "na versão 2 o país fica dentro de origin");
        assertTrue(artist.get("origin").has("country"));
        assertTrue(artist.get("origin").has("formedYear"));
        assertTrue(artist.get("albumCount").isNumber());
        assertTrue(artist.get("hasProfile").isBoolean());
        assertTrue(artist.get("_links").has("self"), "os links HATEOAS continuam na versão 2");
    }

    @Test
    @DisplayName("Versão que não existe é recusada com 400 Bad Request")
    void rejectsUnsupportedVersion() {
        HttpResponse<String> response = get("/artists/1", VERSION_HEADER, "9");
        JsonNode error = json(response);

        assertEquals(400, response.statusCode());
        assertEquals("Bad Request", error.get("error").asString());
        assertTrue(error.get("message").asString().contains(VERSION_HEADER));
    }

    @Test
    @DisplayName("As rotas que só têm uma versão respondem igual para 1 e 2")
    void routesWithASingleVersionAcceptBoth() {
        HttpResponse<String> versionOne = get("/albums/1", VERSION_HEADER, "1");
        HttpResponse<String> versionTwo = get("/albums/1", VERSION_HEADER, "2");

        assertEquals(200, versionOne.statusCode());
        assertEquals(200, versionTwo.statusCode());
        assertEquals("2", header(versionTwo, VERSION_HEADER));
        assertEquals(json(versionOne), json(versionTwo));
    }
}
