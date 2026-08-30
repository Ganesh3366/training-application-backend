package com.ganesh.training_application_backend.course.dto;

import java.util.List;

public record QuizManagementResponse(Long id, String title, Integer passingScore,
		List<QuizQuestionManagementResponse> questions) {
}
