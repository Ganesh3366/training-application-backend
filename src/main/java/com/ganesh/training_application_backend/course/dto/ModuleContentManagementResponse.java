package com.ganesh.training_application_backend.course.dto;

import com.ganesh.training_application_backend.course.ModuleContentType;

public record ModuleContentManagementResponse(
		Long id,
		ModuleContentType type,
		String title,
		String textContent,
		String videoUrl,
		Integer position) {
}
