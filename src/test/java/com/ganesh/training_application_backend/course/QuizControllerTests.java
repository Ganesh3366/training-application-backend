package com.ganesh.training_application_backend.course;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.ganesh.training_application_backend.config.SecurityConfig;
import com.ganesh.training_application_backend.auth.AppUser;
import com.ganesh.training_application_backend.auth.AppUserPrincipal;
import com.ganesh.training_application_backend.auth.Role;
import com.ganesh.training_application_backend.course.dto.AnswerOptionResponse;
import com.ganesh.training_application_backend.course.dto.QuizQuestionResponse;
import com.ganesh.training_application_backend.course.dto.QuizResponse;
import com.ganesh.training_application_backend.course.dto.QuizResultResponse;
import com.ganesh.training_application_backend.course.dto.QuizSubmissionRequest;

@WebMvcTest(QuizController.class)
@Import(SecurityConfig.class)
class QuizControllerTests {

	@MockitoBean
	private QuizService quizService;

	private final MockMvc mockMvc;

	@Autowired
	QuizControllerTests(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void anonymousGetQuizIsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/courses/1/modules/10/quiz"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser
	void authenticatedUserGetsQuizWithoutExposingCorrectAnswers() throws Exception {
		when(quizService.getQuiz(1L, 10L)).thenReturn(quizResponse());

		mockMvc.perform(get("/api/courses/1/modules/10/quiz"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Angular Fundamentals Quiz"))
				.andExpect(jsonPath("$.questions[0].questionText").value("What language does Angular use?"))
				.andExpect(jsonPath("$.questions[0].options[0].optionText").value("TypeScript"))
				.andExpect(jsonPath("$.questions[0].options[0].correct").doesNotExist())
				.andExpect(jsonPath("$.questions[0].options[0].isCorrect").doesNotExist());
	}

	@Test
	void anonymousQuizSubmissionWithValidCsrfIsUnauthorized() throws Exception {
		mockMvc.perform(post("/api/courses/1/modules/10/quiz/submit")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"answers":[{"questionId":101,"optionId":1001}]}
						"""))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser
	void authenticatedQuizSubmissionWithoutCsrfIsForbidden() throws Exception {
		mockMvc.perform(post("/api/courses/1/modules/10/quiz/submit")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"answers":[{"questionId":101,"optionId":1001}]}
						"""))
				.andExpect(status().isForbidden());
	}

	@Test
	void authenticatedQuizSubmissionWithValidCsrfSucceeds() throws Exception {
		when(quizService.submitQuiz(org.mockito.ArgumentMatchers.eq(1L),
				org.mockito.ArgumentMatchers.eq(10L), any(QuizSubmissionRequest.class),
				org.mockito.ArgumentMatchers.eq(7L)))
				.thenReturn(new QuizResultResponse(3, 3, 100, 70, true));

		mockMvc.perform(post("/api/courses/1/modules/10/quiz/submit")
				.with(user(principal()))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"answers":[{"questionId":101,"optionId":1001}],"score":0,"passed":false}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.score").value(100))
				.andExpect(jsonPath("$.passed").value(true));
	}

	private AppUserPrincipal principal() {
		return new AppUserPrincipal(new AppUser(7L, "Learner", "learner@example.com", "hash", Role.USER));
	}

	@Test
	void unrelatedPostRemainsProtected() throws Exception {
		mockMvc.perform(post("/api/courses/1/modules/10/quiz").with(csrf()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser
	void returnsNotFoundForWrongCourseQuiz() throws Exception {
		when(quizService.getQuiz(2L, 10L))
				.thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));

		mockMvc.perform(get("/api/courses/2/modules/10/quiz"))
				.andExpect(status().isNotFound());
	}

	private QuizResponse quizResponse() {
		return new QuizResponse(
				20L,
				"Angular Fundamentals Quiz",
				70,
				List.of(new QuizQuestionResponse(
						101L,
						"What language does Angular use?",
						1,
						List.of(new AnswerOptionResponse(1001L, "TypeScript", 1)))));
	}
}
