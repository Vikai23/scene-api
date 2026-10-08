package com.vinicius.scene_api.dto;

import com.vinicius.scene_api.model.Visibility;
import com.vinicius.scene_api.validation.Text;
import com.vinicius.scene_api.validation.ValidationRules;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Dados para cadastrar ou substituir uma playlist. As faixas são gerenciadas em /playlists/{playlistId}/tracks/{trackId}.")
public record PlaylistRequest(

        @Schema(description = "Nome da playlist (único, sem diferenciar maiúsculas de minúsculas). "
                + "De 2 a 100 caracteres, com ao menos uma letra; aceita números e pontuação comum.",
                example = "Road Trip Mix", requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 2, maxLength = 100, pattern = ValidationRules.NAME)
        @NotBlank
        @Size(min = 2, max = 100)
        @Pattern(regexp = ValidationRules.NAME, message = ValidationRules.NAME_MESSAGE)
        String name,

        @Schema(description = "Descrição (opcional), até 255 caracteres", example = "Músicas para viagens longas",
                maxLength = 255)
        @Size(max = 255)
        String description,

        @Schema(description = "Quem pode ver a playlist. Valores aceitos: PUBLIC, PRIVATE",
                example = "PUBLIC", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Visibility visibility
) {
    public PlaylistRequest {
        name = Text.trim(name);
        description = Text.trimToNull(description);
    }
}
