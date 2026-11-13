package com.vinicius.scene_api.apikey;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca as rotas que exigem uma chave de API válida no header {@code X-API-Key} (quem confere é o
 * {@link ApiKeyInterceptor}).
 * <p>
 * Na Scene API são as rotas que alteram dados (POST, PUT e DELETE dos cinco recursos) e as de gerenciar a própria
 * chave. As consultas (GET) e a geração de uma chave nova (POST /api-keys) continuam abertas.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresApiKey {
}
