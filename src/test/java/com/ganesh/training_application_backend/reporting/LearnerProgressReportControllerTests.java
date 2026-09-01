package com.ganesh.training_application_backend.reporting;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

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
import com.ganesh.training_application_backend.course.dto.CourseProgressStatus;
import com.ganesh.training_application_backend.reporting.dto.LearnerCourseReportResponse;
import com.ganesh.training_application_backend.reporting.dto.LearnerModuleReportResponse;

@WebMvcTest(LearnerProgressReportController.class)
@Import(SecurityConfig.class)
class LearnerProgressReportControllerTests {

	@MockitoBean LearnerProgressReportService service;
	@Autowired MockMvc mockMvc;

	@Test
	void anonymousIsUnauthorizedAndLearnerIsForbidden() throws Exception {
		mockMvc.perform(get("/api/management/reports/learner-courses"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/management/reports/learner-courses").with(user(principal(Role.USER))))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminAndInstructorCanReadReportsWithoutSensitiveAuthenticationData() throws Exception {
		when(service.getReports()).thenReturn(List.of(report()));

		mockMvc.perform(get("/api/management/reports/learner-courses").with(user(principal(Role.ADMIN))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].learnerEmail").value("learner@example.com"))
				.andExpect(jsonPath("$[0].status").value("IN_PROGRESS"))
				.andExpect(jsonPath("$[0].modules[0].attemptCount").value(2))
				.andExpect(jsonPath("$[0].password").doesNotExist())
				.andExpect(jsonPath("$[0].passwordHash").doesNotExist())
				.andExpect(jsonPath("$[0].role").doesNotExist());
		mockMvc.perform(get("/api/management/reports/learner-courses").with(user(principal(Role.INSTRUCTOR))))
				.andExpect(status().isOk());
	}

	@Test
	void failedAttemptAggregatesAreReturnedWithoutBeingReplacedByNotAttemptedDefaults() throws Exception {
		when(service.getReports()).thenReturn(List.of(failedAttemptReport()));

		mockMvc.perform(get("/api/management/reports/learner-courses").with(user(principal(Role.ADMIN))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].learnerId").value(7))
				.andExpect(jsonPath("$[0].courseId").value(1))
				.andExpect(jsonPath("$[0].status").value("IN_PROGRESS"))
				.andExpect(jsonPath("$[0].modules[0].moduleId").value(10))
				.andExpect(jsonPath("$[0].modules[0].completed").value(false))
				.andExpect(jsonPath("$[0].modules[0].attemptCount").value(2))
				.andExpect(jsonPath("$[0].modules[0].lastScore").value(40))
				.andExpect(jsonPath("$[0].modules[0].bestScore").value(60));
	}

	private LearnerCourseReportResponse report() {
		return new LearnerCourseReportResponse(7L, "Learner", "learner@example.com", 1L, "Course",
				1, 2, 1, 50, CourseProgressStatus.IN_PROGRESS, null, null,
				List.of(new LearnerModuleReportResponse(10L, "First", true, 70, 90, 2, null)));
	}

	private LearnerCourseReportResponse failedAttemptReport() {
		return new LearnerCourseReportResponse(7L, "Learner", "learner@example.com", 1L, "Course",
				0, 2, 2, 0, CourseProgressStatus.IN_PROGRESS, null, null,
				List.of(new LearnerModuleReportResponse(10L, "First", false, 40, 60, 2, null)));
	}

	private AppUserPrincipal principal(Role role) {
		return new AppUserPrincipal(new AppUser(9L, "Manager", "manager@example.com", "hash", role));
	}
}
