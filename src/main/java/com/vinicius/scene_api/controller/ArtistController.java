package com.vinicius.scene_api.controller;

import com.vinicius.scene_api.assembler.ArtistModelAssembler;
import com.vinicius.scene_api.assembler.ArtistV2ModelAssembler;
import com.vinicius.scene_api.config.ApiVersionConfig;
import com.vinicius.scene_api.dto.ArtistRequest;
import com.vinicius.scene_api.dto.ArtistResponse;
import com.vinicius.scene_api.dto.ArtistResponseV2;
import com.vinicius.scene_api.dto.ErrorExamples;
import com.vinicius.scene_api.exception.ApiError;
import com.vinicius.scene_api.idempotency.IdempotencyService;
import com.vinicius.scene_api.idempotency.IdempotentResult;
import com.vinicius.scene_api.model.Genre;
import com.vinicius.scene_api.service.ArtistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.data.web.PageableDefault;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/artists")
@Tag(name = "Artistas", description = "Cadastro de artistas e bandas. Um artista tem vários álbuns (relação 1:N) e no máximo um perfil (relação 1:1).")
public class ArtistController {

    private final ArtistService artistService;
    private final IdempotencyService idempotencyService;
    private final ArtistModelAssembler assembler;
    private final ArtistV2ModelAssembler assemblerV2;
    private final PagedResourcesAssembler<ArtistResponse> pagedAssembler;

    public ArtistController(ArtistService artistService,
                            IdempotencyService idempotencyService,
                            ArtistModelAssembler assembler,
                            ArtistV2ModelAssembler assemblerV2,
                            PagedResourcesAssembler<ArtistResponse> pagedAssembler) {
        this.artistService = artistService;
        this.idempotencyService = idempotencyService;
        this.assembler = assembler;
        this.assemblerV2 = assemblerV2;
        this.pagedAssembler = pagedAssembler;
    }

