package com.ganesh.training_application_backend.course;

public enum CourseCategory {
	INFORMATION_TECHNOLOGY("Information Technology (IT)"),
	HEALTH("Health"),
	BUSINESS("Business"),
	SALES_MARKETING("Sales & Marketing"),
	MANAGEMENT("Management"),
	ENGINEERING("Engineering"),
	ELECTRICAL_ELECTRONICS("Electrical & Electronics"),
	ARTIFICIAL_INTELLIGENCE("Artificial Intelligence (AI)"),
	FINANCE("Finance"),
	AGRICULTURE("Agriculture");

	private final String displayValue;

	CourseCategory(String displayValue) {
		this.displayValue = displayValue;
	}

	public String getDisplayValue() {
		return displayValue;
	}
}
