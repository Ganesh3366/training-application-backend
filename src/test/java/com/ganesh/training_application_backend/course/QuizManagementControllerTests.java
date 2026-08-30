package com.ganesh.training_application_backend.course;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ganesh.training_application_backend.auth.AppUser;
import com.ganesh.training_application_backend.auth.AppUserPrincipal;
import com.ganesh.training_application_backend.auth.Role;
import com.ganesh.training_application_backend.config.SecurityConfig;
import com.ganesh.training_application_backend.course.dto.*;

@WebMvcTest(QuizManagementController.class)
@Import(SecurityConfig.class)
class QuizManagementControllerTests {
	@MockitoBean QuizManagementService service;
	@Autowired MockMvc mockMvc;
	private static final String URL = "/api/management/courses/1/modules/2/quiz";

	@Test void managementAccessRequiresAllowedRole() throws Exception {
		when(service.getQuiz(1L, 2L)).thenReturn(response());
		mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
		mockMvc.perform(get(URL).with(user(principal(Role.USER)))).andExpect(status().isForbidden());
		mockMvc.perform(get(URL).with(user(principal(Role.ADMIN)))).andExpect(status().isOk());
		mockMvc.perform(get(URL).with(user(principal(Role.INSTRUCTOR)))).andExpect(status().isOk());
	}

	@Test void mutationRequiresCsrfAndValidCsrfReachesService() throws Exception {
		when(service.createQuiz(eq(1L), eq(2L), any())).thenReturn(response());
		mockMvc.perform(post(URL).with(user(principal(Role.ADMIN))).contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Quiz\",\"passingScore\":70}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post(URL).with(user(principal(Role.INSTRUCTOR))).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Quiz\",\"passingScore\":70}"))
				.andExpect(status().isCreated());
		verify(service).createQuiz(eq(1L), eq(2L), any());
	}

	@Test void validatesQuizFieldsAndPassingScoreRange() throws Exception {
		for (String body : List.of("{\"title\":\" \",\"passingScore\":70}",
				"{\"title\":\"Quiz\",\"passingScore\":-1}", "{\"title\":\"Quiz\",\"passingScore\":101}")) {
			mockMvc.perform(post(URL).with(user(principal(Role.ADMIN))).with(csrf())
					.contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
		}
	}

	@Test void validatesBlankQuestionAndOptionAndExplicitCorrectFlag() throws Exception {
		mockMvc.perform(post(URL + "/questions").with(user(principal(Role.ADMIN))).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"questionText\":\" \"}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post(URL + "/questions/3/options").with(user(principal(Role.ADMIN))).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"optionText\":\" \"}"))
				.andExpect(status().isBadRequest());
	}

	@Test void managementResponseContainsCorrectFlag() throws Exception {
		when(service.getQuiz(1L, 2L)).thenReturn(response());
		mockMvc.perform(get(URL).with(user(principal(Role.ADMIN))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.questions[0].options[0].correct").value(true));
	}

	private QuizManagementResponse response() {
		return new QuizManagementResponse(4L, "Quiz", 70, List.of(new QuizQuestionManagementResponse(5L,
				"Question", 1, List.of(new AnswerOptionManagementResponse(6L, "Answer", true, 1)))));
	}

	private AppUserPrincipal principal(Role role) {
		return new AppUserPrincipal(new AppUser(9L, role.name(), role.name().toLowerCase() + "@test.com", "hash", role));
	}
}
