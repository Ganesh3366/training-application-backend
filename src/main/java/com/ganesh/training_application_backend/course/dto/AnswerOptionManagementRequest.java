package com.ganesh.training_application_backend.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AnswerOptionManagementRequest(@NotBlank String optionText, @NotNull Boolean correct) {
}
