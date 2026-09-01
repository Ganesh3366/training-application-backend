package com.ganesh.training_application_backend.admin.dto;

import jakarta.validation.constraints.NotNull;

public record AdminUserEnabledRequest(@NotNull Boolean enabled) {
}
