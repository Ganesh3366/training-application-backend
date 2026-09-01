package com.ganesh.training_application_backend.course.dto;

public enum CourseProgressStatus {
	NOT_STARTED,
	IN_PROGRESS,
	COMPLETED;

	public static CourseProgressStatus from(int completedModules, int totalModules) {
		if (totalModules == 0 || completedModules == 0) {
			return NOT_STARTED;
		}
		return completedModules == totalModules ? COMPLETED : IN_PROGRESS;
	}
}
