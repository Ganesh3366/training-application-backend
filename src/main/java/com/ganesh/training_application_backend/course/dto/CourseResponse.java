package com.ganesh.training_application_backend.course.dto;

public record CourseResponse(
		Long id,
		String title,
		String description,
		String instructor,
		Integer duration,
		String level,
		String category) {
}
