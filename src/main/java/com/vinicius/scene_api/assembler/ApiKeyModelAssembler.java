package com.vinicius.scene_api.assembler;

import com.vinicius.scene_api.controller.ApiKeyController;
import com.vinicius.scene_api.dto.ApiKeyCreatedResponse;
import com.vinicius.scene_api.dto.ApiKeyResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Links HATEOAS da chave de API: {@code self} (consultar) e {@code delete} (revogar). Os dois apontam para
 * /api-keys/me, porque a chave é identificada pelo header X-API-Key, e não por um id na URL.
 */
@Component
public class ApiKeyModelAssembler {

    public EntityModel<ApiKeyCreatedResponse> toModel(ApiKeyCreatedResponse key) {
        return EntityModel.of(key, links());
    }

    public EntityModel<ApiKeyResponse> toModel(ApiKeyResponse key) {
        return EntityModel.of(key, links());
    }

    private static List<Link> links() {
        return List.of(
                linkTo(methodOn(ApiKeyController.class).me(null)).withSelfRel(),
                linkTo(methodOn(ApiKeyController.class).revoke(null)).withRel("delete"));
    }
}
