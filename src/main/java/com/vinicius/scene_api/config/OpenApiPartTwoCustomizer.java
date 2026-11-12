package com.vinicius.scene_api.config;

import com.vinicius.scene_api.dto.ErrorExamples;
import com.vinicius.scene_api.idempotency.IdempotencyService;
import com.vinicius.scene_api.ratelimit.RateLimitInterceptor;
import com.vinicius.scene_api.ratelimit.RateLimitProperties;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Documenta no Swagger os recursos da Parte 2 que valem para várias rotas de uma vez: versionamento,
 * CORS, rate limiting e idempotência. Em vez de repetir as mesmas anotações nos 40 métodos dos controllers,
 * este customizador percorre as operações já geradas pelo springdoc e acrescenta em cada uma os cabeçalhos
 * e os erros que podem acontecer ali (400 de versão inválida, 403, 406, 415, 422 e 429).
 */
@Component
public class OpenApiPartTwoCustomizer implements OpenApiCustomizer {

    private static final String JSON = "application/json";
    private static final String API_ERROR_SCHEMA = "#/components/schemas/ApiError";
    private static final String HEADER_PARAMETER = "header";
    private static final String QUERY_PARAMETER = "query";

    /** Rota que tem duas versões (ver ArtistController). */
    private static final String VERSIONED_PATH = "/artists/{id}";

    private static final String ARTIST_V1_SCHEMA = "EntityModelArtistResponse";
    private static final String ARTIST_V2_SCHEMA = "EntityModelArtistResponseV2";

    private static final String ARTIST_LINKS = """
            "_links": {"self": {"href": "http://localhost:8080/artists/1"}, "update": {"href": "http://localhost:8080/artists/1"}, "delete": {"href": "http://localhost:8080/artists/1"}, "artists": {"href": "http://localhost:8080/artists"}, "albums": {"href": "http://localhost:8080/albums/by-artist/1"}, "profile": {"href": "http://localhost:8080/artist-profiles/by-artist/1"}}""";

    private static final String ARTIST_V1_EXAMPLE = "{" + ARTIST_LINKS + """
            , "id": 1, "name": "Bring Me the Horizon", "genre": "METALCORE", "country": "United Kingdom", "formedYear": 2004}""";

    private static final String ARTIST_V2_EXAMPLE = "{" + ARTIST_LINKS + """
            , "id": 1, "name": "Bring Me the Horizon", "genre": "METALCORE", "origin": {"country": "United Kingdom", "formedYear": 2004}, "albumCount": 1, "hasProfile": true}""";

    private final JsonMapper jsonMapper;
    private final RateLimitProperties rateLimit;

    public OpenApiPartTwoCustomizer(JsonMapper jsonMapper, RateLimitProperties rateLimit) {
        this.jsonMapper = jsonMapper;
        this.rateLimit = rateLimit;
    }

    @Override
    public void customise(OpenAPI openApi) {
        if (openApi.getPaths() == null) {
            return;
        }
        openApi.getPaths().forEach((path, pathItem) ->
                pathItem.readOperationsMap().forEach((method, operation) -> {
                    boolean versioned = VERSIONED_PATH.equals(path) && method == PathItem.HttpMethod.GET;
                    boolean hasBody = operation.getRequestBody() != null;

                    // 400: cada causa possível entra na descrição e ganha um exemplo
                    if (hasIntegerPathParameter(operation)) {
                        documentInvalidId(operation);
                    }
                    if (findParameter(operation, QUERY_PARAMETER, "sort") != null) {
                        documentPagingErrors(operation);
                    }
                    if (hasBody) {
                        documentBodyErrors(operation);
                    }
                    if (findParameter(operation, HEADER_PARAMETER, IdempotencyService.HEADER) != null) {
                        documentIdempotency(operation);
                    }
                    documentApiVersion(operation, versioned);

                    // demais status que valem para várias rotas
                    documentCors(operation);
                    documentNotAcceptable(operation);
                    if (hasBody) {
                        documentUnsupportedMediaType(operation);
                    }
                    documentRateLimit(operation);
                    if (versioned) {
                        documentArtistVersions(openApi, operation);
                    }
                    sortResponsesByStatus(operation);
                }));
    }

