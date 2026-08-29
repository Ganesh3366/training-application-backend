package com.ganesh.training_application_backend.course;

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
import com.ganesh.training_application_backend.course.dto.CourseProgressResponse;
import com.ganesh.training_application_backend.course.dto.ModuleProgressResponse;

@WebMvcTest(ModuleProgressController.class)
@Import(SecurityConfig.class)
class ModuleProgressControllerTests {

	@MockitoBean ModuleProgressService service;
	@Autowired MockMvc mockMvc;

	@Test
	void anonymousProgressRequestIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/courses/1/progress")).andExpect(status().isUnauthorized());
	}

	@Test
	void authenticatedProgressUsesPrincipalAndNeedsNoUserId() throws Exception {
		when(service.getCourseProgress(1L, 7L)).thenReturn(new CourseProgressResponse(
				1L, 1, 0, 1, List.of(new ModuleProgressResponse(10L, false, 0, null, null, null))));

		mockMvc.perform(get("/api/courses/1/progress").queryParam("userId", "999").with(user(principal())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.courseId").value(1))
				.andExpect(jsonPath("$.modules[0].attemptsCount").value(0))
				.andExpect(jsonPath("$.modules[0].userId").doesNotExist());
	}

	private AppUserPrincipal principal() {
		return new AppUserPrincipal(new AppUser(7L, "Learner", "learner@example.com", "hash", Role.USER));
	}
}
