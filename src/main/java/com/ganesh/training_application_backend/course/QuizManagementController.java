package com.ganesh.training_application_backend.course;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ganesh.training_application_backend.course.dto.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/management/courses/{courseId}/modules/{moduleId}/quiz")
public class QuizManagementController {
	private final QuizManagementService service;

	public QuizManagementController(QuizManagementService service) { this.service = service; }

	@GetMapping
	public QuizManagementResponse getQuiz(@PathVariable Long courseId, @PathVariable Long moduleId) {
		return service.getQuiz(courseId, moduleId);
	}

	@PostMapping
	public ResponseEntity<QuizManagementResponse> createQuiz(@PathVariable Long courseId, @PathVariable Long moduleId,
			@Valid @RequestBody QuizManagementRequest request) {
		QuizManagementResponse response = service.createQuiz(courseId, moduleId, request);
		return ResponseEntity.created(URI.create("/api/management/courses/" + courseId + "/modules/" + moduleId + "/quiz")).body(response);
	}

	@PutMapping
	public QuizManagementResponse updateQuiz(@PathVariable Long courseId, @PathVariable Long moduleId,
			@Valid @RequestBody QuizManagementRequest request) { return service.updateQuiz(courseId, moduleId, request); }

	@DeleteMapping
	public ResponseEntity<Void> deleteQuiz(@PathVariable Long courseId, @PathVariable Long moduleId) {
		service.deleteQuiz(courseId, moduleId); return ResponseEntity.noContent().build();
	}

	@PostMapping("/questions")
	public ResponseEntity<QuizQuestionManagementResponse> createQuestion(@PathVariable Long courseId, @PathVariable Long moduleId,
			@Valid @RequestBody QuizQuestionManagementRequest request) {
		QuizQuestionManagementResponse response = service.createQuestion(courseId, moduleId, request);
		return ResponseEntity.created(URI.create("/api/management/courses/" + courseId + "/modules/" + moduleId + "/quiz/questions/" + response.id())).body(response);
	}

	@PutMapping("/questions/{questionId}")
	public QuizQuestionManagementResponse updateQuestion(@PathVariable Long courseId, @PathVariable Long moduleId,
			@PathVariable Long questionId, @Valid @RequestBody QuizQuestionManagementRequest request) {
		return service.updateQuestion(courseId, moduleId, questionId, request);
	}

	@DeleteMapping("/questions/{questionId}")
	public ResponseEntity<Void> deleteQuestion(@PathVariable Long courseId, @PathVariable Long moduleId, @PathVariable Long questionId) {
		service.deleteQuestion(courseId, moduleId, questionId); return ResponseEntity.noContent().build();
	}

	@PostMapping("/questions/{questionId}/options")
	public ResponseEntity<AnswerOptionManagementResponse> createOption(@PathVariable Long courseId, @PathVariable Long moduleId,
			@PathVariable Long questionId, @Valid @RequestBody AnswerOptionManagementRequest request) {
		AnswerOptionManagementResponse response = service.createOption(courseId, moduleId, questionId, request);
		return ResponseEntity.created(URI.create("/api/management/courses/" + courseId + "/modules/" + moduleId + "/quiz/questions/" + questionId + "/options/" + response.id())).body(response);
	}

	@PutMapping("/questions/{questionId}/options/{optionId}")
	public AnswerOptionManagementResponse updateOption(@PathVariable Long courseId, @PathVariable Long moduleId,
			@PathVariable Long questionId, @PathVariable Long optionId, @Valid @RequestBody AnswerOptionManagementRequest request) {
		return service.updateOption(courseId, moduleId, questionId, optionId, request);
	}

	@DeleteMapping("/questions/{questionId}/options/{optionId}")
	public ResponseEntity<Void> deleteOption(@PathVariable Long courseId, @PathVariable Long moduleId,
			@PathVariable Long questionId, @PathVariable Long optionId) {
		service.deleteOption(courseId, moduleId, questionId, optionId); return ResponseEntity.noContent().build();
	}
}
