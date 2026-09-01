package com.ganesh.training_application_backend.reporting.dto;

import java.time.LocalDate;
import java.util.List;

import com.ganesh.training_application_backend.course.dto.CourseProgressStatus;

public record LearnerCourseReportResponse(
		Long learnerId,
		String learnerName,
		String learnerEmail,
		Long courseId,
		String courseTitle,
		int completedModules,
		int totalModules,
		int pendingModules,
		int progressPercentage,
		CourseProgressStatus status,
		LocalDate completionDate,
		String certificateNumber,
		List<LearnerModuleReportResponse> modules) {
}
