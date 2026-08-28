package com.ganesh.training_application_backend.course.dto;

public record QuizResultResponse(
		int totalQuestions,
		int correctAnswers,
		int score,
		int passingScore,
		boolean passed) {
}
