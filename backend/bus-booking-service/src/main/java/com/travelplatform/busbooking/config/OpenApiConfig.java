package com.travelplatform.busbooking.config;

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
	public OpenAPI busBookingOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Bus Booking Service API")
						.description("Bus search, ticket booking, and admin management of buses/drivers. " +
								"Part of the Travel Platform microservices — auth is handled by auth-service.")
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
