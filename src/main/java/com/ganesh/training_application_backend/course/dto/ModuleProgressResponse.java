package com.ganesh.training_application_backend.course.dto;

import java.time.Instant;

public record ModuleProgressResponse(
		Long moduleId,
		boolean completed,
		int attemptsCount,
		Integer lastScore,
		Integer bestScore,
		Instant completedAt) {
}
