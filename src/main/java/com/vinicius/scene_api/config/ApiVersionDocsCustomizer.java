package com.vinicius.scene_api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;

/**
 * Ajusta a documentação de UMA versão da API.
 * <p>
 * O Swagger UI mostra no topo o seletor "Versão da API", com uma opção para cada versão. Cada opção é uma
 * documentação separada (um {@code GroupedOpenApi}, ver SwaggerConfig), e este customizer deixa cada uma assim:
 * o selo de versão ao lado do título mostra a versão escolhida (1.0 ou 2.0), o header {@code X-API-Version} já
 * vem preenchido com ela em todas as rotas, e {@code GET /artists/{id}} mostra só o formato de resposta daquela versão.
 * <p>
 * O versionamento continua sendo pelo header da requisição: o seletor só muda o valor que o Swagger UI envia.
 */
public class ApiVersionDocsCustomizer implements OpenApiCustomizer {

    private final String version;

    public ApiVersionDocsCustomizer(String version) {
        this.version = version;
    }

    @Override
    public void customise(OpenAPI openApi) {
        if (openApi.getInfo() != null) {
            // O selo ao lado do título do Swagger UI mostra info.version: 1.0 na documentação da v1, 2.0 na da v2
            openApi.getInfo().setVersion(version + ".0");
        }
        if (openApi.getPaths() == null) {
            return;
        }
        openApi.getPaths().forEach((path, pathItem) ->
                pathItem.readOperationsMap().forEach((method, operation) -> {
                    presetVersionHeader(operation);
                    if (OpenApiPartTwoCustomizer.VERSIONED_PATH.equals(path) && method == PathItem.HttpMethod.GET) {
                        keepOnlyThisVersion(operation);
                    }
                }));
    }

    /** O campo X-API-Version de cada rota já vem com a versão desta documentação. */
    private void presetVersionHeader(Operation operation) {
        if (operation.getParameters() != null) {
            operation.getParameters().stream()
                    .filter(parameter -> "header".equals(parameter.getIn())
                            && ApiVersionConfig.HEADER.equalsIgnoreCase(parameter.getName()))
                    .forEach(parameter -> {
                        parameter.setSchema(new StringSchema()._default(version));
                        parameter.setDescription(parameter.getDescription() + " Nesta documentação (v" + version
                                + ", escolhida no seletor do topo da página) o campo já vem com " + version + ".");
                    });
        }
        if (operation.getResponses() != null) {
            operation.getResponses().forEach((status, response) -> {
                Header header = response.getHeaders() == null ? null : response.getHeaders().get(ApiVersionConfig.HEADER);
                if (status.startsWith("2") && header != null) {
                    header.setSchema(new StringSchema().example(version));
                }
            });
        }
    }

    /** Em GET /artists/{id}, a resposta 200 fica só com o schema e o exemplo desta versão. */
    private void keepOnlyThisVersion(Operation operation) {
        ApiResponse ok = operation.getResponses() == null ? null : operation.getResponses().get("200");
        if (ok == null || ok.getContent() == null) {
            return;
        }
        boolean versionTwo = ApiVersionConfig.V2.equals(version);
        String example = versionTwo ? OpenApiPartTwoCustomizer.ARTIST_V2_EXAMPLE_NAME : OpenApiPartTwoCustomizer.ARTIST_V1_EXAMPLE_NAME;
        String schema = versionTwo ? OpenApiPartTwoCustomizer.ARTIST_V2_SCHEMA : OpenApiPartTwoCustomizer.ARTIST_V1_SCHEMA;
        ok.setDescription("OK – artista encontrado, no formato da versão " + version);
        ok.getContent().values().forEach(mediaType -> {
            if (mediaType.getExamples() != null) {
                mediaType.getExamples().keySet().removeIf(name -> !name.equals(example));
            }
            mediaType.setSchema(new Schema<>().$ref("#/components/schemas/" + schema));
        });
    }
}
