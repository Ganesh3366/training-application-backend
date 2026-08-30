package com.ganesh.training_application_backend.course;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ganesh.training_application_backend.course.dto.AnswerOptionManagementRequest;
import com.ganesh.training_application_backend.course.dto.AnswerOptionManagementResponse;
import com.ganesh.training_application_backend.course.dto.QuizManagementRequest;
import com.ganesh.training_application_backend.course.dto.QuizManagementResponse;
import com.ganesh.training_application_backend.course.dto.QuizQuestionManagementRequest;
import com.ganesh.training_application_backend.course.dto.QuizQuestionManagementResponse;

@Service
public class QuizManagementService {

	private final CourseModuleRepository moduleRepository;
	private final QuizRepository quizRepository;
	private final QuizQuestionRepository questionRepository;
	private final AnswerOptionRepository optionRepository;

	public QuizManagementService(CourseModuleRepository moduleRepository, QuizRepository quizRepository,
			QuizQuestionRepository questionRepository, AnswerOptionRepository optionRepository) {
		this.moduleRepository = moduleRepository;
		this.quizRepository = quizRepository;
		this.questionRepository = questionRepository;
		this.optionRepository = optionRepository;
	}

	@Transactional(readOnly = true)
	public QuizManagementResponse getQuiz(Long courseId, Long moduleId) {
		return toResponse(requireQuiz(courseId, moduleId));
	}

	@Transactional
	public QuizManagementResponse createQuiz(Long courseId, Long moduleId, QuizManagementRequest request) {
		CourseModule module = requireModule(courseId, moduleId);
		if (quizRepository.existsByModuleId(moduleId)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Module already has a quiz");
		}
		return toResponse(quizRepository.save(new Quiz(null, module, request.title().trim(), request.passingScore())));
	}

	@Transactional
	public QuizManagementResponse updateQuiz(Long courseId, Long moduleId, QuizManagementRequest request) {
		Quiz quiz = requireQuiz(courseId, moduleId);
		quiz.updateDetails(request.title().trim(), request.passingScore());
		return toResponse(quiz);
	}

	@Transactional
	public void deleteQuiz(Long courseId, Long moduleId) {
		Quiz quiz = requireQuiz(courseId, moduleId);
		List<QuizQuestion> questions = questionRepository.findByQuizIdOrderByPositionAsc(quiz.getId());
		List<Long> questionIds = questions.stream().map(QuizQuestion::getId).toList();
		if (!questionIds.isEmpty()) optionRepository.deleteAllByQuestionIdIn(questionIds);
		questionRepository.deleteAllByQuizId(quiz.getId());
		quizRepository.delete(quiz);
	}

	@Transactional
	public QuizQuestionManagementResponse createQuestion(Long courseId, Long moduleId,
			QuizQuestionManagementRequest request) {
		Quiz quiz = requireQuiz(courseId, moduleId);
		int position = questionRepository.findTopByQuizIdOrderByPositionDesc(quiz.getId())
				.map(q -> q.getPosition() + 1).orElse(1);
		return toQuestionResponse(questionRepository.save(
				new QuizQuestion(null, quiz, request.questionText().trim(), position)), List.of());
	}

	@Transactional
	public QuizQuestionManagementResponse updateQuestion(Long courseId, Long moduleId, Long questionId,
			QuizQuestionManagementRequest request) {
		Quiz quiz = requireQuiz(courseId, moduleId);
		QuizQuestion question = requireQuestion(quiz.getId(), questionId);
		question.updateDetails(request.questionText().trim());
		return toQuestionResponse(question, optionRepository.findByQuestionIdOrderByPositionAsc(questionId));
	}

	@Transactional
	public void deleteQuestion(Long courseId, Long moduleId, Long questionId) {
		Quiz quiz = requireQuiz(courseId, moduleId);
		QuizQuestion question = requireQuestion(quiz.getId(), questionId);
		optionRepository.deleteAllByQuestionId(questionId);
		questionRepository.delete(question);
	}

