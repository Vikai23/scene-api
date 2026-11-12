package com.vinicius.scene_api.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.cfg.EnumFeature;
import tools.jackson.databind.type.LogicalType;

/**
 * Deixa a leitura do JSON estrita: cada campo só aceita o tipo dele.
 * <p>
 * Por padrão o Jackson (biblioteca que converte JSON em objeto Java) "ajeita" valores de tipo errado por
 * conta própria: o texto "2004" vira o número 2004, o número 2004.7 vira 2004, o valor true vira o texto
 * "true" e o número 1 vira o segundo valor de um enum. Assim um dado errado entraria no banco sem aviso.
 * Com as regras abaixo, o tipo errado é recusado e o cliente recebe 400 Bad Request
 * (ver GlobalExceptionHandler.handleUnreadableBody).
 */
@Configuration
public class JacksonConfig {

    @Bean
    public JsonMapperBuilderCustomizer strictJsonTypes() {
        return builder -> builder
                // texto não vira número: "releaseYear": "2015" é recusado
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
                // número com casas decimais não vira inteiro: "releaseYear": 2015.7 é recusado
                .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
                // número não vira valor de enum: "genre": 1 é recusado
                .enable(EnumFeature.FAIL_ON_NUMBERS_FOR_ENUMS)
                // número e booleano não viram texto: "name": 123 e "name": true são recusados
                .withCoercionConfig(LogicalType.Textual, text -> text
                        .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                        .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail));
    }
}
