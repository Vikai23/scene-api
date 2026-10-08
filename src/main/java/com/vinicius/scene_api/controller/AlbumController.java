package com.vinicius.scene_api.controller;

import com.vinicius.scene_api.assembler.AlbumModelAssembler;
import com.vinicius.scene_api.dto.AlbumRequest;
import com.vinicius.scene_api.dto.AlbumResponse;
import com.vinicius.scene_api.dto.ErrorExamples;
import com.vinicius.scene_api.exception.ApiError;
import com.vinicius.scene_api.model.AlbumType;
import com.vinicius.scene_api.service.AlbumService;
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
@RequestMapping("/albums")
@Tag(name = "Álbuns", description = "Cadastro de álbuns, EPs, singles, coletâneas e shows ao vivo. Cada álbum pertence a um artista (N:1) e tem várias faixas (1:N).")
public class AlbumController {

    private final AlbumService albumService;
    private final AlbumModelAssembler assembler;
    private final PagedResourcesAssembler<AlbumResponse> pagedAssembler;

    public AlbumController(AlbumService albumService,
                           AlbumModelAssembler assembler,
                           PagedResourcesAssembler<AlbumResponse> pagedAssembler) {
        this.albumService = albumService;
        this.assembler = assembler;
        this.pagedAssembler = pagedAssembler;
    }

