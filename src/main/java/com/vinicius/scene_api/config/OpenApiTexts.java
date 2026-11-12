package com.vinicius.scene_api.config;

import java.time.Duration;

/**
 * Textos auxiliares da documentação: escreve por extenso os intervalos configurados no application.properties,
 * para o Swagger mostrar sempre os valores que estão valendo de verdade.
 */
final class OpenApiTexts {

    private OpenApiTexts() {
    }

    /** Exemplos: 1 minuto, 30 segundos, 24 horas. */
    static String describe(Duration duration) {
        long seconds = duration.toSeconds();
        if (seconds == 0) {
            return plural(duration.toMillis(), "milissegundo", "milissegundos");
        }
        if (seconds % 3600 == 0) {
            return plural(seconds / 3600, "hora", "horas");
        }
        if (seconds % 60 == 0) {
            return plural(seconds / 60, "minuto", "minutos");
        }
        return plural(seconds, "segundo", "segundos");
    }

    private static String plural(long amount, String singular, String plural) {
        return amount + " " + (amount == 1 ? singular : plural);
    }
}
