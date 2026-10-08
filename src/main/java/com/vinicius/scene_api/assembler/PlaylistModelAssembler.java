package com.vinicius.scene_api.assembler;

import com.vinicius.scene_api.controller.PlaylistController;
import com.vinicius.scene_api.dto.PlaylistResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Transforma a playlist em EntityModel com os links HATEOAS:
 * self, update, delete, a coleção e as faixas da playlist.
 */
@Component
public class PlaylistModelAssembler implements RepresentationModelAssembler<PlaylistResponse, EntityModel<PlaylistResponse>> {

    @Override
    public EntityModel<PlaylistResponse> toModel(PlaylistResponse playlist) {
        return EntityModel.of(playlist,
                linkTo(methodOn(PlaylistController.class).findById(playlist.id())).withSelfRel(),
                linkTo(methodOn(PlaylistController.class).update(playlist.id(), null)).withRel("update"),
                linkTo(methodOn(PlaylistController.class).delete(playlist.id())).withRel("delete"),
                linkTo(methodOn(PlaylistController.class).findAll(Pageable.unpaged())).withRel("playlists"),
                linkTo(methodOn(PlaylistController.class).findTracks(playlist.id(), Pageable.unpaged())).withRel("tracks"));
    }
}
