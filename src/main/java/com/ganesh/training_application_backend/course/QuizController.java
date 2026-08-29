package com.ganesh.training_application_backend.course;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.ganesh.training_application_backend.auth.AppUserPrincipal;
import com.ganesh.training_application_backend.course.dto.QuizResponse;
import com.ganesh.training_application_backend.course.dto.QuizResultResponse;
import com.ganesh.training_application_backend.course.dto.QuizSubmissionRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/courses/{courseId}/modules/{moduleId}/quiz")
public class QuizController {

	private final QuizService quizService;

	public QuizController(QuizService quizService) {
		this.quizService = quizService;
	}

	@GetMapping
	public QuizResponse getQuiz(@PathVariable Long courseId, @PathVariable Long moduleId) {
		return quizService.getQuiz(courseId, moduleId);
	}

	@PostMapping("/submit")
	public QuizResultResponse submitQuiz(@PathVariable Long courseId, @PathVariable Long moduleId,
			@Valid @RequestBody QuizSubmissionRequest request,
			@AuthenticationPrincipal AppUserPrincipal principal) {
		return quizService.submitQuiz(courseId, moduleId, request, principal.getId());
	}
}
