package com.vinicius.scene_api.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Collections;
import java.util.List;

/**
 * A API só responde em JSON. Este interceptor confere o header {@code Accept} antes de a requisição
 * chegar ao controller: se o cliente só aceita outro formato (por exemplo application/xml), a resposta é
 * 406 Not Acceptable e nada é executado.
 * <p>
 * Sem essa checagem o Spring só perceberia o problema na hora de escrever a resposta, ou seja, depois de um
 * POST já ter gravado o registro: o cliente receberia um erro, mas o dado teria sido criado.
 */
@Component
public class AcceptHeaderInterceptor implements HandlerInterceptor {

    /** Formatos que a API produz: JSON comum e JSON com links HATEOAS (HAL). */
    private static final List<MediaType> PRODUCED = List.of(MediaType.APPLICATION_JSON, MediaTypes.HAL_JSON);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws HttpMediaTypeNotAcceptableException {
        List<String> accept = Collections.list(request.getHeaders(HttpHeaders.ACCEPT));
        if (accept.isEmpty()) {
            return true; // sem o header, o cliente aceita qualquer formato
        }
        if (!acceptsJson(accept)) {
            throw new HttpMediaTypeNotAcceptableException(PRODUCED);
        }
        return true;
    }

    private static boolean acceptsJson(List<String> accept) {
        List<MediaType> wanted;
        try {
            wanted = MediaType.parseMediaTypes(accept);
        } catch (InvalidMediaTypeException ex) {
            return false; // valor que nem é um tipo de mídia
        }
        if (wanted.isEmpty()) {
            return true; // header em branco vale como ausente
        }
        // Accept: */* e application/* também servem, porque são compatíveis com application/json
        return wanted.stream().anyMatch(type -> PRODUCED.stream().anyMatch(type::isCompatibleWith));
    }
}
