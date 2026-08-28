package com.ganesh.training_application_backend.course.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record QuizSubmissionRequest(
		@NotNull List<@Valid QuizAnswerRequest> answers) {
}
