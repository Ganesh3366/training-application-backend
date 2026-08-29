package com.ganesh.training_application_backend.course.dto;

import java.util.List;

public record CourseProgressResponse(
		Long courseId,
		int totalModules,
		int completedModules,
		int pendingModules,
		List<ModuleProgressResponse> modules) {
}
