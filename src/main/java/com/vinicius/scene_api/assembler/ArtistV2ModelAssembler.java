package com.vinicius.scene_api.assembler;

import com.vinicius.scene_api.controller.AlbumController;
import com.vinicius.scene_api.controller.ArtistController;
import com.vinicius.scene_api.controller.ArtistProfileController;
import com.vinicius.scene_api.dto.ArtistResponseV2;
import org.springframework.data.domain.Pageable;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Transforma o artista da versão 2 em EntityModel. Os links HATEOAS são os mesmos da versão 1,
 * porque as rotas não mudam entre as versões: a versão vai no header, não no endereço.
 */
@Component
public class ArtistV2ModelAssembler implements RepresentationModelAssembler<ArtistResponseV2, EntityModel<ArtistResponseV2>> {

    @Override
    public EntityModel<ArtistResponseV2> toModel(ArtistResponseV2 artist) {
        return EntityModel.of(artist,
                linkTo(methodOn(ArtistController.class).findById(artist.id())).withSelfRel(),
                linkTo(methodOn(ArtistController.class).update(artist.id(), null)).withRel("update"),
                linkTo(methodOn(ArtistController.class).delete(artist.id())).withRel("delete"),
                linkTo(methodOn(ArtistController.class).findAll(Pageable.unpaged())).withRel("artists"),
                linkTo(methodOn(AlbumController.class).findByArtist(artist.id(), Pageable.unpaged())).withRel("albums"),
                linkTo(methodOn(ArtistProfileController.class).findByArtist(artist.id())).withRel("profile"));
    }
}
