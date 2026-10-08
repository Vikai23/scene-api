package com.vinicius.scene_api.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Formato padrão de todas as respostas de erro da API")
public record ApiError(

        @Schema(description = "Código de status HTTP", example = "404")
        int status,

        @Schema(description = "Nome oficial do status HTTP, em inglês", example = "Not Found")
        String error,

        @Schema(description = "Explicação do problema", example = "Artista não encontrado(a) com id: 99")
        String message,

        @Schema(description = "Rota que foi chamada", example = "/artists/99")
        String path,

        @Schema(description = "Momento em que o erro aconteceu", example = "2026-10-09T10:15:30")
        LocalDateTime timestamp,

        @Schema(description = "Problemas campo a campo. Só vem preenchida nos erros de validação; nos demais erros é uma lista vazia.")
        List<FieldIssue> details
) {

    @Schema(description = "Problema encontrado em um campo ou parâmetro específico")
    public record FieldIssue(

            @Schema(description = "Campo ou parâmetro com problema", example = "name")
            String field,

            @Schema(description = "O que está errado nesse campo", example = "tamanho deve ser entre 2 e 100")
            String message
    ) {
    }
}
