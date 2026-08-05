package com.travelplatform.busbooking.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

/**
 * DTO for /api/user/bookticket/{busId}/{userId}.
 * Bus and UserAdmin are resolved server-side from the path variables
 * instead of being bound directly from the request body/path.
 */
@Schema(description = "Passenger details for booking a bus ticket")
public class BookingRequest {

	@NotBlank(message = "Name is required")
	@Schema(description = "Passenger full name", example = "Ravi Teja") private String name;

	@NotBlank(message = "Email is required")
	@Email(message = "Email should be valid")
	@Schema(description = "Passenger email", example = "ravi@example.com") private String email;

	@NotBlank(message = "Phone number is required")
	@Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone number must be a valid 10-digit number")
	@Schema(description = "10-digit mobile number", example = "9876543210") private String phoneno;

	@Min(value = 1, message = "Age must be a positive number")
	@Schema(description = "Passenger age", example = "32") private int age;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhoneno() {
		return phoneno;
	}

	public void setPhoneno(String phoneno) {
		this.phoneno = phoneno;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}
}
