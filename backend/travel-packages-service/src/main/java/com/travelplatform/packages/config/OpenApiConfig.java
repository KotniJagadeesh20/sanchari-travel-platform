package com.travelplatform.packages.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI packagesOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Travel Packages & Destinations Service API")
                        .description("Destination discovery (browse, search, filter) and admin-curated travel " +
                                "packages with itinerary, inclusions/exclusions, and capacity-enforced booking. " +
                                "Consolidated into one service since packages reference destinations on nearly " +
                                "every read — see ARCHITECTURE.md for the bounded-context reasoning. " +
                                "Part of the Travel Platform microservices.")
                        .version("v1.0"))
                .addSecurityItem(new SecurityRequirement().addList(SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SCHEME_NAME, new SecurityScheme()
                                .name(SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
