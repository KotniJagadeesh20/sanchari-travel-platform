package com.travelplatform.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Login credentials")
public class LoginRequest {

	@NotBlank(message = "Email is required")
	@Email(message = "Email should be valid")
	@Schema(description = "Registered email address", example = "john@example.com") private String email;

	@NotBlank(message = "Password is required")
	@Schema(description = "Account password", example = "Passw0rd!") private String password;

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