    // ---------- Id na rota ----------

    /** Toda rota com id numérico no caminho devolve 400 se vier outra coisa no lugar (ex.: /artists/abc). */
    private void documentInvalidId(Operation operation) {
        ApiResponse badRequest = operation.getResponses().get("400");
        if (badRequest != null) {
            addExampleIfAbsent(badRequest, "Id inválido", ErrorExamples.BAD_REQUEST_ID);
        }
    }

    // ---------- Paginação ----------

    /** Toda rota paginada pode devolver 400 por causa de sort (campo que não existe) ou de page (grande demais). */
    private void documentPagingErrors(Operation operation) {
        ApiResponse badRequest = operation.getResponses().get("400");
        if (badRequest == null) {
            return;
        }
        boolean mentionsSort = badRequest.getDescription().contains("ordenação");
        badRequest.setDescription(badRequest.getDescription()
                + (mentionsSort ? "" : "; campo de ordenação inexistente")
                + "; número de página grande demais para o tamanho informado");
        if (!mentionsSort) {
            addExample(badRequest, "Ordenação inválida", ErrorExamples.BAD_REQUEST_SORT);
        }
        addExample(badRequest, "Página grande demais", ErrorExamples.BAD_REQUEST_PAGE_TOO_LARGE);
    }

    // ---------- Corpo da requisição ----------

    /** Toda rota que recebe JSON pode devolver 400 por JSON malformado, campo com tipo errado ou corpo ausente. */
    private void documentBodyErrors(Operation operation) {
        ApiResponse badRequest = operation.getResponses().get("400");
        if (badRequest == null) {
            return;
        }
        badRequest.setDescription(badRequest.getDescription()
                + "; campo com valor de tipo errado (ex.: texto onde se espera número); corpo ausente");
        addExampleIfAbsent(badRequest, "JSON malformado", ErrorExamples.BAD_REQUEST_MALFORMED);
        addExample(badRequest, "Valor de tipo errado", ErrorExamples.BAD_REQUEST_WRONG_TYPE);
        addExample(badRequest, "Corpo ausente", ErrorExamples.BAD_REQUEST_BODY_MISSING);
    }

    // ---------- Versionamento ----------

    /** Cabeçalho X-API-Version em todas as rotas: qualquer uma devolve 400 se a versão pedida não existir. */
    private void documentApiVersion(Operation operation, boolean versioned) {
        String effect = versioned
                ? "Nesta rota a resposta muda: a versão 1 devolve `country` e `formedYear` soltos; a versão 2 devolve o objeto "
                + "`origin` e os campos `albumCount` e `hasProfile`."
                : "Nesta rota as duas versões respondem igual.";
        Parameter header = findParameter(operation, HEADER_PARAMETER, ApiVersionConfig.HEADER);
        if (header == null) {
            header = new Parameter().in(HEADER_PARAMETER).name(ApiVersionConfig.HEADER);
            operation.addParametersItem(header);
        }
        header.required(false)
                .description("Versão da API que deve responder: " + ApiVersionConfig.V1 + " (padrão, usada quando o cabeçalho não é "
                        + "enviado) ou " + ApiVersionConfig.V2 + ". " + effect + " Qualquer outro valor devolve 400 Bad Request.")
                .schema(new StringSchema()._default(ApiVersionConfig.V1))
                .example(null);

        ApiResponse badRequest = operation.getResponses().get("400");
        if (badRequest == null) {
            badRequest = errorResponse("Bad Request – versão da API não suportada (cabeçalho " + ApiVersionConfig.HEADER + ")");
            operation.getResponses().addApiResponse("400", badRequest);
        } else {
            badRequest.setDescription(badRequest.getDescription() + "; versão da API não suportada (cabeçalho "
                    + ApiVersionConfig.HEADER + ")");
        }
        addExample(badRequest, "Versão da API não suportada", ErrorExamples.BAD_REQUEST_API_VERSION);

        successResponses(operation).forEach(response -> response.addHeaderObject(ApiVersionConfig.HEADER, new Header()
                .description("Versão da API que atendeu a requisição")
                .schema(new StringSchema().example(ApiVersionConfig.V1))));
    }

