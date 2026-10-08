package com.vinicius.scene_api.exception;

/**
 * Regra de negócio violada por dado duplicado ou já existente: vira 409 Conflict.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
