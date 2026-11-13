package com.vinicius.scene_api.dto;

import com.vinicius.scene_api.apikey.ApiKey;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

import java.time.LocalDateTime;

@Relation(collectionRelation = "apiKeys", itemRelation = "apiKey")
@Schema(description = "Chave de API, sem o valor completo (a API só guarda o hash da chave)")
public record ApiKeyResponse(
        @Schema(description = "Identificador único, gerado pelo banco", example = "1")
        Long id,
        @Schema(description = "Nome de quem usa a chave", example = "Vinícius")
        String owner,
        @Schema(description = "Começo da chave, para reconhecê-la sem mostrar o valor inteiro", example = "scn_q8Xv2LmA")
        String prefix,
        @Schema(description = "Se a chave ainda abre as rotas protegidas (false depois de revogada)", example = "true")
        boolean active,
        @Schema(description = "Quando a chave foi gerada", example = "2026-11-13T00:50:12")
        LocalDateTime createdAt
) {
    public static ApiKeyResponse from(ApiKey key) {
        return new ApiKeyResponse(key.getId(), key.getOwner(), key.getPrefix(), key.isActive(), key.getCreatedAt());
    }
}