    /** GET /artists/{id}: uma operação só no Swagger, com a resposta 200 mostrando os dois formatos. */
    private void documentArtistVersions(OpenAPI openApi, Operation operation) {
        operation.setSummary("Busca um artista pelo id (versões 1 e 2)");
        operation.setDescription("Retorna os dados de um único artista a partir do seu identificador, com links para álbuns e perfil. "
                + "É a rota que demonstra o versionamento por cabeçalho: com `" + ApiVersionConfig.HEADER + ": 1` (ou sem o cabeçalho) "
                + "a resposta é a original; com `" + ApiVersionConfig.HEADER + ": 2`, `country` e `formedYear` passam para o objeto "
                + "`origin` e entram `albumCount` (quantidade de álbuns) e `hasProfile` (se o artista já tem perfil).");

        ApiResponse ok = operation.getResponses().get("200");
        if (ok == null) {
            return;
        }
        ok.setDescription("OK – artista encontrado. O formato do corpo depende da versão pedida em " + ApiVersionConfig.HEADER);

        Map<String, Schema> schemas = openApi.getComponents() == null ? Map.of() : openApi.getComponents().getSchemas();
        ComposedSchema eitherVersion = new ComposedSchema();
        for (String name : List.of(ARTIST_V1_SCHEMA, ARTIST_V2_SCHEMA)) {
            if (schemas != null && schemas.containsKey(name)) {
                eitherVersion.addOneOfItem(new Schema<>().$ref("#/components/schemas/" + name));
            }
        }
        MediaType body = new MediaType()
                .addExamples("Versão 1 (padrão)", new Example().value(parse(ARTIST_V1_EXAMPLE)))
                .addExamples("Versão 2", new Example().value(parse(ARTIST_V2_EXAMPLE)));
        if (eitherVersion.getOneOf() != null) {
            body.schema(eitherVersion);
        }
        ok.setContent(new Content().addMediaType(JSON, body));
    }

    // ---------- CORS ----------

    private void documentCors(Operation operation) {
        ApiResponse forbidden = errorResponse("Forbidden – requisição bloqueada pelo CORS: veio de outra origem (cabeçalho Origin) "
                + "que não está na lista de origens permitidas");
        addExample(forbidden, "Origem não permitida", ErrorExamples.FORBIDDEN_CORS);
        operation.getResponses().addApiResponse("403", forbidden);
    }

    // ---------- Negociação de conteúdo ----------

    private void documentNotAcceptable(Operation operation) {
        ApiResponse notAcceptable = errorResponse("Not Acceptable – o cabeçalho Accept só aceita um formato que a API não produz "
                + "(as respostas são sempre JSON). A requisição é recusada antes de ser executada");
        addExample(notAcceptable, "Formato não suportado", ErrorExamples.NOT_ACCEPTABLE);
        operation.getResponses().addApiResponse("406", notAcceptable);
    }

    private void documentUnsupportedMediaType(Operation operation) {
        ApiResponse unsupported = errorResponse("Unsupported Media Type – corpo enviado com Content-Type diferente de application/json");
        addExample(unsupported, "Content-Type não suportado", ErrorExamples.UNSUPPORTED_MEDIA_TYPE);
        operation.getResponses().addApiResponse("415", unsupported);
    }

    // ---------- Idempotência ----------

