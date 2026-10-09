package com.vinicius.scene_api.dto;

import com.vinicius.scene_api.model.Artist;
import com.vinicius.scene_api.model.Genre;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "artists", itemRelation = "artist")
@Schema(description = "Artista retornado pela API")
public record ArtistResponse(

        @Schema(description = "Identificador único, gerado pelo banco", example = "1")
        Long id,

        @Schema(description = "Nome do artista ou banda", example = "Bring Me the Horizon")
        String name,

        @Schema(description = "Gênero principal", example = "METALCORE")
        Genre genre,

        @Schema(description = "País de origem", example = "United Kingdom")
        String country,

        @Schema(description = "Ano de formação da banda ou, em artista solo, de início da carreira", example = "2004")
        Integer formedYear
) {
    public static ArtistResponse from(Artist artist) {
        return new ArtistResponse(artist.getId(), artist.getName(), artist.getGenre(),
                artist.getCountry(), artist.getFormedYear());
    }
}
