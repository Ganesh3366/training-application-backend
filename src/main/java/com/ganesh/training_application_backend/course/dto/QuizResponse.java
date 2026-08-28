package com.ganesh.training_application_backend.course.dto;

import java.util.List;

public record QuizResponse(
		Long id,
		String title,
		Integer passingScore,
		List<QuizQuestionResponse> questions) {
}
