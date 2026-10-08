package com.vinicius.scene_api.validation;

/**
 * Utilitários para limpar textos recebidos antes de validar e salvar.
 */
public final class Text {

    private Text() {
    }

    /** Remove espaços nas pontas; mantém {@code null}. */
    public static String trim(String value) {
        return value == null ? null : value.trim();
    }

    /** Remove espaços nas pontas e transforma texto vazio em {@code null} (campo opcional não informado). */
    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
