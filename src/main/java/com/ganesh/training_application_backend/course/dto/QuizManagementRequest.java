package com.ganesh.training_application_backend.course.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QuizManagementRequest(@NotBlank String title,
		@NotNull @Min(0) @Max(100) Integer passingScore) {
}
