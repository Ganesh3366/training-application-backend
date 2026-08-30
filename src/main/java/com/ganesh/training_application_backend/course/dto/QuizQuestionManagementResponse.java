package com.ganesh.training_application_backend.course.dto;

import java.util.List;

public record QuizQuestionManagementResponse(Long id, String questionText, Integer position,
		List<AnswerOptionManagementResponse> options) {
}
