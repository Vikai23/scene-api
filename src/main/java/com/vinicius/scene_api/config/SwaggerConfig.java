package com.vinicius.scene_api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.tags.Tag;
import org.springdoc.core.customizers.OpenApiCustomizer;
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

@Configuration
public class SwaggerConfig {

    private static final List<String> TAG_ORDER = List.of(
            "Artistas", "Perfis de Artista", "Álbuns", "Faixas", "Playlists");

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

            ### Formato dos erros

            Todo erro tem o mesmo corpo (`ApiError`): `status`, `error` (nome oficial do status HTTP), `message`, `path`, `timestamp` e `details` \
            (problemas campo a campo, preenchido nos erros de validação).

            | Status | Quando acontece |
            |---|---|
            | **200 OK** | Consulta ou atualização feita com sucesso |
            | **201 Created** | Recurso criado (o cabeçalho `Location` aponta para ele) |
            | **204 No Content** | Recurso removido |
            | **400 Bad Request** | Dados inválidos, JSON malformado, id não numérico, enum inexistente ou campo de ordenação inexistente |
            | **404 Not Found** | Recurso ou rota inexistente |
            | **405 Method Not Allowed** | Método HTTP não aceito na rota |
            | **409 Conflict** | Registro duplicado (ex.: nome de artista já cadastrado) |
            | **415 Unsupported Media Type** | Corpo enviado sem ser `application/json` |
            | **500 Internal Server Error** | Erro inesperado no servidor |
            """;

    @Bean
    public OpenAPI sceneOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Scene API")
                        .version("1.0.0")
                        .description(DESCRIPTION)
                        .contact(new Contact()
                                .name("Vinícius – Web Services (Senac TSI)"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
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

    /** Mantém os grupos do Swagger na ordem do domínio (e não na ordem alfabética). */
    @Bean
    public OpenApiCustomizer tagOrderCustomizer() {
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

    /** Troca o transformador padrão do Swagger UI pelo que injeta o CSS e o favicon da API. */
    @Bean
    public SwaggerIndexTransformer swaggerIndexTransformer(SwaggerUiConfigProperties swaggerUiConfig,
                                                           SwaggerUiOAuthProperties swaggerUiOAuthProperties,
                                                           SwaggerWelcomeCommon swaggerWelcomeCommon,
                                                           ObjectMapperProvider objectMapperProvider) {
        return new SwaggerCssInjector(swaggerUiConfig, swaggerUiOAuthProperties, swaggerWelcomeCommon, objectMapperProvider);
    }
}
