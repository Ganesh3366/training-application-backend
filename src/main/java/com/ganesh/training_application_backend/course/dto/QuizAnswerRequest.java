package com.ganesh.training_application_backend.course.dto;

import jakarta.validation.constraints.NotNull;

public record QuizAnswerRequest(
		@NotNull Long questionId,
		@NotNull Long optionId) {
}
