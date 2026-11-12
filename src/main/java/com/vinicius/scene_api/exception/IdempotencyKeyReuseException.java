package com.vinicius.scene_api.exception;

/**
 * A mesma chave de idempotência chegou de novo, mas em uma requisição diferente da original:
 * vira 422 Unprocessable Content.
 */
public class IdempotencyKeyReuseException extends RuntimeException {

    public IdempotencyKeyReuseException(String message) {
        super(message);
    }
}
