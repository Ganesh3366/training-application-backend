package com.ganesh.training_application_backend.auth.dto;

import com.ganesh.training_application_backend.auth.Role;

public record UserResponse(Long id, String name, String email, Role role) {
}
