package com.vinicius.scene_api.controller;

import com.vinicius.scene_api.assembler.PlaylistModelAssembler;
import com.vinicius.scene_api.assembler.TrackModelAssembler;
import com.vinicius.scene_api.dto.ErrorExamples;
import com.vinicius.scene_api.dto.PlaylistResponse;
import com.vinicius.scene_api.dto.TrackRequest;
import com.vinicius.scene_api.dto.TrackResponse;
import com.vinicius.scene_api.exception.ApiError;
import com.vinicius.scene_api.service.TrackService;
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
@RequestMapping("/tracks")
@Tag(name = "Faixas", description = "Cadastro de faixas. Cada faixa pertence a um álbum (N:1) e pode estar em várias playlists (N:N).")
public class TrackController {

    private final TrackService trackService;
    private final TrackModelAssembler assembler;
    private final PlaylistModelAssembler playlistAssembler;
    private final PagedResourcesAssembler<TrackResponse> pagedAssembler;
    private final PagedResourcesAssembler<PlaylistResponse> pagedPlaylistAssembler;

    public TrackController(TrackService trackService,
                           TrackModelAssembler assembler,
                           PlaylistModelAssembler playlistAssembler,
                           PagedResourcesAssembler<TrackResponse> pagedAssembler,
                           PagedResourcesAssembler<PlaylistResponse> pagedPlaylistAssembler) {
        this.trackService = trackService;
        this.assembler = assembler;
        this.playlistAssembler = playlistAssembler;
        this.pagedAssembler = pagedAssembler;
        this.pagedPlaylistAssembler = pagedPlaylistAssembler;
    }