    private void documentIdempotency(Operation operation) {
        operation.setDescription(operation.getDescription() + " Aceita o cabeçalho opcional `" + IdempotencyService.HEADER
                + "`: repetir a mesma requisição com a mesma chave não cria nada de novo e devolve a resposta original.");

        // Sem "example" de propósito: o Swagger UI preencheria o campo sozinho e todo teste sairia com a mesma chave
        Parameter header = findParameter(operation, HEADER_PARAMETER, IdempotencyService.HEADER);
        header.required(false)
                .description("Chave de idempotência (opcional): um valor único gerado pelo cliente para esta operação, de preferência "
                        + "um UUID, como `3f2b8c1e-7a4d-4e9b-9c55-2d1f6a8b0e77`. De 1 a 100 caracteres entre letras, números, ponto, "
                        + "hífen, dois-pontos e underline. Se a mesma requisição chegar de novo com a mesma chave, a API não processa "
                        + "outra vez e devolve a resposta original com `" + IdempotencyService.REPLAYED_HEADER + ": true`. "
                        + "Deixe em branco para um POST comum.")
                .schema(new StringSchema())
                .example(null);

        ApiResponse created = operation.getResponses().get("201");
        if (created != null) {
            created.setDescription(created.getDescription() + ". Com " + IdempotencyService.HEADER
                    + ", uma repetição devolve este mesmo 201 com a resposta original, sem criar outro registro");
            created.addHeaderObject(IdempotencyService.REPLAYED_HEADER, new Header()
                    .description("Só aparece quando a requisição traz " + IdempotencyService.HEADER + ": false se a operação "
                            + "acabou de ser processada, true se a resposta é a original, guardada")
                    .schema(new BooleanSchema()));
            created.addHeaderObject(HttpHeaders.LOCATION, new Header()
                    .description("Endereço do recurso criado")
                    .schema(new StringSchema().example("http://localhost:8080/artists/7")));
        }

        ApiResponse badRequest = operation.getResponses().get("400");
        if (badRequest != null) {
            badRequest.setDescription(badRequest.getDescription() + "; " + IdempotencyService.HEADER + " fora do formato aceito");
            addExample(badRequest, "Chave de idempotência inválida", ErrorExamples.BAD_REQUEST_IDEMPOTENCY_KEY);
        }

        ApiResponse conflict = operation.getResponses().get("409");
        if (conflict != null) {
            conflict.setDescription(conflict.getDescription() + "; outra requisição com a mesma chave de idempotência "
                    + "ainda está sendo processada");
            addExample(conflict, "Chave em processamento", ErrorExamples.CONFLICT_IDEMPOTENCY_IN_PROGRESS);
        }

        ApiResponse unprocessable = errorResponse("Unprocessable Content – a chave de idempotência já foi usada em uma requisição "
                + "diferente (outra rota ou outro corpo)");
        addExample(unprocessable, "Chave reaproveitada", ErrorExamples.UNPROCESSABLE_IDEMPOTENCY_KEY);
        operation.getResponses().addApiResponse("422", unprocessable);
    }

    // ---------- Rate limiting ----------

    private void documentRateLimit(Operation operation) {
        long secondsToRefill = (long) Math.ceil(rateLimit.capacity() * (double) rateLimit.refillPeriod().toSeconds()
                / rateLimit.refillTokens());
        successResponses(operation).forEach(response -> addRateLimitHeaders(response, rateLimit.capacity() - 1, 1));

        ApiResponse tooMany = errorResponse("Too Many Requests – limite de requisições por IP excedido (token bucket com "
                + rateLimit.capacity() + " fichas e reposição de " + rateLimit.refillTokens() + " a cada "
                + OpenApiTexts.describe(rateLimit.refillPeriod()) + "). Espere o tempo indicado em Retry-After");
        tooMany.addHeaderObject(HttpHeaders.RETRY_AFTER, new Header()
                .description("Segundos até a próxima requisição ser aceita")
                .schema(new IntegerSchema().example(1)));
        addRateLimitHeaders(tooMany, 0, secondsToRefill);
        addExample(tooMany, "Limite excedido", ErrorExamples.TOO_MANY_REQUESTS);
        operation.getResponses().addApiResponse("429", tooMany);
    }

