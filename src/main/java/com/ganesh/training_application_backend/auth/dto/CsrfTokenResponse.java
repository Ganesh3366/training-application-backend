package com.ganesh.training_application_backend.auth.dto;

public record CsrfTokenResponse(String token, String headerName, String parameterName) {
}