    @Operation(
            summary = "Lista as faixas (paginado)",
            description = "Retorna uma página com as faixas cadastradas. Paginação via page (começa em 0), size (padrão 10, máximo 50) "
                    + "e sort (ex.: sort=title,asc). Campos de ordenação: id, title, trackNumber, durationSeconds."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK – página de faixas retornada (vazia se não houver nenhuma cadastrada)"),
            @ApiResponse(responseCode = "400", description = "Bad Request – campo de ordenação inexistente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Ordenação inválida", value = ErrorExamples.BAD_REQUEST_SORT)))
    })
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<TrackResponse>>> findAll(
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedAssembler.toModel(trackService.findAll(pageable), assembler));
    }

    @Operation(
            summary = "Busca faixas pelo título (paginado)",
            description = "Consulta personalizada: retorna as faixas cujo título contém o texto informado, sem diferenciar "
                    + "maiúsculas de minúsculas. Ex.: title=heart encontra 'Can You Feel My Heart'."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK – página com as faixas encontradas (vazia se nenhuma bater com o texto)"),
            @ApiResponse(responseCode = "400", description = "Bad Request – parâmetro 'title' ausente, em branco ou com mais de 150 caracteres",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(name = "Parâmetro ausente", value = ErrorExamples.BAD_REQUEST_PARAM_MISSING),
                                    @ExampleObject(name = "Parâmetro em branco", value = ErrorExamples.BAD_REQUEST_PARAM_BLANK)
                            }))
    })
    @GetMapping("/search")
    public ResponseEntity<PagedModel<EntityModel<TrackResponse>>> searchByTitle(
            @Parameter(description = "Parte do título da faixa (de 1 a 150 caracteres)", example = "heart")
            @RequestParam @NotBlank @Size(max = 150) String title,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedAssembler.toModel(trackService.searchByTitle(title, pageable), assembler));
    }

    @Operation(
            summary = "Lista as faixas de um álbum (paginado)",
            description = "Consulta personalizada: retorna as faixas do álbum informado (relação 1:N). Use sort=trackNumber,asc para a ordem do disco."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK – página com as faixas do álbum (vazia se ele não tiver nenhuma)"),
            @ApiResponse(responseCode = "400", description = "Bad Request – id do álbum inválido (não numérico)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Id inválido", value = ErrorExamples.BAD_REQUEST_ID))),
            @ApiResponse(responseCode = "404", description = "Not Found – nenhum álbum com o id informado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Álbum inexistente", value = ErrorExamples.NOT_FOUND_ALBUM)))
    })
    @GetMapping("/by-album/{albumId}")
    public ResponseEntity<PagedModel<EntityModel<TrackResponse>>> findByAlbum(
            @Parameter(description = "Id do álbum", example = "1")
            @PathVariable Long albumId,
            @ParameterObject @PageableDefault(size = 10, sort = "trackNumber") Pageable pageable) {
        return ResponseEntity.ok(pagedAssembler.toModel(trackService.findByAlbum(albumId, pageable), assembler));
    }

    @Operation(
            summary = "Lista as playlists que contêm a faixa (paginado)",
            description = "Consulta personalizada: retorna as playlists em que a faixa informada aparece (relação N:N)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK – página com as playlists da faixa (vazia se ela não estiver em nenhuma)"),
            @ApiResponse(responseCode = "400", description = "Bad Request – id da faixa inválido (não numérico)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Id inválido", value = ErrorExamples.BAD_REQUEST_ID))),
            @ApiResponse(responseCode = "404", description = "Not Found – nenhuma faixa com o id informado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Faixa inexistente", value = ErrorExamples.NOT_FOUND_TRACK)))
    })
    @GetMapping("/{id}/playlists")
    public ResponseEntity<PagedModel<EntityModel<PlaylistResponse>>> findPlaylists(
            @Parameter(description = "Id da faixa", example = "1")
            @PathVariable Long id,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedPlaylistAssembler.toModel(trackService.findPlaylists(id, pageable), playlistAssembler));
    }

    @Operation(
            summary = "Busca uma faixa pelo id",
            description = "Retorna os dados de uma única faixa a partir do seu identificador, com links para o álbum e as playlists."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK – faixa encontrada"),
            @ApiResponse(responseCode = "400", description = "Bad Request – id inválido (não numérico)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Id inválido", value = ErrorExamples.BAD_REQUEST_ID))),
            @ApiResponse(responseCode = "404", description = "Not Found – nenhuma faixa com o id informado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Não encontrada", value = ErrorExamples.NOT_FOUND_TRACK)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<TrackResponse>> findById(
            @Parameter(description = "Id da faixa", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(assembler.toModel(trackService.findById(id)));
    }

    @Operation(
            summary = "Cadastra uma nova faixa",
            description = "Cria uma faixa em um álbum existente. Obrigatórios: albumId, title (1 a 150 caracteres) e trackNumber (1 a 99, único dentro do álbum). "
                    + "Opcional: durationSeconds (1 a 7200)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created – faixa criada; o cabeçalho Location aponta para o novo recurso"),
            @ApiResponse(responseCode = "400", description = "Bad Request – dados inválidos (título em branco ou com símbolos proibidos, número da faixa ou duração fora do intervalo...) ou JSON malformado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(name = "Dados inválidos", value = ErrorExamples.BAD_REQUEST_VALIDATION),
                                    @ExampleObject(name = "JSON malformado", value = ErrorExamples.BAD_REQUEST_MALFORMED)
                            })),
            @ApiResponse(responseCode = "404", description = "Not Found – o álbum informado em albumId não existe",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Álbum inexistente", value = ErrorExamples.NOT_FOUND_ALBUM))),
            @ApiResponse(responseCode = "409", description = "Conflict – o álbum já tem uma faixa com esse número",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Número duplicado", value = ErrorExamples.CONFLICT_TRACK)))
    })
    @PostMapping
    public ResponseEntity<EntityModel<TrackResponse>> create(@RequestBody @Valid TrackRequest request) {
        EntityModel<TrackResponse> model = assembler.toModel(trackService.create(request));
        return ResponseEntity.created(model.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(model);
    }

    @Operation(
            summary = "Atualiza uma faixa",
            description = "Substitui todos os dados de uma faixa existente (inclusive o álbum). As mesmas regras de validação do cadastro se aplicam."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK – faixa atualizada"),
            @ApiResponse(responseCode = "400", description = "Bad Request – dados inválidos, id não numérico ou JSON malformado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Dados inválidos", value = ErrorExamples.BAD_REQUEST_VALIDATION))),
            @ApiResponse(responseCode = "404", description = "Not Found – nenhuma faixa com o id informado, ou álbum de albumId inexistente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(name = "Faixa inexistente", value = ErrorExamples.NOT_FOUND_TRACK),
                                    @ExampleObject(name = "Álbum inexistente", value = ErrorExamples.NOT_FOUND_ALBUM)
                            })),
            @ApiResponse(responseCode = "409", description = "Conflict – o álbum já tem outra faixa com esse número",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Número duplicado", value = ErrorExamples.CONFLICT_TRACK)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<TrackResponse>> update(
            @Parameter(description = "Id da faixa a atualizar", example = "1")
            @PathVariable Long id,
            @RequestBody @Valid TrackRequest request) {
        return ResponseEntity.ok(assembler.toModel(trackService.update(id, request)));
    }

    @Operation(
            summary = "Remove uma faixa",
            description = "Exclui a faixa e a tira de todas as playlists em que estava."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "No Content – faixa removida (sem corpo na resposta)"),
            @ApiResponse(responseCode = "400", description = "Bad Request – id inválido (não numérico)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Id inválido", value = ErrorExamples.BAD_REQUEST_ID))),
            @ApiResponse(responseCode = "404", description = "Not Found – nenhuma faixa com o id informado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Não encontrada", value = ErrorExamples.NOT_FOUND_TRACK)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Id da faixa a remover", example = "1")
            @PathVariable Long id) {
        trackService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