    private void addRateLimitHeaders(ApiResponse response, long remainingExample, long resetExample) {
        response.addHeaderObject(RateLimitInterceptor.LIMIT_HEADER, new Header()
                .description("Capacidade do balde: quantas requisições seguidas o IP pode fazer")
                .schema(new IntegerSchema().example(rateLimit.capacity())));
        response.addHeaderObject(RateLimitInterceptor.REMAINING_HEADER, new Header()
                .description("Fichas que sobraram no balde depois desta requisição")
                .schema(new IntegerSchema().example(remainingExample)));
        response.addHeaderObject(RateLimitInterceptor.RESET_HEADER, new Header()
                .description("Segundos até o balde ficar cheio de novo")
                .schema(new IntegerSchema().example(resetExample)));
    }

    // ---------- Auxiliares ----------

    private static Parameter findParameter(Operation operation, String in, String name) {
        if (operation.getParameters() == null) {
            return null;
        }
        return operation.getParameters().stream()
                .filter(parameter -> in.equals(parameter.getIn()) && name.equalsIgnoreCase(parameter.getName()))
                .findFirst()
                .orElse(null);
    }

    private static boolean hasIntegerPathParameter(Operation operation) {
        return operation.getParameters() != null && operation.getParameters().stream()
                .anyMatch(parameter -> "path".equals(parameter.getIn()) && isInteger(parameter.getSchema()));
    }

    /** No OpenAPI 3.1 o tipo fica em "types" (pode haver mais de um); no 3.0, em "type". Aqui valem os dois. */
    private static boolean isInteger(Schema<?> schema) {
        if (schema == null) {
            return false;
        }
        return "integer".equals(schema.getType()) || (schema.getTypes() != null && schema.getTypes().contains("integer"));
    }

    private static List<ApiResponse> successResponses(Operation operation) {
        List<ApiResponse> success = new ArrayList<>();
        operation.getResponses().forEach((status, response) -> {
            if (status.startsWith("2")) {
                success.add(response);
            }
        });
        return success;
    }

    /** Resposta de erro no formato padrão da API (ApiError). */
    private static ApiResponse errorResponse(String description) {
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType(JSON, new MediaType().schema(new Schema<>().$ref(API_ERROR_SCHEMA))));
    }

    private void addExample(ApiResponse response, String name, String json) {
        if (response.getContent() == null) {
            response.setContent(new Content().addMediaType(JSON, new MediaType().schema(new Schema<>().$ref(API_ERROR_SCHEMA))));
        }
        response.getContent().values().forEach(mediaType -> mediaType.addExamples(name, new Example().value(parse(json))));
    }

    private void addExampleIfAbsent(ApiResponse response, String name, String json) {
        boolean present = response.getContent() != null && response.getContent().values().stream()
                .anyMatch(mediaType -> mediaType.getExamples() != null && mediaType.getExamples().containsKey(name));
        if (!present) {
            addExample(response, name, json);
        }
    }

    /** Transforma o texto JSON do exemplo em objeto, para o Swagger mostrar JSON formatado e não uma string. */
    private Object parse(String json) {
        return jsonMapper.readValue(json, Object.class);
    }

    /** Deixa as respostas em ordem crescente de status (200, 400, 403, 404...). */
    private static void sortResponsesByStatus(Operation operation) {
        ApiResponses sorted = new ApiResponses();
        operation.getResponses().entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .forEach(entry -> sorted.addApiResponse(entry.getKey(), entry.getValue()));
        sorted.setExtensions(operation.getResponses().getExtensions());
        operation.setResponses(sorted);
    }
}
