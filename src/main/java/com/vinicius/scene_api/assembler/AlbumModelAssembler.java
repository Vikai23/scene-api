package com.vinicius.scene_api.assembler;

import com.vinicius.scene_api.controller.AlbumController;
import com.vinicius.scene_api.controller.ArtistController;
import com.vinicius.scene_api.controller.TrackController;
import com.vinicius.scene_api.dto.AlbumResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Transforma o álbum em EntityModel com os links HATEOAS:
 * self, update, delete, a coleção, o artista dono e as faixas do álbum.
 */
@Component
public class AlbumModelAssembler implements RepresentationModelAssembler<AlbumResponse, EntityModel<AlbumResponse>> {

    @Override
    public EntityModel<AlbumResponse> toModel(AlbumResponse album) {
        return EntityModel.of(album,
                linkTo(methodOn(AlbumController.class).findById(album.id())).withSelfRel(),
                linkTo(methodOn(AlbumController.class).update(album.id(), null)).withRel("update"),
                linkTo(methodOn(AlbumController.class).delete(album.id())).withRel("delete"),
                linkTo(methodOn(AlbumController.class).findAll(Pageable.unpaged())).withRel("albums"),
                linkTo(methodOn(ArtistController.class).findById(album.artistId())).withRel("artist"),
                linkTo(methodOn(TrackController.class).findByAlbum(album.id(), Pageable.unpaged())).withRel("tracks"));
    }
}
