package com.vinicius.scene_api.dto;

import com.vinicius.scene_api.model.Track;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "tracks", itemRelation = "track")
@Schema(description = "Faixa retornada pela API")
public record TrackResponse(

        @Schema(description = "Identificador único, gerado pelo banco", example = "1")
        Long id,

        @Schema(description = "Título da faixa", example = "Can You Feel My Heart")
        String title,

        @Schema(description = "Número da faixa dentro do álbum", example = "1")
        Integer trackNumber,

        @Schema(description = "Duração em segundos (pode ser nula se não informada)", example = "228")
        Integer durationSeconds,

        @Schema(description = "Id do álbum da faixa", example = "1")
        Long albumId,

        @Schema(description = "Título do álbum da faixa", example = "Sempiternal")
        String albumTitle,

        @Schema(description = "Nome do artista dono do álbum", example = "Bring Me the Horizon")
        String artistName
) {
    public static TrackResponse from(Track track) {
        return new TrackResponse(track.getId(), track.getTitle(), track.getTrackNumber(), track.getDurationSeconds(),
                track.getAlbum().getId(), track.getAlbum().getTitle(), track.getAlbum().getArtist().getName());
    }
}
