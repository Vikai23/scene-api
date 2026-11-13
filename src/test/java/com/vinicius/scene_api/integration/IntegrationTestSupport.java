package com.vinicius.scene_api.integration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base dos testes de integração: sobe a aplicação inteira em uma porta livre e faz requisições HTTP de
 * verdade, como o Postman faria. Assim os testes passam por tudo: filtro de CORS, interceptors,
 * controllers, services e banco.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        // banco só destes testes, para não misturar com o de outras classes
        "spring.datasource.url=jdbc:h2:mem:integration-tests",
        // limite alto: aqui o rate limiting não é o assunto (ele tem a própria classe de teste)
        "scene.rate-limit.capacity=100000",
        "scene.rate-limit.refill-tokens=100000"
})
abstract class IntegrationTestSupport {

    private static final AtomicInteger SEQUENCE = new AtomicInteger();

    protected static final String API_KEY_HEADER = "X-API-Key";

    private String apiKey;

    private final HttpClient client = HttpClient.newHttpClient();

    /** Porta em que a aplicação subiu neste teste. */
    @Value("${local.server.port}")
    private int port;

    @Autowired
    private JsonMapper jsonMapper;

    protected HttpResponse<String> get(String path, String... headers) {
        return send("GET", path, null, headers);
    }

    protected HttpResponse<String> post(String path, String json, String... headers) {
        return send("POST", path, json, headers);
    }

    /**
     * Envia uma requisição. Os headers vêm em pares: nome, valor, nome, valor...
     * Quando há corpo, o Content-Type é application/json, a não ser que o teste informe outro.
     * As rotas que alteram dados exigem uma chave de API: se o teste não mandar o header X-API-Key, vai a chave
     * gerada para os testes (ver {@link #apiKey()}). Para testar o 401, use {@link #sendRaw}.
     */
    protected HttpResponse<String> send(String method, String path, String json, String... headers) {
        return sendRaw(method, path, json, withApiKey(method, path, headers));
    }

    /** Envia a requisição exatamente com os headers informados, sem acrescentar a chave de API. */
    protected HttpResponse<String> sendRaw(String method, String path, String json, String... headers) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (json == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.method(method, HttpRequest.BodyPublishers.ofString(json));
            request.setHeader("Content-Type", "application/json");
        }
        for (int i = 0; i < headers.length; i += 2) {
            request.setHeader(headers[i], headers[i + 1]);
        }
        try {
            return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao chamar " + method + " " + path, ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Chamada interrompida: " + method + " " + path, ex);
        }
    }

    /** Chave de API dos testes, gerada em POST /api-keys na primeira vez que alguma escrita precisa dela. */
    protected String apiKey() {
        if (apiKey == null) {
            HttpResponse<String> created = sendRaw("POST", "/api-keys", "{\"owner\": \"Testes de integração\"}");
            apiKey = json(created).get("key").asString();
        }
        return apiKey;
    }

    private String[] withApiKey(String method, String path, String[] headers) {
        boolean write = List.of("POST", "PUT", "DELETE", "PATCH").contains(method);
        boolean keyCreation = method.equals("POST") && path.startsWith("/api-keys") && !path.startsWith("/api-keys/");
        for (int i = 0; i < headers.length; i += 2) {
            if (API_KEY_HEADER.equalsIgnoreCase(headers[i])) {
                return headers; // o teste já escolheu a chave
            }
        }
        if (!write || keyCreation) {
            return headers;
        }
        String[] withKey = Arrays.copyOf(headers, headers.length + 2);
        withKey[headers.length] = API_KEY_HEADER;
        withKey[headers.length + 1] = apiKey();
        return withKey;
    }

    /** Corpo da resposta já convertido de JSON, para o teste ler os campos. */
    protected JsonNode json(HttpResponse<String> response) {
        return jsonMapper.readTree(response.body());
    }

    /** Valor de um header da resposta, ou {@code null} se ele não veio. */
    protected String header(HttpResponse<String> response, String name) {
        return response.headers().firstValue(name).orElse(null);
    }

    /** Nome que não se repete entre os testes, para um cadastro não esbarrar em outro (ex.: "Banda 3 de teste"). */
    protected String unique(String prefix) {
        return prefix + " " + SEQUENCE.incrementAndGet() + " de teste";
    }
}
