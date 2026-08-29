package com.ganesh.training_application_backend.course.dto;

import com.ganesh.training_application_backend.course.ModuleContentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ModuleContentUpdateRequest(
		@NotNull ModuleContentType type,
		@NotBlank String title,
		String textContent,
		String videoUrl) {
}
