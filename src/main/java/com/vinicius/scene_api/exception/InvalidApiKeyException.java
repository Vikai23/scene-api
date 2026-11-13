package com.vinicius.scene_api.exception;

/**
 * Rota protegida chamada sem a chave de API, com uma chave que não existe ou com uma chave revogada:
 * vira 401 Unauthorized no tratamento global de erros.
 */
public class InvalidApiKeyException extends RuntimeException {

    public InvalidApiKeyException(String message) {
        super(message);
    }
}
