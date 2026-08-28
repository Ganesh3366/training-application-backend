package com.ganesh.training_application_backend.course.dto;

import java.util.List;

public record CourseModuleResponse(
		Long id,
		String title,
		String description,
		Integer position,
		List<ModuleContentResponse> contents) {
}
