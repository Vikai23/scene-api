package com.vinicius.scene_api.dto;

import com.vinicius.scene_api.model.Album;
import com.vinicius.scene_api.model.AlbumType;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "albums", itemRelation = "album")
@Schema(description = "Álbum retornado pela API")
public record AlbumResponse(

        @Schema(description = "Identificador único, gerado pelo banco", example = "1")
        Long id,

        @Schema(description = "Título do álbum", example = "Sempiternal")
        String title,

        @Schema(description = "Ano de lançamento", example = "2013")
        Integer releaseYear,

        @Schema(description = "Formato do lançamento", example = "ALBUM")
        AlbumType type,

        @Schema(description = "Gravadora", example = "SideOneDummy Records")
        String label,

        @Schema(description = "Id do artista dono do álbum", example = "1")
        Long artistId,

        @Schema(description = "Nome do artista dono do álbum", example = "Bring Me the Horizon")
        String artistName
) {
    public static AlbumResponse from(Album album) {
        return new AlbumResponse(album.getId(), album.getTitle(), album.getReleaseYear(), album.getType(),
                album.getLabel(), album.getArtist().getId(), album.getArtist().getName());
    }
}