    @Operation(
            summary = "Lista os álbuns (paginado)",
            description = "Retorna uma página com os álbuns cadastrados. Paginação via page (começa em 0), size (padrão 10, máximo 50) "
                    + "e sort (ex.: sort=releaseYear,desc). Campos de ordenação: id, title, releaseYear, type, label."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK – página de álbuns retornada (vazia se não houver nenhum cadastrado)"),
            @ApiResponse(responseCode = "400", description = "Bad Request – campo de ordenação inexistente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Ordenação inválida", value = ErrorExamples.BAD_REQUEST_SORT)))
    })
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<AlbumResponse>>> findAll(
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedAssembler.toModel(albumService.findAll(pageable), assembler));
    }

    @Operation(
            summary = "Busca álbuns pelo título (paginado)",
            description = "Consulta personalizada: retorna os álbuns cujo título contém o texto informado, sem diferenciar "
                    + "maiúsculas de minúsculas. Ex.: title=scream encontra 'Stand Up and Scream'."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK – página com os álbuns encontrados (vazia se nenhum bater com o texto)"),
            @ApiResponse(responseCode = "400", description = "Bad Request – parâmetro 'title' ausente, em branco ou com mais de 150 caracteres",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(name = "Parâmetro ausente", value = ErrorExamples.BAD_REQUEST_PARAM_MISSING),
                                    @ExampleObject(name = "Parâmetro em branco", value = ErrorExamples.BAD_REQUEST_PARAM_BLANK)
                            }))
    })
    @GetMapping("/search")
    public ResponseEntity<PagedModel<EntityModel<AlbumResponse>>> searchByTitle(
            @Parameter(description = "Parte do título do álbum (de 1 a 150 caracteres)", example = "scream")
            @RequestParam @NotBlank @Size(max = 150) String title,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedAssembler.toModel(albumService.searchByTitle(title, pageable), assembler));
    }

    @Operation(
            summary = "Lista os álbuns de um artista (paginado)",
            description = "Consulta personalizada: retorna os álbuns do artista informado (relação 1:N)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK – página com os álbuns do artista (vazia se ele não tiver nenhum)"),
            @ApiResponse(responseCode = "400", description = "Bad Request – id do artista inválido (não numérico)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Id inválido", value = ErrorExamples.BAD_REQUEST_ID))),
            @ApiResponse(responseCode = "404", description = "Not Found – nenhum artista com o id informado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Artista inexistente", value = ErrorExamples.NOT_FOUND_ARTIST)))
    })
    @GetMapping("/by-artist/{artistId}")
    public ResponseEntity<PagedModel<EntityModel<AlbumResponse>>> findByArtist(
            @Parameter(description = "Id do artista", example = "1")
            @PathVariable Long artistId,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedAssembler.toModel(albumService.findByArtist(artistId, pageable), assembler));
    }

    @Operation(
            summary = "Lista os álbuns de um formato (paginado)",
            description = "Consulta personalizada: retorna os álbuns do formato informado. Valores aceitos: ALBUM, EP, SINGLE, COMPILATION, LIVE."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK – página com os álbuns do formato (vazia se não houver nenhum)"),
            @ApiResponse(responseCode = "400", description = "Bad Request – formato inexistente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Formato inválido", value = ErrorExamples.BAD_REQUEST_ENUM_TYPE)))
    })
    @GetMapping("/by-type/{type}")
    public ResponseEntity<PagedModel<EntityModel<AlbumResponse>>> findByType(
            @Parameter(description = "Formato do lançamento", example = "ALBUM")
            @PathVariable AlbumType type,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedAssembler.toModel(albumService.findByType(type, pageable), assembler));
    }

    @Operation(
            summary = "Busca um álbum pelo id",
            description = "Retorna os dados de um único álbum a partir do seu identificador, com links para o artista e as faixas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK – álbum encontrado"),
            @ApiResponse(responseCode = "400", description = "Bad Request – id inválido (não numérico)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Id inválido", value = ErrorExamples.BAD_REQUEST_ID))),
            @ApiResponse(responseCode = "404", description = "Not Found – nenhum álbum com o id informado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Não encontrado", value = ErrorExamples.NOT_FOUND_ALBUM)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<AlbumResponse>> findById(
            @Parameter(description = "Id do álbum", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(assembler.toModel(albumService.findById(id)));
    }

    @Operation(
            summary = "Cadastra um novo álbum",
            description = "Cria um álbum para um artista existente. Obrigatórios: artistId, title (1 a 150 caracteres, único dentro do artista), "
                    + "releaseYear (1900 a 2100) e type. Opcional: label (gravadora)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created – álbum criado; o cabeçalho Location aponta para o novo recurso"),
            @ApiResponse(responseCode = "400", description = "Bad Request – dados inválidos (título em branco ou com símbolos proibidos, ano fora do intervalo, formato inexistente...) ou JSON malformado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(name = "Dados inválidos", value = ErrorExamples.BAD_REQUEST_VALIDATION),
                                    @ExampleObject(name = "JSON malformado", value = ErrorExamples.BAD_REQUEST_MALFORMED)
                            })),
            @ApiResponse(responseCode = "404", description = "Not Found – o artista informado em artistId não existe",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Artista inexistente", value = ErrorExamples.NOT_FOUND_ARTIST))),
            @ApiResponse(responseCode = "409", description = "Conflict – esse artista já tem um álbum com esse título",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Título duplicado", value = ErrorExamples.CONFLICT_ALBUM)))
    })
    @PostMapping
    public ResponseEntity<EntityModel<AlbumResponse>> create(@RequestBody @Valid AlbumRequest request) {
        EntityModel<AlbumResponse> model = assembler.toModel(albumService.create(request));
        return ResponseEntity.created(model.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(model);
    }

    @Operation(
            summary = "Atualiza um álbum",
            description = "Substitui todos os dados de um álbum existente (inclusive o artista dono). As mesmas regras de validação do cadastro se aplicam."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK – álbum atualizado"),
            @ApiResponse(responseCode = "400", description = "Bad Request – dados inválidos, id não numérico ou JSON malformado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Dados inválidos", value = ErrorExamples.BAD_REQUEST_VALIDATION))),
            @ApiResponse(responseCode = "404", description = "Not Found – nenhum álbum com o id informado, ou artista de artistId inexistente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(name = "Álbum inexistente", value = ErrorExamples.NOT_FOUND_ALBUM),
                                    @ExampleObject(name = "Artista inexistente", value = ErrorExamples.NOT_FOUND_ARTIST)
                            })),
            @ApiResponse(responseCode = "409", description = "Conflict – o artista já tem outro álbum com esse título",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Título duplicado", value = ErrorExamples.CONFLICT_ALBUM)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<AlbumResponse>> update(
            @Parameter(description = "Id do álbum a atualizar", example = "1")
            @PathVariable Long id,
            @RequestBody @Valid AlbumRequest request) {
        return ResponseEntity.ok(assembler.toModel(albumService.update(id, request)));
    }

    @Operation(
            summary = "Remove um álbum",
            description = "Exclui o álbum junto com as faixas dele. As faixas também saem de todas as playlists."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "No Content – álbum removido (sem corpo na resposta)"),
            @ApiResponse(responseCode = "400", description = "Bad Request – id inválido (não numérico)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Id inválido", value = ErrorExamples.BAD_REQUEST_ID))),
            @ApiResponse(responseCode = "404", description = "Not Found – nenhum álbum com o id informado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Não encontrado", value = ErrorExamples.NOT_FOUND_ALBUM)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Id do álbum a remover", example = "1")
            @PathVariable Long id) {
        albumService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
