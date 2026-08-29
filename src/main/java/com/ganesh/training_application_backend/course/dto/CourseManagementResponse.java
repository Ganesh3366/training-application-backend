package com.ganesh.training_application_backend.course.dto;

import com.ganesh.training_application_backend.course.CourseCategory;
import com.ganesh.training_application_backend.course.CourseLevel;

public record CourseManagementResponse(
		Long id,
		String title,
		String description,
		String instructor,
		Integer duration,
		CourseLevel level,
		CourseCategory category) {
}
