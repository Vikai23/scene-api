package com.vinicius.scene_api.config;

import com.vinicius.scene_api.idempotency.IdempotencyProperties;
import com.vinicius.scene_api.ratelimit.RateLimitProperties;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class SwaggerConfig {

    private static final List<String> TAG_ORDER = List.of(
            "Artistas", "Perfis de Artista", "Álbuns", "Faixas", "Playlists");

    private static final String PAGE_DESCRIPTION =
            "Número da página, começando em 0 (a primeira página é a 0). Padrão: 0.";
    private static final String SIZE_DESCRIPTION =
            "Quantidade de itens por página. Padrão: 10. Máximo: 50 (valores maiores são reduzidos para 50).";
    private static final String SORT_DESCRIPTION =
            "Ordenação no formato campo,asc ou campo,desc (ex.: name,asc). Os campos aceitos estão na descrição da rota; "
                    + "um campo que não existe dá 400 Bad Request. Padrão: id,asc.";

    private static final String DESCRIPTION = """
            API REST de catálogo musical: artistas, perfis, álbuns, faixas e playlists. \
            Projeto final da disciplina de Web Services (Senac TSI), feito com Spring Boot, Spring Data JPA, H2, Bean Validation, \
            Spring HATEOAS e Springdoc. Os dados de exemplo são carregados pela classe `DataLoader` e usam artistas do perfil \
            do autor no Last.fm (Bring Me the Horizon, Title Fight, Asking Alexandria, blessthefall, Alesana, I Promised the World).

            ### Entidades e relacionamentos

            | Entidade | Relacionamento |
            |---|---|
            | **Artista** e **Perfil de Artista** | 1:1 – cada artista tem no máximo um perfil |
            | **Artista** e **Álbum** | 1:N – um artista tem vários álbuns |
            | **Álbum** e **Faixa** | 1:N – um álbum tem várias faixas |
            | **Playlist** e **Faixa** | N:N – uma playlist tem várias faixas e uma faixa pode estar em várias playlists |

            Enums: `Genre` (gênero do artista), `AlbumType` (álbum, EP, single, coletânea ou ao vivo) e `Visibility` (playlist pública ou privada).

            ### Paginação

            Todas as listagens (inclusive as buscas) são paginadas e aceitam:
            * `page` – número da página, começando em **0**;
            * `size` – itens por página (padrão **10**, máximo **50**);
            * `sort` – ordenação, por exemplo `sort=name,asc` ou `sort=releaseYear,desc`.

            ### HATEOAS

            Cada recurso devolve `_links` com `self`, `update`, `delete` e links para os recursos relacionados. \
            As listagens vêm em `_embedded`, com `page` (tamanho, total de itens e de páginas) e links de navegação (`first`, `prev`, `next`, `last`).

            ### Versionamento (header `X-API-Version`)

            A API tem as versões **1** e **2**, escolhidas no header `X-API-Version`. Sem o header vale a versão 1 \
            e um valor diferente de 1 e 2 devolve **400 Bad Request**. A resposta traz o mesmo header com a versão que atendeu. \
            Hoje só `GET /artists/{id}` muda entre as versões: na 2, `country` e `formedYear` passam para o objeto `origin` e entram \
            `albumCount` e `hasProfile`. Nas demais rotas as duas versões respondem igual.

            No topo desta página, o seletor **Versão da API** troca a documentação entre a **v1** e a **v2**. \
            Com a v2 escolhida, o campo `X-API-Version` de todas as rotas já vem com `2` no *Try it out*.

            ### Idempotência (header `X-Idempotency-Key`)

            Todo `POST` aceita o header opcional `X-Idempotency-Key` (de 1 a 100 caracteres entre letras, números e `. _ : -`; o ideal é um UUID). \
            A API guarda a chave e a resposta por %s. Repetir a mesma requisição com a mesma chave **não** cria nada de novo: \
            volta a resposta original, com `X-Idempotent-Replayed: true`. A mesma chave em uma requisição diferente devolve \
            **422 Unprocessable Content**. Sem o header, o POST funciona normalmente.

            ### Rate limiting (token bucket)

            Cada IP tem um bucket com **%d tokens**. Cada chamada a uma rota da API consome um token e o bucket recebe %d tokens a cada %s. \
            Com o bucket vazio a resposta é **429 Too Many Requests**, com o header `Retry-After` (segundos até o próximo token). \
            Toda resposta informa o saldo em `X-RateLimit-Limit` (capacidade), `X-RateLimit-Remaining` (tokens restantes) e \
            `X-RateLimit-Reset` (segundos até o bucket encher de novo).

            ### CORS

            Uma página de outra origem só consegue chamar a API pelo navegador se a origem estiver na lista: %s. \
            Métodos aceitos: `GET`, `POST`, `PUT`, `DELETE` e `OPTIONS` (o *preflight*). Outra origem recebe \
            **403 Forbidden**. Chamadas sem o header `Origin` (Postman, curl e este Swagger UI) não passam por essa checagem. \
            Para ver na prática, o projeto tem um front-end em Angular em outro repositório \
            ([scene-web](https://github.com/Vikai23/scene-web)), que roda em `http://localhost:4200` e consome estas rotas pelo navegador.

            ### Formato dos erros

            Todo erro tem o mesmo corpo (`ApiError`): `status`, `error` (nome oficial do status HTTP), `message`, `path`, `timestamp` e `details` \
            (problemas campo a campo, preenchido nos erros de validação). Cada rota lista, em *Responses*, todos os erros que podem acontecer nela.

            | Status | Quando acontece |
            |---|---|
            | **200 OK** | Consulta ou atualização feita com sucesso |
            | **201 Created** | Recurso criado (o header `Location` aponta para ele) |
            | **204 No Content** | Recurso removido |
            | **400 Bad Request** | Dados inválidos, JSON malformado ou com valor de tipo errado, id não numérico, enum inexistente, paginação ou ordenação inválida, versão da API inexistente ou chave de idempotência fora do formato |
            | **403 Forbidden** | Requisição de uma origem não permitida pelo CORS |
            | **404 Not Found** | Recurso ou rota inexistente |
            | **405 Method Not Allowed** | Método HTTP não aceito na rota |
            | **406 Not Acceptable** | Header `Accept` que só aceita um formato diferente de JSON (a requisição não é executada) |
            | **409 Conflict** | Registro duplicado (ex.: nome de artista já cadastrado) ou requisição simultânea com a mesma chave de idempotência |
            | **415 Unsupported Media Type** | Corpo enviado sem ser `application/json` |
            | **422 Unprocessable Content** | Chave de idempotência reaproveitada em uma requisição diferente |
            | **429 Too Many Requests** | Limite de requisições por IP excedido |

            Entrada inválida do cliente sempre resulta em um erro 4xx. O **500 Internal Server Error** fica reservado para falhas \
            inesperadas do próprio servidor e, por isso, não aparece na lista de respostas das rotas.
            """;

    @Bean
    public OpenAPI sceneOpenAPI(RateLimitProperties rateLimit, IdempotencyProperties idempotency, CorsProperties cors) {
        String description = DESCRIPTION.formatted(
                OpenApiTexts.describe(idempotency.ttl()),
                rateLimit.capacity(), rateLimit.refillTokens(), OpenApiTexts.describe(rateLimit.refillPeriod()),
                cors.allowedOrigins().stream().map(origin -> "`" + origin + "`").collect(Collectors.joining(", ")));
        return new OpenAPI()
                .info(new Info()
                        .title("Scene API")
                        .version("2.0.0")
                        .description(description)
                        .termsOfService("/termos-de-uso.html")
                        .contact(new Contact()
                                .name("Vinícius (TSI Senac)")
                                .url("https://github.com/Vikai23"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .externalDocs(new ExternalDocumentation()
                        .description("Repositório do projeto no GitHub (README com instruções e coleção do Postman)")
                        .url("https://github.com/Vikai23/scene-api"))
                .tags(List.of(
                        new Tag().name("Artistas").description(
                                "Cadastro de artistas e bandas. Um artista tem vários álbuns (relação 1:N) e no máximo um perfil (relação 1:1)."),
                        new Tag().name("Perfis de Artista").description(
                                "Perfil detalhado de um artista (biografia, cidade natal, site e Instagram). Relação 1:1: cada artista tem no máximo um perfil."),
                        new Tag().name("Álbuns").description(
                                "Cadastro de álbuns, EPs, singles, coletâneas e shows ao vivo. Cada álbum pertence a um artista (N:1) e tem várias faixas (1:N)."),
                        new Tag().name("Faixas").description(
                                "Cadastro de faixas. Cada faixa pertence a um álbum (N:1) e pode estar em várias playlists (N:N)."),
                        new Tag().name("Playlists").description(
                                "Cadastro de playlists e das faixas que fazem parte de cada uma. Relação N:N: uma playlist tem várias faixas e uma faixa pode estar em várias playlists.")));
    }

    /**
     * Uma documentação por versão da API. Com duas, o Swagger UI mostra no topo o seletor "Versão da API":
     * escolhida a "v2", todo Try it out já envia o header X-API-Version: 2 (ver ApiVersionDocsCustomizer).
     * O endereço /v3/api-docs, sem versão, continua existindo e documenta as duas versões juntas.
     */
    @Bean
    public GroupedOpenApi versionOneDocs() {
        return versionDocs(ApiVersionConfig.V1, "v1");
    }

    @Bean
    public GroupedOpenApi versionTwoDocs() {
        return versionDocs(ApiVersionConfig.V2, "v2");
    }

    private static GroupedOpenApi versionDocs(String version, String displayName) {
        return GroupedOpenApi.builder()
                .group("v" + version)
                .displayName(displayName)
                .pathsToMatch("/**")
                .addOpenApiCustomizer(new ApiVersionDocsCustomizer(version))
                .build();
    }

    /**
     * Mantém os grupos do Swagger na ordem do domínio (e não na ordem alfabética). Os customizers globais
     * (GlobalOpenApiCustomizer) valem para a documentação de cada versão também.
     */
    @Bean
    public GlobalOpenApiCustomizer tagOrderCustomizer() {
        return openApi -> {
            if (openApi.getTags() == null) {
                return;
            }
            List<Tag> sorted = new ArrayList<>(openApi.getTags());
            sorted.sort(Comparator.comparingInt(tag -> {
                int index = TAG_ORDER.indexOf(tag.getName());
                return index < 0 ? Integer.MAX_VALUE : index;
            }));
            openApi.setTags(sorted);
        };
    }

    /**
     * O springdoc descreve page, size e sort em inglês. Este customizer troca esses textos por versões em português
     * e faz o mesmo com o bloco "page" das respostas paginadas, para a documentação não misturar idiomas.
     */
    @Bean
    public GlobalOpenApiCustomizer pagingDescriptionsCustomizer() {
        return openApi -> {
            if (openApi.getPaths() != null) {
                openApi.getPaths().values().forEach(pathItem -> {
                    for (Operation operation : pathItem.readOperations()) {
                        if (operation.getParameters() == null) {
                            continue;
                        }
                        for (Parameter parameter : operation.getParameters()) {
                            if (!"query".equals(parameter.getIn())) {
                                continue;
                            }
                            if ("page".equals(parameter.getName())) {
                                parameter.setDescription(PAGE_DESCRIPTION);
                            } else if ("size".equals(parameter.getName())) {
                                parameter.setDescription(SIZE_DESCRIPTION);
                            } else if ("sort".equals(parameter.getName())) {
                                parameter.setDescription(SORT_DESCRIPTION);
                            }
                        }
                    }
                });
            }
            if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
                Schema<?> metadata = openApi.getComponents().getSchemas().get("PageMetadata");
                if (metadata != null && metadata.getProperties() != null) {
                    describe(metadata, "size", "Quantidade de itens por página");
                    describe(metadata, "totalElements", "Total de itens encontrados, somando todas as páginas");
                    describe(metadata, "totalPages", "Total de páginas");
                    describe(metadata, "number", "Número da página atual (começa em 0)");
                }
            }
        };
    }

    private static void describe(Schema<?> schema, String property, String description) {
        Schema<?> field = schema.getProperties().get(property);
        if (field != null) {
            field.setDescription(description);
        }
    }

    /** Troca o transformador padrão do Swagger UI pelo que injeta o CSS e o favicon da API. */
    @Bean
    public SwaggerIndexTransformer swaggerIndexTransformer(SwaggerUiConfigProperties swaggerUiConfig,
                                                           SwaggerUiOAuthProperties swaggerUiOAuthProperties,
                                                           SwaggerWelcomeCommon swaggerWelcomeCommon,
                                                           ObjectMapperProvider objectMapperProvider) {
        return new SwaggerCssInjector(swaggerUiConfig, swaggerUiOAuthProperties, swaggerWelcomeCommon, objectMapperProvider);
    }
}
