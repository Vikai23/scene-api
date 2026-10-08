package com.vinicius.scene_api.controller;

import com.vinicius.scene_api.assembler.ArtistProfileModelAssembler;
import com.vinicius.scene_api.dto.ArtistProfileRequest;
import com.vinicius.scene_api.dto.ArtistProfileResponse;
import com.vinicius.scene_api.dto.ErrorExamples;
import com.vinicius.scene_api.exception.ApiError;
import com.vinicius.scene_api.service.ArtistProfileService;
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
@RequestMapping("/artist-profiles")
@Tag(name = "Perfis de Artista", description = "Perfil detalhado de um artista (biografia, cidade natal, site e Instagram). Relação 1:1: cada artista tem no máximo um perfil.")
public class ArtistProfileController {

    private final ArtistProfileService profileService;
    private final ArtistProfileModelAssembler assembler;
    private final PagedResourcesAssembler<ArtistProfileResponse> pagedAssembler;

    public ArtistProfileController(ArtistProfileService profileService,
                                   ArtistProfileModelAssembler assembler,
                                   PagedResourcesAssembler<ArtistProfileResponse> pagedAssembler) {
        this.profileService = profileService;
        this.assembler = assembler;
        this.pagedAssembler = pagedAssembler;
    }

    @Operation(
            summary = "Lista os perfis de artista (paginado)",
            description = "Retorna uma página com os perfis cadastrados. Paginação via page (começa em 0), size (padrão 10, máximo 50) "
                    + "e sort (ex.: sort=hometown,asc). Campos de ordenação: id, hometown, website, instagram."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK – página de perfis retornada (vazia se não houver nenhum cadastrado)"),
            @ApiResponse(responseCode = "400", description = "Bad Request – campo de ordenação inexistente",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Ordenação inválida", value = ErrorExamples.BAD_REQUEST_SORT)))
    })
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<ArtistProfileResponse>>> findAll(
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedAssembler.toModel(profileService.findAll(pageable), assembler));
    }

    @Operation(
            summary = "Busca perfis pela cidade natal (paginado)",
            description = "Consulta personalizada: retorna os perfis cuja cidade natal contém o texto informado, sem diferenciar "
                    + "maiúsculas de minúsculas. Ex.: hometown=england encontra 'Sheffield, England' e 'York, England'."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK – página com os perfis encontrados (vazia se nenhum bater com o texto)"),
            @ApiResponse(responseCode = "400", description = "Bad Request – parâmetro 'hometown' ausente, em branco ou com mais de 100 caracteres",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(name = "Parâmetro ausente", value = ErrorExamples.BAD_REQUEST_PARAM_MISSING),
                                    @ExampleObject(name = "Parâmetro em branco", value = ErrorExamples.BAD_REQUEST_PARAM_BLANK)
                            }))
    })
    @GetMapping("/search")
    public ResponseEntity<PagedModel<EntityModel<ArtistProfileResponse>>> searchByHometown(
            @Parameter(description = "Parte da cidade natal (de 1 a 100 caracteres)", example = "england")
            @RequestParam @NotBlank @Size(max = 100) String hometown,
            @ParameterObject @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(pagedAssembler.toModel(profileService.searchByHometown(hometown, pageable), assembler));
    }

    @Operation(
            summary = "Busca o perfil de um artista",
            description = "Consulta personalizada: retorna o perfil do artista cujo id foi informado (relação 1:1)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK – perfil do artista encontrado"),
            @ApiResponse(responseCode = "400", description = "Bad Request – id do artista inválido (não numérico)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Id inválido", value = ErrorExamples.BAD_REQUEST_ID))),
            @ApiResponse(responseCode = "404", description = "Not Found – o artista não existe ou ainda não tem perfil",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(name = "Artista sem perfil", value = ErrorExamples.NOT_FOUND_PROFILE_OF_ARTIST),
                                    @ExampleObject(name = "Artista inexistente", value = ErrorExamples.NOT_FOUND_ARTIST)
                            }))
    })
    @GetMapping("/by-artist/{artistId}")
    public ResponseEntity<EntityModel<ArtistProfileResponse>> findByArtist(
            @Parameter(description = "Id do artista dono do perfil", example = "1")
            @PathVariable Long artistId) {
        return ResponseEntity.ok(assembler.toModel(profileService.findByArtist(artistId)));
    }

    @Operation(
            summary = "Busca um perfil pelo id",
            description = "Retorna os dados de um único perfil a partir do seu identificador."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK – perfil encontrado"),
            @ApiResponse(responseCode = "400", description = "Bad Request – id inválido (não numérico)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Id inválido", value = ErrorExamples.BAD_REQUEST_ID))),
            @ApiResponse(responseCode = "404", description = "Not Found – nenhum perfil com o id informado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Não encontrado", value = ErrorExamples.NOT_FOUND_PROFILE)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<ArtistProfileResponse>> findById(
            @Parameter(description = "Id do perfil", example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(assembler.toModel(profileService.findById(id)));
    }

    @Operation(
            summary = "Cadastra o perfil de um artista",
            description = "Cria o perfil de um artista que ainda não tem um. Obrigatórios: artistId (o artista precisa existir) e biography (10 a 1000 caracteres). "
                    + "Opcionais: hometown (somente letras), website (http:// ou https://) e instagram (sem o @)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created – perfil criado; o cabeçalho Location aponta para o novo recurso"),
            @ApiResponse(responseCode = "400", description = "Bad Request – dados inválidos (biografia curta demais, cidade com números, site sem http/https, Instagram inválido...) ou JSON malformado",
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
            @ApiResponse(responseCode = "409", description = "Conflict – o artista já possui um perfil",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Perfil duplicado", value = ErrorExamples.CONFLICT_PROFILE)))
    })
    @PostMapping
    public ResponseEntity<EntityModel<ArtistProfileResponse>> create(@RequestBody @Valid ArtistProfileRequest request) {
        EntityModel<ArtistProfileResponse> model = assembler.toModel(profileService.create(request));
        return ResponseEntity.created(model.getRequiredLink(IanaLinkRelations.SELF).toUri()).body(model);
    }

    @Operation(
            summary = "Atualiza um perfil",
            description = "Substitui os dados de um perfil existente. O artistId enviado precisa ser o mesmo do perfil: um perfil não muda de artista."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK – perfil atualizado"),
            @ApiResponse(responseCode = "400", description = "Bad Request – dados inválidos, tentativa de trocar o artista do perfil, id não numérico ou JSON malformado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(name = "Dados inválidos", value = ErrorExamples.BAD_REQUEST_VALIDATION),
                                    @ExampleObject(name = "Troca de artista", value = ErrorExamples.BAD_REQUEST_PROFILE_ARTIST)
                            })),
            @ApiResponse(responseCode = "404", description = "Not Found – nenhum perfil com o id informado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Não encontrado", value = ErrorExamples.NOT_FOUND_PROFILE)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<EntityModel<ArtistProfileResponse>> update(
            @Parameter(description = "Id do perfil a atualizar", example = "1")
            @PathVariable Long id,
            @RequestBody @Valid ArtistProfileRequest request) {
        return ResponseEntity.ok(assembler.toModel(profileService.update(id, request)));
    }

    @Operation(
            summary = "Remove um perfil",
            description = "Exclui o perfil. O artista continua cadastrado e pode receber um novo perfil depois."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "No Content – perfil removido (sem corpo na resposta)"),
            @ApiResponse(responseCode = "400", description = "Bad Request – id inválido (não numérico)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Id inválido", value = ErrorExamples.BAD_REQUEST_ID))),
            @ApiResponse(responseCode = "404", description = "Not Found – nenhum perfil com o id informado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(name = "Não encontrado", value = ErrorExamples.NOT_FOUND_PROFILE)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Id do perfil a remover", example = "1")
            @PathVariable Long id) {
        profileService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
