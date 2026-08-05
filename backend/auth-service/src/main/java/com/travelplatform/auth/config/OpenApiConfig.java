package com.travelplatform.auth.config;

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
	public OpenAPI busTicketBookingOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Bus Ticket Booking API")
						.description("REST API for searching buses, booking tickets, and managing buses/drivers/users")
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
