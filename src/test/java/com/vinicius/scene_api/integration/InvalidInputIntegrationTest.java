package com.vinicius.scene_api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Entrada errada do cliente tem que virar um erro 4xx claro, nunca 500 e nunca um dado gravado pela metade. */
class InvalidInputIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("Texto em campo numérico é recusado com 400 e a resposta aponta o campo")
    void textIsNotAcceptedAsNumber() {
        HttpResponse<String> response = post("/artists",
                "{\"name\": \"Banda Ano Texto\", \"genre\": \"ROCK\", \"formedYear\": \"2004\"}");

        assertEquals(400, response.statusCode());
        assertEquals("formedYear", firstInvalidField(response));
    }

    @Test
    @DisplayName("Número com casas decimais em campo inteiro é recusado com 400")
    void decimalIsNotAcceptedAsInteger() {
        HttpResponse<String> response = post("/artists",
                "{\"name\": \"Banda Ano Decimal\", \"genre\": \"ROCK\", \"formedYear\": 2004.7}");

        assertEquals(400, response.statusCode());
        assertEquals("formedYear", firstInvalidField(response));
    }

    @Test
    @DisplayName("Número ou booleano em campo de texto é recusado com 400")
    void numberIsNotAcceptedAsText() {
        HttpResponse<String> number = post("/artists", "{\"name\": 12345, \"genre\": \"ROCK\"}");
        HttpResponse<String> bool = post("/artists", "{\"name\": true, \"genre\": \"ROCK\"}");

        assertEquals(400, number.statusCode());
        assertEquals("name", firstInvalidField(number));
        assertEquals(400, bool.statusCode());
        assertEquals("name", firstInvalidField(bool));
    }

    @Test
    @DisplayName("Número no lugar de um enum é recusado com 400, em vez de virar o valor daquela posição")
    void numberIsNotAcceptedAsEnum() {
        HttpResponse<String> response = post("/artists", "{\"name\": \"Banda Enum Numero\", \"genre\": 1}");

        assertEquals(400, response.statusCode());
        assertEquals("genre", firstInvalidField(response));
    }

    @Test
    @DisplayName("POST sem corpo é recusado com 400")
    void missingBodyIsRejected() {
        HttpResponse<String> response = send("POST", "/artists", null, "Content-Type", "application/json");

        assertEquals(400, response.statusCode());
        assertTrue(json(response).get("message").asString().contains("ausente"));
    }

    @Test
    @DisplayName("Corpo que não é JSON recebe 415 Unsupported Media Type")
    void unsupportedContentTypeIsRejected() {
        HttpResponse<String> response = post("/artists", "name=Banda", "Content-Type", "text/plain");

        assertEquals(415, response.statusCode());
        assertEquals("Unsupported Media Type", json(response).get("error").asString());
    }

    @Test
    @DisplayName("Accept que não aceita JSON recebe 406 Not Acceptable antes de qualquer coisa ser gravada")
    void unacceptableAcceptHeaderIsRefusedBeforeExecuting() {
        HttpResponse<String> response = post("/artists",
                "{\"name\": \"Banda Somente XML\", \"genre\": \"ROCK\"}", "Accept", "application/xml");
        JsonNode search = json(get("/artists/search?name=Somente%20XML"));

        assertEquals(406, response.statusCode());
        assertEquals("Not Acceptable", json(response).get("error").asString());
        assertEquals(0, search.get("page").get("totalElements").asInt(), "o artista não pode ter sido criado");
    }

    @Test
    @DisplayName("Ordenar por um campo que não existe, ou com opção não suportada, dá 400")
    void invalidSortIsRejected() {
        HttpResponse<String> unknownField = get("/artists?sort=senha,asc");
        HttpResponse<String> unsupportedOption = get("/artists?sort=id,asc,ignorecase");

        assertEquals(400, unknownField.statusCode());
        assertTrue(json(unknownField).get("message").asString().contains("senha"));
        assertEquals(400, unsupportedOption.statusCode());
    }

    /** Nome do primeiro campo listado em "details" na resposta de erro. */
    private String firstInvalidField(HttpResponse<String> response) {
        return json(response).get("details").get(0).get("field").asString();
    }
}
