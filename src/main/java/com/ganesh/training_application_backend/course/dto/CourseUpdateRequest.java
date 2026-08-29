package com.ganesh.training_application_backend.course.dto;

import com.ganesh.training_application_backend.course.CourseCategory;
import com.ganesh.training_application_backend.course.CourseLevel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CourseUpdateRequest(
		@NotBlank String title,
		@NotBlank String description,
		@NotBlank String instructor,
		@NotNull @Positive Integer duration,
		@NotNull CourseLevel level,
		@NotNull CourseCategory category) {
}
