package com.ganesh.training_application_backend.course;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ganesh.training_application_backend.auth.AppUserPrincipal;
import com.ganesh.training_application_backend.course.dto.CertificateResponse;

@RestController
@RequestMapping("/api/courses/{courseId}/certificate")
public class CertificateController {

	private final CertificateService certificateService;

	public CertificateController(CertificateService certificateService) {
		this.certificateService = certificateService;
	}

	@GetMapping
	public CertificateResponse getCertificate(@PathVariable Long courseId,
			@AuthenticationPrincipal AppUserPrincipal principal) {
		return certificateService.getOrCreateCertificate(courseId, principal.getId(), principal.getName());
	}
}
