package com.vinicius.scene_api.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * O springdoc não tem uma propriedade para CSS próprio, então este transformador intercepta o
 * index.html do Swagger UI e injeta o favicon da API e o tema em /swagger-theme.css
 * (arquivo em src/main/resources/static).
 */
public class SwaggerCssInjector extends SwaggerIndexPageTransformer {

    /**
     * O Swagger UI liga o modo escuro sozinho quando o sistema operacional está no escuro. O tema desta API
     * é fixo (papel claro, tinta preta), então este script desfaz isso assim que a classe aparece no html.
     */
    private static final String KEEP_THEME_FIXED = "<script>(function(){var h=document.documentElement;"
            + "new MutationObserver(function(){if(h.classList.contains('dark-mode')){h.classList.remove('dark-mode');}})"
            + ".observe(h,{attributes:true,attributeFilter:['class']});})();</script>";

    public SwaggerCssInjector(SwaggerUiConfigProperties swaggerUiConfig,
                              SwaggerUiOAuthProperties swaggerUiOAuthProperties,
                              SwaggerWelcomeCommon swaggerWelcomeCommon,
                              ObjectMapperProvider objectMapperProvider) {
        super(swaggerUiConfig, swaggerUiOAuthProperties, swaggerWelcomeCommon, objectMapperProvider);
    }

    @Override
    public Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain transformer)
            throws IOException {
        if ("index.html".equals(resource.getFilename())) {
            try (InputStream in = resource.getInputStream()) {
                String html = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                String themed = html
                        .replace("<title>Swagger UI</title>", "<title>Scene API</title>")
                        // remove os favicons padrão do Swagger e usa o disco de vinil com estrela da API
                        .replaceAll("<link[^>]*rel=\"icon\"[^>]*>", "")
                        .replace("</head>",
                                "<link rel=\"icon\" type=\"image/svg+xml\" href=\"/music-logo.svg\" />"
                                        + "<link rel=\"stylesheet\" type=\"text/css\" href=\"/swagger-theme.css\" />"
                                        + KEEP_THEME_FIXED + "</head>");
                return new TransformedResource(resource, themed.getBytes(StandardCharsets.UTF_8));
            }
        }
        return super.transform(request, resource, transformer);
    }
}
