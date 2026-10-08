package com.vinicius.scene_api.validation;

/**
 * Expressões regulares e mensagens compartilhadas entre entidades e DTOs.
 * <p>
 * As expressões não usam {@code \p{L}} para que também funcionem na validação
 * de parâmetros feita em JavaScript pelo próprio Swagger UI.
 */
public final class ValidationRules {

    private ValidationRules() {
    }

    /** Somente letras (com acentos), espaços e . , ' - (usado em país e cidade natal). */
    public static final String LETTERS_ONLY = "^[A-Za-z\\u00C0-\\u00FF .,'-]+$";

    /** Nomes de artista, playlist e gravadora: precisa ter ao menos uma letra. */
    public static final String NAME = "^(?=.*[A-Za-z\\u00C0-\\u00FF])[A-Za-z0-9\\u00C0-\\u00FF .,'&!?:;()/+*#@_-]+$";

    /** Títulos de álbum e faixa: aceita títulos numéricos como "27" ou "1989". */
    public static final String TITLE = "^[A-Za-z0-9\\u00C0-\\u00FF .,'&!?:;()/+*#@_-]+$";

    /** Endereço de site começando com http:// ou https://. */
    public static final String URL = "^https?://[A-Za-z0-9.-]+(:[0-9]{1,5})?(/[^\\s]*)?$";

    /** Usuário do Instagram, sem o @. */
    public static final String INSTAGRAM = "^[A-Za-z0-9._]{1,30}$";

    public static final String LETTERS_ONLY_MESSAGE =
            "deve conter apenas letras, espaços e os símbolos . , ' - (números não são permitidos)";
    public static final String NAME_MESSAGE =
            "deve conter ao menos uma letra e apenas letras, números, espaços e pontuação comum";
    public static final String TITLE_MESSAGE =
            "deve conter apenas letras, números, espaços e pontuação comum";
    public static final String URL_MESSAGE =
            "deve ser um endereço válido começando com http:// ou https://";
    public static final String INSTAGRAM_MESSAGE =
            "deve ter até 30 caracteres entre letras, números, ponto e underline (sem o @)";
}
