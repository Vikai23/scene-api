package com.vinicius.scene_api.dto;

import com.vinicius.scene_api.model.ArtistProfile;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "artistProfiles", itemRelation = "artistProfile")
@Schema(description = "Perfil de artista retornado pela API")
public record ArtistProfileResponse(

        @Schema(description = "Identificador único do perfil, gerado pelo banco", example = "1")
        Long id,

        @Schema(description = "Id do artista dono do perfil", example = "1")
        Long artistId,

        @Schema(description = "Nome do artista dono do perfil", example = "Bring Me the Horizon")
        String artistName,

        @Schema(description = "Biografia do artista", example = "Banda britânica formada em Sheffield em 2004.")
        String biography,

        @Schema(description = "Cidade natal", example = "Sheffield, England")
        String hometown,

        @Schema(description = "Site oficial", example = "https://www.bmthofficial.com")
        String website,

        @Schema(description = "Usuário do Instagram, sem o @", example = "bmthofficial")
        String instagram
) {
    public static ArtistProfileResponse from(ArtistProfile profile) {
        return new ArtistProfileResponse(profile.getId(), profile.getArtist().getId(), profile.getArtist().getName(),
                profile.getBiography(), profile.getHometown(), profile.getWebsite(), profile.getInstagram());
    }
}
