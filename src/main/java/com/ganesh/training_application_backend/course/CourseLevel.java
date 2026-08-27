package com.ganesh.training_application_backend.course;

public enum CourseLevel {
	BEGINNER("Beginner"),
	INTERMEDIATE("Intermediate"),
	ADVANCED("Advanced");

	private final String displayValue;

	CourseLevel(String displayValue) {
		this.displayValue = displayValue;
	}

	public String getDisplayValue() {
		return displayValue;
	}
}
