package com.ganesh.training_application_backend.course.dto;

public record CourseModuleSummaryResponse(
		Long id,
		String title,
		String description,
		Integer position) {
}
