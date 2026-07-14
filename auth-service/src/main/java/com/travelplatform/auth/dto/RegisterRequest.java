package com.travelplatform.auth.dto;

import java.util.Date;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO used for both /userRegister and /registerAdmin.
 * Keeps the entity (and its password field) out of the controller signature.
 */
@Schema(description = "Registration payload for new users and admins")
public class RegisterRequest {

	@NotBlank(message = "Name is required")
	@Schema(description = "Full name", example = "Asha Kumar") private String name;

	@NotBlank(message = "Email is required")
	@Email(message = "Email should be valid")
	@Schema(description = "Unique email address", example = "asha@example.com") private String email;

	@NotBlank(message = "Password is required")
	@Pattern(
		regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$",
		message = "Password must be at least 8 characters long and contain at least one uppercase letter, one lowercase letter, one digit, and one special character."
	)
	@Schema(description = "Min 8 chars with upper, lower, digit, special char", example = "Passw0rd!") private String password;

	@NotBlank(message = "Phone number is required")
	@Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone number must be a valid 10-digit number")
	@Schema(description = "10-digit Indian mobile number starting with 6-9", example = "9876543210") private String phone;

	@NotNull(message = "Date of birth is required")
	@Schema(description = "Date of birth", example = "1995-06-15") private Date dob;

	@NotBlank(message = "Gender is required")
	@Schema(description = "Gender", example = "Female") private String gender;

	@Min(value = 1, message = "Age must be a positive number")
	@Schema(description = "Age in years", example = "29") private int age;

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

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public Date getDob() {
		return dob;
	}

	public void setDob(Date dob) {
		this.dob = dob;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}
}
