package com.vinicius.scene_api.dto;

import com.vinicius.scene_api.validation.Text;
import com.vinicius.scene_api.validation.ValidationRules;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Dados para cadastrar ou substituir o perfil de um artista. Cada artista tem no máximo um perfil.")
public record ArtistProfileRequest(

        @Schema(description = "Id do artista dono do perfil. O artista precisa existir e ainda não ter perfil; "
                + "depois de criado, o perfil não pode mudar de artista.",
                example = "6", requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1")
        @NotNull
        @Positive
        Long artistId,

        @Schema(description = "Biografia do artista, de 10 a 1000 caracteres",
                example = "Banda de Denton, Texas, formada em 2020, que já se chamou Sw!tch e Sinema.",
                requiredMode = Schema.RequiredMode.REQUIRED, minLength = 10, maxLength = 1000)
        @NotBlank
        @Size(min = 10, max = 1000)
        String biography,

        @Schema(description = "Cidade natal (opcional). De 2 a 100 caracteres, somente letras, espaços e os símbolos . , ' - "
                + "(números não são aceitos).",
                example = "Denton, Texas", minLength = 2, maxLength = 100, pattern = ValidationRules.LETTERS_ONLY)
        @Size(min = 2, max = 100)
        @Pattern(regexp = ValidationRules.LETTERS_ONLY, message = ValidationRules.LETTERS_ONLY_MESSAGE)
        String hometown,

        @Schema(description = "Site oficial (opcional). Precisa começar com http:// ou https://; até 200 caracteres.",
                example = "https://www.example.com", maxLength = 200, pattern = ValidationRules.URL)
        @Size(max = 200)
        @Pattern(regexp = ValidationRules.URL, message = ValidationRules.URL_MESSAGE)
        String website,

        @Schema(description = "Usuário do Instagram, sem o @ (opcional). Até 30 caracteres: letras, números, ponto e underline.",
                example = "iptw.example", maxLength = 30, pattern = ValidationRules.INSTAGRAM)
        @Pattern(regexp = ValidationRules.INSTAGRAM, message = ValidationRules.INSTAGRAM_MESSAGE)
        String instagram
) {
    public ArtistProfileRequest {
        biography = Text.trim(biography);
        hometown = Text.trimToNull(hometown);
        website = Text.trimToNull(website);
        instagram = Text.trimToNull(instagram);
    }
}
