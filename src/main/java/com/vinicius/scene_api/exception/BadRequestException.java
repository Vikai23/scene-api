package com.vinicius.scene_api.exception;

/**
 * Requisição que não respeita uma regra da API: vira 400 Bad Request.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
