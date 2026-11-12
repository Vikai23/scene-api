package com.vinicius.scene_api.idempotency;

import org.springframework.http.HttpHeaders;

/**
 * Resultado de uma operação protegida por idempotência.
 *
 * @param body     resposta da operação (a de agora ou a original, guardada)
 * @param keyUsed  {@code true} se o cliente mandou o cabeçalho X-Idempotency-Key
 * @param replayed {@code true} se a operação não rodou de novo e a resposta é a original
 */
public record IdempotentResult<T>(T body, boolean keyUsed, boolean replayed) {

    /**
     * Cabeçalhos a acrescentar na resposta: quando o cliente usa a chave, {@code X-Idempotent-Replayed}
     * diz se a resposta é repetida ({@code true}) ou se a operação acabou de ser processada ({@code false}).
     */
    public HttpHeaders responseHeaders() {
        HttpHeaders headers = new HttpHeaders();
        if (keyUsed) {
            headers.set(IdempotencyService.REPLAYED_HEADER, String.valueOf(replayed));
        }
        return headers;
    }
}
