package com.vinicius.scene_api.controller;

import com.vinicius.scene_api.assembler.PlaylistModelAssembler;
import com.vinicius.scene_api.assembler.TrackModelAssembler;
import com.vinicius.scene_api.dto.ErrorExamples;
import com.vinicius.scene_api.dto.PlaylistRequest;
import com.vinicius.scene_api.dto.PlaylistResponse;
import com.vinicius.scene_api.dto.TrackResponse;
import com.vinicius.scene_api.exception.ApiError;
import com.vinicius.scene_api.model.Visibility;
import com.vinicius.scene_api.service.PlaylistService;
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

import java.net.URI;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@RequestMapping("/playlists")
@Tag(name = "Playlists", description = "Cadastro de playlists e das faixas que fazem parte de cada uma. Relação N:N: uma playlist tem várias faixas e uma faixa pode estar em várias playlists.")
public class PlaylistController {

    private final PlaylistService playlistService;
    private final PlaylistModelAssembler assembler;
    private final TrackModelAssembler trackAssembler;
    private final PagedResourcesAssembler<PlaylistResponse> pagedAssembler;
    private final PagedResourcesAssembler<TrackResponse> pagedTrackAssembler;

    public PlaylistController(PlaylistService playlistService,
                              PlaylistModelAssembler assembler,
                              TrackModelAssembler trackAssembler,
                              PagedResourcesAssembler<PlaylistResponse> pagedAssembler,
                              PagedResourcesAssembler<TrackResponse> pagedTrackAssembler) {
        this.playlistService = playlistService;
        this.assembler = assembler;
        this.trackAssembler = trackAssembler;
        this.pagedAssembler = pagedAssembler;
        this.pagedTrackAssembler = pagedTrackAssembler;
    }

