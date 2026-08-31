package com.ganesh.training_application_backend.admin.dto;

import java.time.Instant;

import com.ganesh.training_application_backend.course.dto.CourseManagementResponse;

public record CourseAssignmentResponse(Long id, CourseManagementResponse course, Instant assignedAt) {
}
