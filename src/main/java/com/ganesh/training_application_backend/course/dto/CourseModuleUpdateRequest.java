package com.ganesh.training_application_backend.course.dto;

import jakarta.validation.constraints.NotBlank;

public record CourseModuleUpdateRequest(@NotBlank String title, String description) {
}
