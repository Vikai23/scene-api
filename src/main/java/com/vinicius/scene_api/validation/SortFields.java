package com.vinicius.scene_api.validation;

import com.vinicius.scene_api.exception.BadRequestException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/**
 * Campos que podem ser usados no parâmetro {@code sort} de cada recurso (os mesmos listados no Swagger).
 * Qualquer outro campo, ou uma opção que a API não oferece, é recusado com 400 Bad Request antes de a
 * consulta chegar ao banco.
 */
public final class SortFields {

    private SortFields() {
    }

    public static final Set<String> ARTIST = Set.of("id", "name", "genre", "country", "formedYear");
    public static final Set<String> ARTIST_PROFILE = Set.of("id", "hometown", "website", "instagram");
    public static final Set<String> ALBUM = Set.of("id", "title", "releaseYear", "type", "label");
    public static final Set<String> TRACK = Set.of("id", "title", "trackNumber", "durationSeconds");
    public static final Set<String> PLAYLIST = Set.of("id", "name", "description", "visibility");

    /** Confere se todos os campos pedidos em {@code sort} estão na lista permitida do recurso. */
    public static void check(Pageable pageable, Set<String> allowed) {
        for (Sort.Order order : pageable.getSort()) {
            if (!allowed.contains(order.getProperty())) {
                throw new BadRequestException("Campo de ordenação inexistente: " + order.getProperty());
            }
            // sort=campo,asc,ignorecase é uma opção do Spring Data que não funciona em campo numérico
            if (order.isIgnoreCase()) {
                throw new BadRequestException("Opção de ordenação não suportada: ignorecase. Use campo,asc ou campo,desc");
            }
        }
    }
}
