package com.vinicius.scene_api.dto;

import com.vinicius.scene_api.validation.ValidationRules;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para gerar uma chave de API")
public record ApiKeyRequest(

        @Schema(description = "Nome de quem vai usar a chave. De 2 a 100 caracteres, com ao menos uma letra; aceita números "
                + "e pontuação comum.",
                example = "Vinícius", requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 2, maxLength = 100, pattern = ValidationRules.NAME)
        @NotBlank
        @Size(min = 2, max = 100)
        @Pattern(regexp = ValidationRules.NAME, message = ValidationRules.NAME_MESSAGE)
        String owner
) {
}
