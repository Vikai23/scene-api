package com.vinicius.scene_api.dto;

import com.vinicius.scene_api.apikey.ApiKey;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

import java.time.LocalDateTime;

/**
 * Resposta de POST /api-keys. É a única que traz a chave completa ({@code key}): depois disso a API só tem o hash.
 */
@Relation(collectionRelation = "apiKeys", itemRelation = "apiKey")
@Schema(description = "Chave de API recém-gerada. O campo key só aparece nesta resposta")
public record ApiKeyCreatedResponse(
        @Schema(description = "Identificador único, gerado pelo banco", example = "1")
        Long id,
        @Schema(description = "Nome de quem usa a chave", example = "Vinícius")
        String owner,
        @Schema(description = "A chave completa, para enviar no header X-API-Key. Só aparece nesta resposta: guarde agora",
                example = "scn_q8Xv2LmA9fKd0pRt7wYz3bNc5hJe1uSg")
        String key,
        @Schema(description = "Começo da chave, para reconhecê-la depois sem mostrar o valor inteiro", example = "scn_q8Xv2LmA")
        String prefix,
        @Schema(description = "Se a chave abre as rotas protegidas (true ao ser gerada)", example = "true")
        boolean active,
        @Schema(description = "Quando a chave foi gerada", example = "2026-11-13T00:50:12")
        LocalDateTime createdAt
) {
    public static ApiKeyCreatedResponse from(ApiKey saved, String key) {
        return new ApiKeyCreatedResponse(saved.getId(), saved.getOwner(), key, saved.getPrefix(), saved.isActive(),
                saved.getCreatedAt());
    }
}
