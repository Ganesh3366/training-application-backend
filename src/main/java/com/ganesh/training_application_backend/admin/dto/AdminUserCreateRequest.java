package com.ganesh.training_application_backend.admin.dto;

import com.ganesh.training_application_backend.auth.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminUserCreateRequest(
		@NotBlank @Size(max = 100) String firstName,
		@NotBlank @Size(max = 100) String lastName,
		@NotBlank @Email @Size(max = 254) String email,
		@NotNull String password,
		@NotNull Role role) {

	@JsonIgnore
	@AssertTrue(message = "password must be between 8 and 72 nonblank characters")
	public boolean isPasswordValid() {
		return password != null && !password.isBlank() && password.length() >= 8 && password.length() <= 72;
	}

	@Override
	public String toString() {
		return "AdminUserCreateRequest[firstName=" + firstName + ", lastName=" + lastName + ", email=" + email
				+ ", password=[REDACTED], role=" + role + "]";
	}
}