	@Transactional
	public AnswerOptionManagementResponse createOption(Long courseId, Long moduleId, Long questionId,
			AnswerOptionManagementRequest request) {
		QuizQuestion question = requireNestedQuestion(courseId, moduleId, questionId);
		if (request.correct()) clearCorrectOption(questionId, null);
		int position = optionRepository.findTopByQuestionIdOrderByPositionDesc(questionId)
				.map(o -> o.getPosition() + 1).orElse(1);
		return toOptionResponse(optionRepository.save(new AnswerOption(null, question,
				request.optionText().trim(), request.correct(), position)));
	}

	@Transactional
	public AnswerOptionManagementResponse updateOption(Long courseId, Long moduleId, Long questionId, Long optionId,
			AnswerOptionManagementRequest request) {
		QuizQuestion question = requireNestedQuestion(courseId, moduleId, questionId);
		AnswerOption option = requireOption(question.getId(), optionId);
		if (request.correct()) clearCorrectOption(questionId, optionId);
		option.updateDetails(request.optionText().trim(), request.correct());
		return toOptionResponse(option);
	}

	@Transactional
	public void deleteOption(Long courseId, Long moduleId, Long questionId, Long optionId) {
		QuizQuestion question = requireNestedQuestion(courseId, moduleId, questionId);
		optionRepository.delete(requireOption(question.getId(), optionId));
	}

	private CourseModule requireModule(Long courseId, Long moduleId) {
		return moduleRepository.findByIdAndCourseId(moduleId, courseId).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course module not found"));
	}

	private Quiz requireQuiz(Long courseId, Long moduleId) {
		requireModule(courseId, moduleId);
		return quizRepository.findByModuleIdAndModuleCourseId(moduleId, courseId).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz not found"));
	}

	private QuizQuestion requireNestedQuestion(Long courseId, Long moduleId, Long questionId) {
		Quiz quiz = requireQuiz(courseId, moduleId);
		return requireQuestion(quiz.getId(), questionId);
	}

	private QuizQuestion requireQuestion(Long quizId, Long questionId) {
		return questionRepository.findByIdAndQuizId(questionId, quizId).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz question not found"));
	}

	private AnswerOption requireOption(Long questionId, Long optionId) {
		return optionRepository.findByIdAndQuestionId(optionId, questionId).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Answer option not found"));
	}

	private void clearCorrectOption(Long questionId, Long exceptId) {
		optionRepository.findByQuestionIdAndCorrectTrue(questionId).stream()
				.filter(option -> !option.getId().equals(exceptId)).forEach(AnswerOption::clearCorrect);
	}

	private QuizManagementResponse toResponse(Quiz quiz) {
		List<QuizQuestion> questions = questionRepository.findByQuizIdOrderByPositionAsc(quiz.getId());
		Map<Long, List<AnswerOption>> options = questions.isEmpty() ? Map.of()
				: optionRepository.findByQuestionIdInOrderByQuestionIdAscPositionAsc(
						questions.stream().map(QuizQuestion::getId).toList()).stream()
						.collect(Collectors.groupingBy(o -> o.getQuestion().getId()));
		return new QuizManagementResponse(quiz.getId(), quiz.getTitle(), quiz.getPassingScore(), questions.stream()
				.map(q -> toQuestionResponse(q, options.getOrDefault(q.getId(), List.of()))).toList());
	}

	private QuizQuestionManagementResponse toQuestionResponse(QuizQuestion question, List<AnswerOption> options) {
		return new QuizQuestionManagementResponse(question.getId(), question.getQuestionText(), question.getPosition(),
				options.stream().map(this::toOptionResponse).toList());
	}

	private AnswerOptionManagementResponse toOptionResponse(AnswerOption option) {
		return new AnswerOptionManagementResponse(option.getId(), option.getOptionText(), option.isCorrect(),
				option.getPosition());
	}
}
