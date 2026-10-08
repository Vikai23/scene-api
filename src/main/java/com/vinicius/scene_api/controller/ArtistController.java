package com.vinicius.scene_api.controller;

import com.vinicius.scene_api.assembler.ArtistModelAssembler;
import com.vinicius.scene_api.dto.ArtistRequest;
import com.vinicius.scene_api.dto.ArtistResponse;
import com.vinicius.scene_api.dto.ErrorExamples;
import com.vinicius.scene_api.exception.ApiError;
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
    private final ArtistModelAssembler assembler;
    private final PagedResourcesAssembler<ArtistResponse> pagedAssembler;

    public ArtistController(ArtistService artistService,
                            ArtistModelAssembler assembler,
                            PagedResourcesAssembler<ArtistResponse> pagedAssembler) {
        this.artistService = artistService;
        this.assembler = assembler;
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
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<ArtistResponse>> findById(
            @Parameter(description = "Id do artista", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(assembler.toModel(artistService.findById(id)));
    }

    @Operation(
            summary = "Cadastra um novo artista",
            description = "Cria um artista. Nome (2 a 100 caracteres, único) e gênero são obrigatórios; país (somente letras) e ano de formação (1900 a 2100) são opcionais."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created – artista criado; o cabeçalho Location aponta para o novo recurso"),
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
    public ResponseEntity<EntityModel<ArtistResponse>> create(@RequestBody @Valid ArtistRequest request) {
        EntityModel<ArtistResponse> model = assembler.toModel(artistService.create(request));
        return ResponseEntity.created(model.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(model);
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
