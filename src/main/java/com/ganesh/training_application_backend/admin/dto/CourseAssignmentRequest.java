package com.ganesh.training_application_backend.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CourseAssignmentRequest(
		@NotNull @Positive Long courseId) {
}
