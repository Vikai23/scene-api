package com.vinicius.scene_api.dto;

import com.vinicius.scene_api.model.Genre;
import com.vinicius.scene_api.validation.Text;
import com.vinicius.scene_api.validation.ValidationRules;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Dados para cadastrar ou substituir um artista")
public record ArtistRequest(

        @Schema(description = "Nome do artista ou banda (único, sem diferenciar maiúsculas de minúsculas). "
                + "De 2 a 100 caracteres, com ao menos uma letra; aceita números e pontuação comum.",
                example = "Sleeping with Sirens", requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 2, maxLength = 100, pattern = ValidationRules.NAME)
        @NotBlank
        @Size(min = 2, max = 100)
        @Pattern(regexp = ValidationRules.NAME, message = ValidationRules.NAME_MESSAGE)
        String name,

        @Schema(description = "Gênero principal. Valores aceitos: ROCK, ALTERNATIVE, PUNK, EMO, POST_HARDCORE, "
                + "METALCORE, METAL, POP, HIP_HOP, ELECTRONIC, JAZZ, FOLK, CLASSICAL, OTHER",
                example = "POST_HARDCORE", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Genre genre,

        @Schema(description = "País de origem (opcional). De 2 a 60 caracteres, somente letras, espaços e os símbolos . , ' - "
                + "(números não são aceitos).",
                example = "United States", minLength = 2, maxLength = 60, pattern = ValidationRules.LETTERS_ONLY)
        @Size(min = 2, max = 60)
        @Pattern(regexp = ValidationRules.LETTERS_ONLY, message = ValidationRules.LETTERS_ONLY_MESSAGE)
        String country,

        @Schema(description = "Ano de formação (opcional), de 1900 a 2100", example = "2009",
                minimum = "1900", maximum = "2100")
        @Min(1900)
        @Max(2100)
        Integer formedYear
) {
    public ArtistRequest {
        name = Text.trim(name);
        country = Text.trimToNull(country);
    }
}
