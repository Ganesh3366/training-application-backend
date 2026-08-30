package com.ganesh.training_application_backend.course.dto;

public record AnswerOptionManagementResponse(Long id, String optionText, boolean correct, Integer position) {
}
