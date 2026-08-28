package com.ganesh.training_application_backend.course;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ganesh.training_application_backend.course.dto.AnswerOptionResponse;
import com.ganesh.training_application_backend.course.dto.QuizAnswerRequest;
import com.ganesh.training_application_backend.course.dto.QuizQuestionResponse;
import com.ganesh.training_application_backend.course.dto.QuizResponse;
import com.ganesh.training_application_backend.course.dto.QuizResultResponse;
import com.ganesh.training_application_backend.course.dto.QuizSubmissionRequest;

@Service
public class QuizService {

	private final QuizRepository quizRepository;
	private final QuizQuestionRepository quizQuestionRepository;
	private final AnswerOptionRepository answerOptionRepository;

	public QuizService(QuizRepository quizRepository, QuizQuestionRepository quizQuestionRepository,
			AnswerOptionRepository answerOptionRepository) {
		this.quizRepository = quizRepository;
		this.quizQuestionRepository = quizQuestionRepository;
		this.answerOptionRepository = answerOptionRepository;
	}

	@Transactional(readOnly = true)
	public QuizResponse getQuiz(Long courseId, Long moduleId) {
		Quiz quiz = findNestedQuiz(courseId, moduleId);
		List<QuizQuestion> questions = quizQuestionRepository.findByQuizIdOrderByPositionAsc(quiz.getId());
		Map<Long, List<AnswerOption>> optionsByQuestion = loadOptionsByQuestion(questions);

		List<QuizQuestionResponse> questionResponses = questions.stream()
				.map(question -> toQuestionResponse(question, optionsByQuestion.getOrDefault(question.getId(), List.of())))
				.toList();

		return new QuizResponse(quiz.getId(), quiz.getTitle(), quiz.getPassingScore(), questionResponses);
	}

	@Transactional(readOnly = true)
	public QuizResultResponse submitQuiz(Long courseId, Long moduleId, QuizSubmissionRequest request) {
		Quiz quiz = findNestedQuiz(courseId, moduleId);
		List<QuizQuestion> questions = quizQuestionRepository.findByQuizIdOrderByPositionAsc(quiz.getId());
		if (questions.isEmpty()) {
			throw badRequest("Quiz has no questions");
		}

		List<QuizAnswerRequest> answers = requireAnswers(request);
		Map<Long, QuizAnswerRequest> answersByQuestion = validateQuestionAnswers(questions, answers);
		Map<Long, List<AnswerOption>> optionsByQuestion = loadOptionsByQuestion(questions);

		int correctAnswers = 0;
		for (QuizQuestion question : questions) {
			QuizAnswerRequest answer = answersByQuestion.get(question.getId());
			AnswerOption selectedOption = findSelectedOption(
					question.getId(), answer.optionId(), optionsByQuestion.getOrDefault(question.getId(), List.of()));
			if (selectedOption.isCorrect()) {
				correctAnswers++;
			}
		}

		int score = calculateScore(correctAnswers, questions.size());
		return new QuizResultResponse(
				questions.size(),
				correctAnswers,
				score,
				quiz.getPassingScore(),
				score >= quiz.getPassingScore());
	}

	static int calculateScore(int correctAnswers, int totalQuestions) {
		if (totalQuestions <= 0) {
			throw new IllegalArgumentException("Total questions must be positive");
		}
		return (int) Math.round(correctAnswers * 100.0 / totalQuestions);
	}

	private Quiz findNestedQuiz(Long courseId, Long moduleId) {
		return quizRepository.findByModuleIdAndModuleCourseId(moduleId, courseId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));
	}

	private List<QuizAnswerRequest> requireAnswers(QuizSubmissionRequest request) {
		if (request == null || request.answers() == null || request.answers().stream().anyMatch(answer -> answer == null
				|| answer.questionId() == null || answer.optionId() == null)) {
			throw badRequest("Every answer must include a question and option ID");
		}
		return request.answers();
	}

	private Map<Long, QuizAnswerRequest> validateQuestionAnswers(List<QuizQuestion> questions,
			List<QuizAnswerRequest> answers) {
		if (answers.size() != questions.size()) {
			throw badRequest("Submit exactly one answer for every quiz question");
		}

		Set<Long> quizQuestionIds = new HashSet<>();
		questions.forEach(question -> quizQuestionIds.add(question.getId()));
		Map<Long, QuizAnswerRequest> answersByQuestion = new HashMap<>();
		for (QuizAnswerRequest answer : answers) {
			if (!quizQuestionIds.contains(answer.questionId())) {
				throw badRequest("Submitted question does not belong to this quiz");
			}
			if (answersByQuestion.put(answer.questionId(), answer) != null) {
				throw badRequest("A quiz question was answered more than once");
			}
		}
		return answersByQuestion;
	}

	private Map<Long, List<AnswerOption>> loadOptionsByQuestion(List<QuizQuestion> questions) {
		if (questions.isEmpty()) {
			return Map.of();
		}
		List<Long> questionIds = questions.stream().map(QuizQuestion::getId).toList();
		Map<Long, List<AnswerOption>> optionsByQuestion = new HashMap<>();
		for (AnswerOption option : answerOptionRepository
				.findByQuestionIdInOrderByQuestionIdAscPositionAsc(questionIds)) {
			optionsByQuestion.computeIfAbsent(option.getQuestion().getId(), ignored -> new java.util.ArrayList<>())
					.add(option);
		}
		return optionsByQuestion;
	}

	private AnswerOption findSelectedOption(Long questionId, Long optionId, List<AnswerOption> options) {
		return options.stream()
				.filter(option -> option.getId().equals(optionId))
				.findFirst()
				.orElseThrow(() -> badRequest("Selected option does not belong to question " + questionId));
	}

	private QuizQuestionResponse toQuestionResponse(QuizQuestion question, List<AnswerOption> options) {
		return new QuizQuestionResponse(
				question.getId(),
				question.getQuestionText(),
				question.getPosition(),
				options.stream().map(this::toOptionResponse).toList());
	}

	private AnswerOptionResponse toOptionResponse(AnswerOption option) {
		return new AnswerOptionResponse(option.getId(), option.getOptionText(), option.getPosition());
	}

	private ResponseStatusException badRequest(String reason) {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
	}
}
