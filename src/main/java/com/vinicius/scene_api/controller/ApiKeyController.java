package com.vinicius.scene_api.controller;

import com.vinicius.scene_api.apikey.ApiKey;
import com.vinicius.scene_api.apikey.ApiKeyService;
import com.vinicius.scene_api.apikey.RequiresApiKey;
import com.vinicius.scene_api.assembler.ApiKeyModelAssembler;
import com.vinicius.scene_api.dto.ApiKeyCreatedResponse;
import com.vinicius.scene_api.dto.ApiKeyRequest;
import com.vinicius.scene_api.dto.ApiKeyResponse;
import com.vinicius.scene_api.dto.ErrorExamples;
import com.vinicius.scene_api.exception.ApiError;
import com.vinicius.scene_api.idempotency.IdempotencyService;
import com.vinicius.scene_api.idempotency.IdempotentResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api-keys")
@Tag(name = "Chaves de API", description = "Geração e gerenciamento das chaves de API. As rotas que alteram dados "
        + "(POST, PUT e DELETE) exigem uma chave válida no header X-API-Key; as consultas (GET) são abertas.")
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final IdempotencyService idempotencyService;
    private final ApiKeyModelAssembler assembler;

    public ApiKeyController(ApiKeyService apiKeyService,
                            IdempotencyService idempotencyService,
                            ApiKeyModelAssembler assembler) {
        this.apiKeyService = apiKeyService;
        this.idempotencyService = idempotencyService;
        this.assembler = assembler;
    }

    @Operation(
            summary = "Gera uma chave de API",
            description = "Cria uma chave nova para o usuário informado em owner. A chave completa vem só nesta resposta, "
                    + "no campo key: a API guarda apenas o hash dela (SHA-256). Para usar, envie a chave no header X-API-Key "
                    + "(no Swagger UI, botão Authorize). Esta rota é aberta: não precisa de chave."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Created – chave gerada. O campo key traz o valor completo, que não aparece de novo"),
            @ApiResponse(responseCode = "400", description = "Bad Request – owner em branco, fora de 2 a 100 caracteres ou sem nenhuma letra, ou JSON malformado",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = {
                                    @ExampleObject(name = "Dados inválidos", value = ErrorExamples.BAD_REQUEST_API_KEY_OWNER),
                                    @ExampleObject(name = "JSON malformado", value = ErrorExamples.BAD_REQUEST_MALFORMED_API_KEY)
                            }))
    })
    @PostMapping
    public ResponseEntity<EntityModel<ApiKeyCreatedResponse>> create(
            @RequestHeader(name = IdempotencyService.HEADER, required = false) String idempotencyKey,
            @RequestBody @Valid ApiKeyRequest request) {
        IdempotentResult<ApiKeyCreatedResponse> result = idempotencyService.execute(idempotencyKey, "POST /api-keys",
                request, ApiKeyCreatedResponse.class, () -> apiKeyService.create(request));
        EntityModel<ApiKeyCreatedResponse> model = assembler.toModel(result.body());
        return ResponseEntity.created(model.getRequiredLink(IanaLinkRelations.SELF).toUri())
                .headers(result.responseHeaders())
                .body(model);
    }

    @Operation(
            summary = "Mostra a chave enviada",
            description = "Retorna os dados da chave enviada no header X-API-Key: dono, prefixo, se está ativa e quando foi gerada. "
                    + "O valor completo não aparece, porque a API só guarda o hash da chave."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK – dados da chave enviada")
    })
    @RequiresApiKey
    @GetMapping("/me")
    public ResponseEntity<EntityModel<ApiKeyResponse>> me(
            @Parameter(hidden = true) @RequestAttribute(ApiKeyService.REQUEST_ATTRIBUTE) ApiKey key) {
        return ResponseEntity.ok(assembler.toModel(apiKeyService.describe(key)));
    }

    @Operation(
            summary = "Revoga a chave enviada",
            description = "Desativa a chave enviada no header X-API-Key. Depois disso ela não abre mais as rotas protegidas: "
                    + "qualquer uso devolve 401. Para continuar, gere outra em POST /api-keys."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "No Content – chave revogada (sem corpo na resposta)")
    })
    @RequiresApiKey
    @DeleteMapping("/me")
    public ResponseEntity<Void> revoke(
            @Parameter(hidden = true) @RequestAttribute(ApiKeyService.REQUEST_ATTRIBUTE) ApiKey key) {
        apiKeyService.revoke(key);
        return ResponseEntity.noContent().build();
    }
}
