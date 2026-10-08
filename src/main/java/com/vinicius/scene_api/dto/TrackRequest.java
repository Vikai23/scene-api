package com.vinicius.scene_api.dto;

import com.vinicius.scene_api.validation.Text;
import com.vinicius.scene_api.validation.ValidationRules;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Dados para cadastrar ou substituir uma faixa")
public record TrackRequest(

        @Schema(description = "Id do álbum da faixa (o álbum precisa existir)",
                example = "8", requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1")
        @NotNull
        @Positive
        Long albumId,

        @Schema(description = "Título da faixa. De 1 a 150 caracteres: letras, números, espaços e pontuação comum "
                + "(títulos numéricos como \"27\" são válidos).",
                example = "Intro", requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 1, maxLength = 150, pattern = ValidationRules.TITLE)
        @NotBlank
        @Size(min = 1, max = 150)
        @Pattern(regexp = ValidationRules.TITLE, message = ValidationRules.TITLE_MESSAGE)
        String title,

        @Schema(description = "Número da faixa dentro do álbum, de 1 a 99. Não pode repetir dentro do mesmo álbum.",
                example = "1", requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "99")
        @NotNull
        @Min(1)
        @Max(99)
        Integer trackNumber,

        @Schema(description = "Duração em segundos (opcional), de 1 a 7200", example = "95",
                minimum = "1", maximum = "7200")
        @Min(1)
        @Max(7200)
        Integer durationSeconds
) {
    public TrackRequest {
        title = Text.trim(title);
    }
}
