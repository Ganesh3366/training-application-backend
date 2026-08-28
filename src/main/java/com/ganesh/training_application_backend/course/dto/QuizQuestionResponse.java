package com.ganesh.training_application_backend.course.dto;

import java.util.List;

public record QuizQuestionResponse(
		Long id,
		String questionText,
		Integer position,
		List<AnswerOptionResponse> options) {
}
