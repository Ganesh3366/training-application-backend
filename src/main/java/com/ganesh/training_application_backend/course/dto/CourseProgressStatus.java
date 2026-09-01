package com.ganesh.training_application_backend.course.dto;

public enum CourseProgressStatus {
	NOT_STARTED,
	IN_PROGRESS,
	COMPLETED;

	public static CourseProgressStatus from(int completedModules, int totalModules, boolean hasLearnerActivity) {
		if (totalModules == 0) {
			return NOT_STARTED;
		}
		if (completedModules == totalModules) {
			return COMPLETED;
		}
		return completedModules > 0 || hasLearnerActivity ? IN_PROGRESS : NOT_STARTED;
	}
}
