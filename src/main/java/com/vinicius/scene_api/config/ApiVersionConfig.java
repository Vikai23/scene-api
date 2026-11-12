package com.vinicius.scene_api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Versionamento da API por cabeçalho personalizado (recurso nativo do Spring Framework 7).
 * <p>
 * O cliente escolhe a versão no cabeçalho {@code X-API-Version}. Sem o cabeçalho vale a versão 1,
 * então quem já usava a API continua funcionando. Uma versão fora da lista é recusada com 400 Bad Request.
 * Cada método de controller declara a versão que atende com {@code version = "..."} no mapeamento;
 * método sem versão atende a todas.
 */
@Configuration
public class ApiVersionConfig implements WebMvcConfigurer {

    /** Cabeçalho em que o cliente informa a versão desejada (e em que a API devolve a versão usada). */
    public static final String HEADER = "X-API-Version";

    public static final String V1 = "1";
    public static final String V2 = "2";

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer.useRequestHeader(HEADER)
                .addSupportedVersions(V1, V2)
                .setDefaultVersion(V1);
    }
}
