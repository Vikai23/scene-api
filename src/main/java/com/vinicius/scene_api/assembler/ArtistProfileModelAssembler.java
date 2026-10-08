package com.vinicius.scene_api.assembler;

import com.vinicius.scene_api.controller.ArtistController;
import com.vinicius.scene_api.controller.ArtistProfileController;
import com.vinicius.scene_api.dto.ArtistProfileResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Transforma o perfil em EntityModel com os links HATEOAS:
 * self, update, delete, a coleção e o artista dono do perfil.
 */
@Component
public class ArtistProfileModelAssembler
        implements RepresentationModelAssembler<ArtistProfileResponse, EntityModel<ArtistProfileResponse>> {

    @Override
    public EntityModel<ArtistProfileResponse> toModel(ArtistProfileResponse profile) {
        return EntityModel.of(profile,
                linkTo(methodOn(ArtistProfileController.class).findById(profile.id())).withSelfRel(),
                linkTo(methodOn(ArtistProfileController.class).update(profile.id(), null)).withRel("update"),
                linkTo(methodOn(ArtistProfileController.class).delete(profile.id())).withRel("delete"),
                linkTo(methodOn(ArtistProfileController.class).findAll(Pageable.unpaged())).withRel("artist-profiles"),
                linkTo(methodOn(ArtistController.class).findById(profile.artistId())).withRel("artist"));
    }
}
