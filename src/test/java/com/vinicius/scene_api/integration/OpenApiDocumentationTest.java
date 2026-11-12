package com.vinicius.scene_api.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Confere a documentação gerada (o JSON que o Swagger UI lê em /v3/api-docs): toda rota tem que listar
 * os erros que podem acontecer nela. Se alguém criar uma rota nova e esquecer um erro, este teste avisa.
 */
class OpenApiDocumentationTest extends IntegrationTestSupport {

    private static final List<String> HTTP_METHODS = List.of("get", "post", "put", "delete");

    @Test
    @DisplayName("Toda rota documenta os erros comuns: 400, 403, 406 e 429")
    void everyOperationDocumentsTheCommonErrors() {
        for (Operation operation : operations()) {
            for (String status : List.of("400", "403", "406", "429")) {
                assertTrue(operation.responses().has(status), operation.name() + " deveria documentar " + status);
            }
        }
    }

    @Test
    @DisplayName("Nenhuma rota lista 500: entrada errada do cliente sempre vira 4xx")
    void noOperationDocumentsInternalServerError() {
        for (Operation operation : operations()) {
            assertFalse(operation.responses().has("500"), operation.name() + " não deveria documentar 500");
        }
    }

    @Test
    @DisplayName("Toda rota que recebe corpo documenta 415")
    void operationsWithBodyDocumentUnsupportedMediaType() {
        for (Operation operation : operations()) {
            if (operation.details().has("requestBody")) {
                assertTrue(operation.responses().has("415"), operation.name() + " deveria documentar 415");
            }
        }
    }

    @Test
    @DisplayName("Todo POST documenta a chave de idempotência e os erros 409 e 422")
    void postOperationsDocumentIdempotency() {
        for (Operation operation : operations()) {
            if (operation.name().startsWith("POST")) {
                assertTrue(hasHeaderParameter(operation, "X-Idempotency-Key"), operation.name() + " deveria ter X-Idempotency-Key");
                assertTrue(operation.responses().has("409"), operation.name() + " deveria documentar 409");
                assertTrue(operation.responses().has("422"), operation.name() + " deveria documentar 422");
            }
        }
    }

    @Test
    @DisplayName("Toda rota aceita o cabeçalho X-API-Version, e GET /artists/{id} mostra as duas versões")
    void versioningIsDocumented() {
        for (Operation operation : operations()) {
            assertTrue(hasHeaderParameter(operation, "X-API-Version"), operation.name() + " deveria ter X-API-Version");
        }
        JsonNode examples = apiDocs().get("paths").get("/artists/{id}").get("get")
                .get("responses").get("200").get("content").get("application/json").get("examples");
        assertEquals(2, examples.size(), "um exemplo de resposta para cada versão");
    }

    @Test
    @DisplayName("O cabeçalho da documentação traz termos de uso, contato, licença e link do repositório")
    void infoHasTermsContactLicenseAndExternalDocs() {
        JsonNode docs = apiDocs();
        JsonNode info = docs.get("info");

        assertEquals("/termos-de-uso.html", info.get("termsOfService").asString());
        assertTrue(info.get("contact").has("url"));
        assertEquals("Apache 2.0", info.get("license").get("name").asString());
        assertTrue(docs.get("externalDocs").has("url"));
        assertEquals(200, get("/termos-de-uso.html").statusCode(), "a página de termos de uso tem que existir");
    }

    private JsonNode apiDocs() {
        return json(get("/v3/api-docs"));
    }

    /** Todas as operações da documentação, cada uma com um nome como "GET /artists/{id}". */
    private List<Operation> operations() {
        List<Operation> operations = new ArrayList<>();
        for (Map.Entry<String, JsonNode> path : apiDocs().get("paths").properties()) {
            for (String method : HTTP_METHODS) {
                if (path.getValue().has(method)) {
                    operations.add(new Operation(method.toUpperCase() + " " + path.getKey(), path.getValue().get(method)));
                }
            }
        }
        assertFalse(operations.isEmpty(), "a documentação deveria ter operações");
        return operations;
    }

    private static boolean hasHeaderParameter(Operation operation, String name) {
        for (JsonNode parameter : operation.details().path("parameters")) {
            if ("header".equals(parameter.get("in").asString()) && name.equals(parameter.get("name").asString())) {
                return true;
            }
        }
        return false;
    }

    private record Operation(String name, JsonNode details) {

        JsonNode responses() {
            return details.get("responses");
        }
    }
}
