package com.vinicius.scene_api.dto;

import com.vinicius.scene_api.model.Playlist;
import com.vinicius.scene_api.model.Visibility;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "playlists", itemRelation = "playlist")
@Schema(description = "Playlist retornada pela API")
public record PlaylistResponse(

        @Schema(description = "Identificador único, gerado pelo banco", example = "1")
        Long id,

        @Schema(description = "Nome da playlist", example = "Road Trip Mix")
        String name,

        @Schema(description = "Descrição da playlist", example = "Músicas para viagens longas")
        String description,

        @Schema(description = "Quem pode ver a playlist", example = "PUBLIC")
        Visibility visibility,

        @Schema(description = "Quantidade de faixas na playlist", example = "12")
        int trackCount
) {
    public static PlaylistResponse from(Playlist playlist) {
        return new PlaylistResponse(playlist.getId(), playlist.getName(), playlist.getDescription(),
                playlist.getVisibility(), playlist.getTracks().size());
    }
}
