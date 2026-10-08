package com.vinicius.scene_api.dto;

import com.vinicius.scene_api.model.AlbumType;
import com.vinicius.scene_api.validation.Text;
import com.vinicius.scene_api.validation.ValidationRules;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Dados para cadastrar ou substituir um álbum")
public record AlbumRequest(

        @Schema(description = "Id do artista dono do álbum (o artista precisa existir)",
                example = "1", requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1")
        @NotNull
        @Positive
        Long artistId,

        @Schema(description = "Título do álbum. De 1 a 150 caracteres: letras, números, espaços e pontuação comum "
                + "(títulos numéricos como \"1989\" são válidos). Não pode repetir dentro do mesmo artista.",
                example = "That's the Spirit", requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 1, maxLength = 150, pattern = ValidationRules.TITLE)
        @NotBlank
        @Size(min = 1, max = 150)
        @Pattern(regexp = ValidationRules.TITLE, message = ValidationRules.TITLE_MESSAGE)
        String title,

        @Schema(description = "Ano de lançamento, de 1900 a 2100", example = "2015",
                requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1900", maximum = "2100")
        @NotNull
        @Min(1900)
        @Max(2100)
        Integer releaseYear,

        @Schema(description = "Formato do lançamento. Valores aceitos: ALBUM, EP, SINGLE, COMPILATION, LIVE",
                example = "ALBUM", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        AlbumType type,

        @Schema(description = "Gravadora (opcional). De 2 a 100 caracteres, com ao menos uma letra.",
                example = "RCA Records", minLength = 2, maxLength = 100, pattern = ValidationRules.NAME)
        @Size(min = 2, max = 100)
        @Pattern(regexp = ValidationRules.NAME, message = ValidationRules.NAME_MESSAGE)
        String label
) {
    public AlbumRequest {
        title = Text.trim(title);
        label = Text.trimToNull(label);
    }
}
