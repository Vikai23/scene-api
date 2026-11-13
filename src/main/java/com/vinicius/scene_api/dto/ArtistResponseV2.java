package com.vinicius.scene_api.dto;

import com.vinicius.scene_api.model.Artist;
import com.vinicius.scene_api.model.Genre;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

/**
 * Representação do artista na versão 2 da API (header {@code X-API-Version: 2}).
 * <p>
 * Em relação à versão 1 ({@link ArtistResponse}): o país e o ano de formação saem do nível principal e
 * passam para o objeto {@code origin}, e entram os campos {@code albumCount} e {@code hasProfile}.
 * Mover campos de lugar quebraria quem já consome a versão 1, por isso a mudança ficou numa versão nova.
 */
@Relation(collectionRelation = "artists", itemRelation = "artist")
@Schema(description = "Artista retornado pela versão 2 da API (header X-API-Version: 2)")
public record ArtistResponseV2(

        @Schema(description = "Identificador único, gerado pelo banco", example = "1")
        Long id,

        @Schema(description = "Nome do artista ou banda", example = "Bring Me the Horizon")
        String name,

        @Schema(description = "Gênero principal", example = "METALCORE")
        Genre genre,

        @Schema(description = "Origem do artista. Na versão 1 esses dados ficam soltos em country e formedYear")
        Origin origin,

        @Schema(description = "Quantidade de álbuns cadastrados do artista (campo novo da versão 2)", example = "2")
        long albumCount,

        @Schema(description = "Indica se o artista já tem perfil cadastrado (campo novo da versão 2)", example = "true")
        boolean hasProfile
) {

    @Schema(name = "ArtistOrigin", description = "País e época de origem do artista")
    public record Origin(

            @Schema(description = "País de origem (nulo se não informado)", example = "United Kingdom")
            String country,

            @Schema(description = "Ano de formação da banda ou, em artista solo, de início da carreira (nulo se não informado)",
                    example = "2004")
            Integer formedYear
    ) {
    }

    public static ArtistResponseV2 from(Artist artist, long albumCount, boolean hasProfile) {
        return new ArtistResponseV2(artist.getId(), artist.getName(), artist.getGenre(),
                new Origin(artist.getCountry(), artist.getFormedYear()), albumCount, hasProfile);
    }
}