    @Operation(
            summary = "Lista as playlists (paginado)",
            description = "Retorna uma página com as playlists cadastradas. Paginação via page (começa em 0), size (padrão 10, máximo 50) "
                    + "e sort (ex.: sort=name,asc). Campos de ordenação: id, name, description, visibility."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK – página de playlists retornada (vazia se não houver nenhuma cadastrada)"),
            @ApiResponse(responseCode = "400", description = "Bad Request – campo de ordenação inexistente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Ordenação inválida", value = ErrorExamples.BAD_REQUEST_SORT)))
    })
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<PlaylistResponse>>> findAll(
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedAssembler.toModel(playlistService.findAll(pageable), assembler));
    }

    @Operation(
            summary = "Busca playlists pelo nome (paginado)",
            description = "Consulta personalizada: retorna as playlists cujo nome contém o texto informado, sem diferenciar "
                    + "maiúsculas de minúsculas. Ex.: name=rotation encontra 'Heavy Rotation'."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK – página com as playlists encontradas (vazia se nenhuma bater com o texto)"),
            @ApiResponse(responseCode = "400", description = "Bad Request – parâmetro 'name' ausente, em branco ou com mais de 100 caracteres",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(name = "Parâmetro ausente", value = ErrorExamples.BAD_REQUEST_PARAM_MISSING),
                                    @ExampleObject(name = "Parâmetro em branco", value = ErrorExamples.BAD_REQUEST_PARAM_BLANK)
                            }))
    })
    @GetMapping("/search")
    public ResponseEntity<PagedModel<EntityModel<PlaylistResponse>>> searchByName(
            @Parameter(description = "Parte do nome da playlist (de 1 a 100 caracteres)", example = "rotation")
            @RequestParam @NotBlank @Size(max = 100) String name,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedAssembler.toModel(playlistService.searchByName(name, pageable), assembler));
    }

    @Operation(
            summary = "Lista as playlists por visibilidade (paginado)",
            description = "Consulta personalizada: retorna as playlists PUBLIC ou PRIVATE."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK – página com as playlists da visibilidade (vazia se não houver nenhuma)"),
            @ApiResponse(responseCode = "400", description = "Bad Request – visibilidade inexistente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Visibilidade inválida", value = ErrorExamples.BAD_REQUEST_ENUM_VISIBILITY)))
    })
    @GetMapping("/by-visibility/{visibility}")
    public ResponseEntity<PagedModel<EntityModel<PlaylistResponse>>> findByVisibility(
            @Parameter(description = "Visibilidade da playlist", example = "PUBLIC")
            @PathVariable Visibility visibility,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedAssembler.toModel(playlistService.findByVisibility(visibility, pageable), assembler));
    }

    @Operation(
            summary = "Lista as faixas de uma playlist (paginado)",
            description = "Consulta personalizada: retorna as faixas que fazem parte da playlist informada (relação N:N)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK – página com as faixas da playlist (vazia se ela não tiver nenhuma)"),
            @ApiResponse(responseCode = "400", description = "Bad Request – id da playlist inválido (não numérico)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Id inválido", value = ErrorExamples.BAD_REQUEST_ID))),
            @ApiResponse(responseCode = "404", description = "Not Found – nenhuma playlist com o id informado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Playlist inexistente", value = ErrorExamples.NOT_FOUND_PLAYLIST)))
    })
    @GetMapping("/{id}/tracks")
    public ResponseEntity<PagedModel<EntityModel<TrackResponse>>> findTracks(
            @Parameter(description = "Id da playlist", example = "1")
            @PathVariable Long id,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedTrackAssembler.toModel(playlistService.findTracks(id, pageable), trackAssembler));
    }

    @Operation(
            summary = "Busca uma playlist pelo id",
            description = "Retorna os dados de uma única playlist a partir do seu identificador, com a quantidade de faixas e o link para elas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK – playlist encontrada"),
            @ApiResponse(responseCode = "400", description = "Bad Request – id inválido (não numérico)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Id inválido", value = ErrorExamples.BAD_REQUEST_ID))),
            @ApiResponse(responseCode = "404", description = "Not Found – nenhuma playlist com o id informado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Não encontrada", value = ErrorExamples.NOT_FOUND_PLAYLIST)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<PlaylistResponse>> findById(
            @Parameter(description = "Id da playlist", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(assembler.toModel(playlistService.findById(id)));
    }

    @Operation(
            summary = "Cadastra uma nova playlist",
            description = "Cria uma playlist vazia. Nome (2 a 100 caracteres, único) e visibilidade são obrigatórios; descrição (até 255 caracteres) é opcional. "
                    + "Para colocar faixas nela, use POST /playlists/{playlistId}/tracks/{trackId}."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created – playlist criada; o cabeçalho Location aponta para o novo recurso"),
            @ApiResponse(responseCode = "400", description = "Bad Request – dados inválidos (nome em branco ou fora de 2 a 100 caracteres, descrição longa demais, visibilidade inexistente) ou JSON malformado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(name = "Dados inválidos", value = ErrorExamples.BAD_REQUEST_VALIDATION),
                                    @ExampleObject(name = "JSON malformado", value = ErrorExamples.BAD_REQUEST_MALFORMED)
                            })),
            @ApiResponse(responseCode = "409", description = "Conflict – já existe uma playlist com esse nome",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Nome duplicado", value = ErrorExamples.CONFLICT_PLAYLIST)))
    })
    @PostMapping
    public ResponseEntity<EntityModel<PlaylistResponse>> create(@RequestBody @Valid PlaylistRequest request) {
        EntityModel<PlaylistResponse> model = assembler.toModel(playlistService.create(request));
        return ResponseEntity.created(model.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(model);
    }

    @Operation(
            summary = "Atualiza uma playlist",
            description = "Substitui nome, descrição e visibilidade de uma playlist existente. As faixas dela não mudam."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK – playlist atualizada"),
            @ApiResponse(responseCode = "400", description = "Bad Request – dados inválidos, id não numérico ou JSON malformado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Dados inválidos", value = ErrorExamples.BAD_REQUEST_VALIDATION))),
            @ApiResponse(responseCode = "404", description = "Not Found – nenhuma playlist com o id informado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Não encontrada", value = ErrorExamples.NOT_FOUND_PLAYLIST))),
            @ApiResponse(responseCode = "409", description = "Conflict – já existe outra playlist com esse nome",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Nome duplicado", value = ErrorExamples.CONFLICT_PLAYLIST)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<PlaylistResponse>> update(
            @Parameter(description = "Id da playlist a atualizar", example = "1")
            @PathVariable Long id,
            @RequestBody @Valid PlaylistRequest request) {
        return ResponseEntity.ok(assembler.toModel(playlistService.update(id, request)));
    }

    @Operation(
            summary = "Remove uma playlist",
            description = "Exclui a playlist. As faixas continuam cadastradas; só deixam de fazer parte dela."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "No Content – playlist removida (sem corpo na resposta)"),
            @ApiResponse(responseCode = "400", description = "Bad Request – id inválido (não numérico)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Id inválido", value = ErrorExamples.BAD_REQUEST_ID))),
            @ApiResponse(responseCode = "404", description = "Not Found – nenhuma playlist com o id informado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Não encontrada", value = ErrorExamples.NOT_FOUND_PLAYLIST)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Id da playlist a remover", example = "1")
            @PathVariable Long id) {
        playlistService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Adiciona uma faixa à playlist",
            description = "Coloca uma faixa existente na playlist (relação N:N). Devolve a playlist atualizada; o cabeçalho Location aponta para a lista de faixas dela."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created – faixa adicionada à playlist"),
            @ApiResponse(responseCode = "400", description = "Bad Request – algum id inválido (não numérico)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Id inválido", value = ErrorExamples.BAD_REQUEST_ID))),
            @ApiResponse(responseCode = "404", description = "Not Found – a playlist ou a faixa não existe",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(name = "Playlist inexistente", value = ErrorExamples.NOT_FOUND_PLAYLIST),
                                    @ExampleObject(name = "Faixa inexistente", value = ErrorExamples.NOT_FOUND_TRACK)
                            })),
            @ApiResponse(responseCode = "409", description = "Conflict – a faixa já está na playlist",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Faixa repetida", value = ErrorExamples.CONFLICT_TRACK_IN_PLAYLIST)))
    })
    @PostMapping("/{playlistId}/tracks/{trackId}")
    public ResponseEntity<EntityModel<PlaylistResponse>> addTrack(
            @Parameter(description = "Id da playlist", example = "1")
            @PathVariable Long playlistId,
            @Parameter(description = "Id da faixa a adicionar", example = "5")
            @PathVariable Long trackId) {
        EntityModel<PlaylistResponse> model = assembler.toModel(playlistService.addTrack(playlistId, trackId));
        URI location = linkTo(methodOn(PlaylistController.class).findTracks(playlistId, Pageable.unpaged())).toUri();
        return ResponseEntity.created(location).body(model);
    }

    @Operation(
            summary = "Remove uma faixa da playlist",
            description = "Tira a faixa da playlist. A faixa continua cadastrada e em outras playlists."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "No Content – faixa removida da playlist (sem corpo na resposta)"),
            @ApiResponse(responseCode = "400", description = "Bad Request – algum id inválido (não numérico)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Id inválido", value = ErrorExamples.BAD_REQUEST_ID))),
            @ApiResponse(responseCode = "404", description = "Not Found – a playlist ou a faixa não existe, ou a faixa não está na playlist",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(name = "Playlist inexistente", value = ErrorExamples.NOT_FOUND_PLAYLIST),
                                    @ExampleObject(name = "Faixa fora da playlist", value = ErrorExamples.NOT_FOUND_TRACK_IN_PLAYLIST)
                            }))
    })
    @DeleteMapping("/{playlistId}/tracks/{trackId}")
    public ResponseEntity<Void> removeTrack(
            @Parameter(description = "Id da playlist", example = "1")
            @PathVariable Long playlistId,
            @Parameter(description = "Id da faixa a remover", example = "5")
            @PathVariable Long trackId) {
        playlistService.removeTrack(playlistId, trackId);
        return ResponseEntity.noContent().build();
    }
}
