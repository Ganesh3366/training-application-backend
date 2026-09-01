package com.ganesh.training_application_backend.reporting.dto;

import java.time.Instant;

public record LearnerModuleReportResponse(
		Long moduleId,
		String moduleTitle,
		boolean completed,
		Integer lastScore,
		Integer bestScore,
		int attemptCount,
		Instant completedAt) {
}
