package com.ganesh.training_application_backend.course;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ganesh.training_application_backend.auth.AppUser;
import com.ganesh.training_application_backend.auth.AppUserPrincipal;
import com.ganesh.training_application_backend.auth.Role;
import com.ganesh.training_application_backend.config.SecurityConfig;
import com.ganesh.training_application_backend.course.dto.CertificateResponse;

@WebMvcTest(CertificateController.class)
@Import(SecurityConfig.class)
class CertificateControllerTests {

	@MockitoBean CertificateService service;
	@Autowired MockMvc mockMvc;

	@Test
	void anonymousCertificateRequestIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/courses/1/certificate")).andExpect(status().isUnauthorized());
	}

	@Test
	void authenticatedRequestUsesPrincipalEvenWhenClientSuppliesAnotherUserId() throws Exception {
		when(service.getOrCreateCertificate(1L, 7L, "Learner Name")).thenReturn(new CertificateResponse(
				"SF-2026-ABCDEF123456", "Learner Name", "Introduction to Angular",
				LocalDate.of(2026, 8, 29), 90));

		mockMvc.perform(get("/api/courses/1/certificate")
				.queryParam("userId", "999")
				.with(user(principal())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.certificateNumber").value("SF-2026-ABCDEF123456"))
				.andExpect(jsonPath("$.participantName").value("Learner Name"))
				.andExpect(jsonPath("$.id").doesNotExist())
				.andExpect(jsonPath("$.userId").doesNotExist());
		verify(service).getOrCreateCertificate(1L, 7L, "Learner Name");
	}

	private AppUserPrincipal principal() {
		return new AppUserPrincipal(new AppUser(7L, "Learner Name", "learner@example.com", "hash", Role.USER));
	}
}
