package com.vinicius.scene_api.assembler;

import com.vinicius.scene_api.controller.AlbumController;
import com.vinicius.scene_api.controller.TrackController;
import com.vinicius.scene_api.dto.TrackResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Transforma a faixa em EntityModel com os links HATEOAS:
 * self, update, delete, a coleção, o álbum da faixa e as playlists que a contêm.
 */
@Component
public class TrackModelAssembler implements RepresentationModelAssembler<TrackResponse, EntityModel<TrackResponse>> {

    @Override
    public EntityModel<TrackResponse> toModel(TrackResponse track) {
        return EntityModel.of(track,
                linkTo(methodOn(TrackController.class).findById(track.id())).withSelfRel(),
                linkTo(methodOn(TrackController.class).update(track.id(), null)).withRel("update"),
                linkTo(methodOn(TrackController.class).delete(track.id())).withRel("delete"),
                linkTo(methodOn(TrackController.class).findAll(Pageable.unpaged())).withRel("tracks"),
                linkTo(methodOn(AlbumController.class).findById(track.albumId())).withRel("album"),
                linkTo(methodOn(TrackController.class).findPlaylists(track.id(), Pageable.unpaged())).withRel("playlists"));
    }
}
