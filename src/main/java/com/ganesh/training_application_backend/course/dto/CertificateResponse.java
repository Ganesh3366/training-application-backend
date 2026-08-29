package com.ganesh.training_application_backend.course.dto;

import java.time.LocalDate;

public record CertificateResponse(
		String certificateNumber,
		String participantName,
		String courseName,
		LocalDate completionDate,
		int finalScore) {
}
