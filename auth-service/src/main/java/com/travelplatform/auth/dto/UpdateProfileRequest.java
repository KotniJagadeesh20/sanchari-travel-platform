package com.travelplatform.auth.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Date;

/**
 * PUT /auth/users/me payload. Deliberately excludes email and password:
 * - email is the login identifier and unique-constrained — changing it needs
 *   its own verification flow (confirm the new address), not a plain field edit.
 * - password already has (or will have) its own change-password flow with its
 *   own validation; mixing it into a general profile edit is how people end up
 *   accidentally clearing/weakening it via a generic form.
 *
 * Partial update: every field is optional. Only non-null fields are applied
 * — same pattern used for Hotel/Room/Package updates elsewhere in this
 * platform. Sending {} changes nothing.
 */
@Schema(description = "Partial profile update — only non-null fields are applied. No email/password here.")
public class UpdateProfileRequest {

    @Schema(example = "Asha Kumar")
    private String name;

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone number must be a valid 10-digit number")
    @Schema(example = "9876543210")
    private String phone;

    @Schema(example = "Female")
    private String gender;

    @Min(value = 1, message = "Age must be a positive number")
    @Schema(example = "29")
    private Integer age;

    @Schema(example = "1995-06-15")
    private Date dob;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public Date getDob() { return dob; }
    public void setDob(Date dob) { this.dob = dob; }
}
