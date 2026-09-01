package com.ganesh.training_application_backend.admin.dto;

import com.ganesh.training_application_backend.auth.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminUserUpdateRequest(
		@NotBlank @Size(max = 100) String name,
		@NotBlank @Email @Size(max = 254) String email,
		@NotNull Role role) {
}