    @Operation(
            summary = "Lista os artistas (paginado)",
            description = "Retorna uma página com os artistas cadastrados. Paginação via page (começa em 0), size (padrão 10, máximo 50) "
                    + "e sort (ex.: sort=name,asc). Campos de ordenação: id, name, genre, country, formedYear."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK – página de artistas retornada (vazia se não houver nenhum cadastrado)"),
            @ApiResponse(responseCode = "400", description = "Bad Request – campo de ordenação inexistente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Ordenação inválida", value = ErrorExamples.BAD_REQUEST_SORT)))
    })
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<ArtistResponse>>> findAll(
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedAssembler.toModel(artistService.findAll(pageable), assembler));
    }

    @Operation(
            summary = "Busca artistas pelo nome (paginado)",
            description = "Consulta personalizada: retorna os artistas cujo nome contém o texto informado, sem diferenciar "
                    + "maiúsculas de minúsculas. Ex.: name=bring encontra 'Bring Me the Horizon'."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK – página com os artistas encontrados (vazia se nenhum bater com o texto)"),
            @ApiResponse(responseCode = "400", description = "Bad Request – parâmetro 'name' ausente, em branco ou com mais de 100 caracteres",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(name = "Parâmetro ausente", value = ErrorExamples.BAD_REQUEST_PARAM_MISSING),
                                    @ExampleObject(name = "Parâmetro em branco", value = ErrorExamples.BAD_REQUEST_PARAM_BLANK)
                            }))
    })
    @GetMapping("/search")
    public ResponseEntity<PagedModel<EntityModel<ArtistResponse>>> searchByName(
            @Parameter(description = "Parte do nome do artista (de 1 a 100 caracteres)", example = "bring")
            @RequestParam @NotBlank @Size(max = 100) String name,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedAssembler.toModel(artistService.searchByName(name, pageable), assembler));
    }

    @Operation(
            summary = "Lista os artistas de um gênero (paginado)",
            description = "Consulta personalizada: retorna os artistas do gênero informado. "
                    + "Valores aceitos: ROCK, ALTERNATIVE, PUNK, EMO, POST_HARDCORE, METALCORE, METAL, POP, HIP_HOP, ELECTRONIC, JAZZ, FOLK, CLASSICAL, OTHER."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK – página com os artistas do gênero (vazia se não houver nenhum)"),
            @ApiResponse(responseCode = "400", description = "Bad Request – gênero inexistente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Gênero inválido", value = ErrorExamples.BAD_REQUEST_ENUM_GENRE)))
    })
    @GetMapping("/by-genre/{genre}")
    public ResponseEntity<PagedModel<EntityModel<ArtistResponse>>> findByGenre(
            @Parameter(description = "Gênero musical", example = "METALCORE")
            @PathVariable Genre genre,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedAssembler.toModel(artistService.findByGenre(genre, pageable), assembler));
    }

    @Operation(
            summary = "Busca um artista pelo id",
            description = "Retorna os dados de um único artista a partir do seu identificador, com links para álbuns e perfil."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK – artista encontrado"),
            @ApiResponse(responseCode = "400", description = "Bad Request – id inválido (não numérico)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Id inválido", value = ErrorExamples.BAD_REQUEST_ID))),
            @ApiResponse(responseCode = "404", description = "Not Found – nenhum artista com o id informado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Não encontrado", value = ErrorExamples.NOT_FOUND_ARTIST)))
    })
    @GetMapping(path = "/{id}", version = ApiVersionConfig.V1)
    public ResponseEntity<EntityModel<ArtistResponse>> findById(
            @Parameter(description = "Id do artista", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(assembler.toModel(artistService.findById(id)));
    }

    /**
     * Versão 2 da mesma rota: atende quando o cliente manda o header X-API-Version: 2.
     * A documentação das duas versões fica em uma única operação no Swagger (ver OpenApiPartTwoCustomizer).
     */
    @GetMapping(path = "/{id}", version = ApiVersionConfig.V2)
    public ResponseEntity<EntityModel<ArtistResponseV2>> findByIdV2(
            @Parameter(description = "Id do artista", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(assemblerV2.toModel(artistService.findByIdV2(id)));
    }

    @Operation(
            summary = "Cadastra um novo artista",
            description = "Cria um artista. Nome (2 a 100 caracteres, único) e gênero são obrigatórios; país (somente letras) e ano de formação (1000 a 2100) são opcionais."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created – artista criado; o header Location aponta para o novo recurso"),
            @ApiResponse(responseCode = "400", description = "Bad Request – dados inválidos (nome em branco ou fora de 2 a 100 caracteres, país com números, ano fora do intervalo, gênero inexistente) ou JSON malformado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(name = "Dados inválidos", value = ErrorExamples.BAD_REQUEST_VALIDATION),
                                    @ExampleObject(name = "JSON malformado", value = ErrorExamples.BAD_REQUEST_MALFORMED)
                            })),
            @ApiResponse(responseCode = "409", description = "Conflict – já existe um artista com esse nome",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Nome duplicado", value = ErrorExamples.CONFLICT_ARTIST)))
    })
    @PostMapping
    public ResponseEntity<EntityModel<ArtistResponse>> create(
            @RequestHeader(name = IdempotencyService.HEADER, required = false) String idempotencyKey,
            @RequestBody @Valid ArtistRequest request) {
        IdempotentResult<ArtistResponse> result = idempotencyService.execute(idempotencyKey, "POST /artists",
                request, ArtistResponse.class, () -> artistService.create(request));
        EntityModel<ArtistResponse> model = assembler.toModel(result.body());
        return ResponseEntity.created(model.getRequiredLink(IanaLinkRelations.SELF).toUri())
                .headers(result.responseHeaders())
                .body(model);
    }

    @Operation(
            summary = "Atualiza um artista",
            description = "Substitui todos os dados de um artista existente. As mesmas regras de validação do cadastro se aplicam."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK – artista atualizado"),
            @ApiResponse(responseCode = "400", description = "Bad Request – dados inválidos, id não numérico ou JSON malformado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Dados inválidos", value = ErrorExamples.BAD_REQUEST_VALIDATION))),
            @ApiResponse(responseCode = "404", description = "Not Found – nenhum artista com o id informado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Não encontrado", value = ErrorExamples.NOT_FOUND_ARTIST))),
            @ApiResponse(responseCode = "409", description = "Conflict – já existe outro artista com esse nome",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Nome duplicado", value = ErrorExamples.CONFLICT_ARTIST)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<ArtistResponse>> update(
            @Parameter(description = "Id do artista a atualizar", example = "1")
            @PathVariable Long id,
            @RequestBody @Valid ArtistRequest request) {
        return ResponseEntity.ok(assembler.toModel(artistService.update(id, request)));
    }

    @Operation(
            summary = "Remove um artista",
            description = "Exclui o artista junto com o perfil, os álbuns e as faixas dele. As faixas também saem de todas as playlists."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "No Content – artista removido (sem corpo na resposta)"),
            @ApiResponse(responseCode = "400", description = "Bad Request – id inválido (não numérico)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Id inválido", value = ErrorExamples.BAD_REQUEST_ID))),
            @ApiResponse(responseCode = "404", description = "Not Found – nenhum artista com o id informado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Não encontrado", value = ErrorExamples.NOT_FOUND_ARTIST)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Id do artista a remover", example = "1")
            @PathVariable Long id) {
        artistService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
