package com.ganesh.training_application_backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.ganesh.training_application_backend.auth.AppUser;
import com.ganesh.training_application_backend.auth.Role;
import com.ganesh.training_application_backend.course.dto.QuizAnswerRequest;
import com.ganesh.training_application_backend.course.dto.QuizResponse;
import com.ganesh.training_application_backend.course.dto.QuizResultResponse;
import com.ganesh.training_application_backend.course.dto.QuizSubmissionRequest;

@ExtendWith(MockitoExtension.class)
class QuizServiceTests {

	@Mock
	private QuizRepository quizRepository;

	@Mock
	private QuizQuestionRepository quizQuestionRepository;

	@Mock
	private AnswerOptionRepository answerOptionRepository;

	@Mock
	private ModuleProgressRepository moduleProgressRepository;

	@Mock
	private com.ganesh.training_application_backend.auth.AppUserRepository appUserRepository;

	@InjectMocks
	private QuizService quizService;

	private Quiz quiz;
	private QuizQuestion firstQuestion;
	private QuizQuestion secondQuestion;
	private QuizQuestion thirdQuestion;
	private List<QuizQuestion> questions;
	private List<AnswerOption> options;

	@BeforeEach
	void setUp() {
		Course course = new Course(1L, "Angular", "Angular course", "John Doe", 10,
				CourseLevel.BEGINNER, CourseCategory.INFORMATION_TECHNOLOGY);
		CourseModule module = new CourseModule(10L, course, "Fundamentals", "Start here", 1);
		quiz = new Quiz(20L, module, "Angular Quiz", 70);
		firstQuestion = new QuizQuestion(101L, quiz, "Primary language?", 1);
		secondQuestion = new QuizQuestion(102L, quiz, "What is a component?", 2);
		thirdQuestion = new QuizQuestion(103L, quiz, "Purpose of dependency injection?", 3);
		questions = List.of(firstQuestion, secondQuestion, thirdQuestion);
		options = List.of(
				option(1001L, firstQuestion, "TypeScript", true, 1),
				option(1002L, firstQuestion, "COBOL", false, 2),
				option(1003L, secondQuestion, "A UI building block", true, 1),
				option(1004L, secondQuestion, "A database", false, 2),
				option(1005L, thirdQuestion, "Provide dependencies", true, 1),
				option(1006L, thirdQuestion, "Style templates", false, 2));
	}

	@Test
	void returnsOrderedQuizWithoutCorrectAnswerInformation() {
		stubQuizData();

		QuizResponse result = quizService.getQuiz(1L, 10L);

		assertThat(result.id()).isEqualTo(20L);
		assertThat(result.questions()).extracting(question -> question.position()).containsExactly(1, 2, 3);
		assertThat(result.questions().get(0).options()).extracting(option -> option.position())
				.containsExactly(1, 2);
		assertThat(result.questions().get(0).options().get(0).optionText()).isEqualTo("TypeScript");
	}

	@Test
	void returnsNotFoundForUnknownOrWrongCourseQuiz() {
		when(quizRepository.findByModuleIdAndModuleCourseId(10L, 2L)).thenReturn(Optional.empty());

		assertStatus(HttpStatus.NOT_FOUND, () -> quizService.getQuiz(2L, 10L));
	}

	@Test
	void fullyCorrectSubmissionScoresOneHundredAndPasses() {
		stubQuizData();

		QuizResultResponse result = quizService.submitQuiz(1L, 10L, submission(1001L, 1003L, 1005L), 1L);

		assertThat(result).isEqualTo(new QuizResultResponse(3, 3, 100, 70, true));
		ArgumentCaptor<ModuleProgress> progressCaptor = ArgumentCaptor.forClass(ModuleProgress.class);
		verify(moduleProgressRepository).save(progressCaptor.capture());
		ModuleProgress progress = progressCaptor.getValue();
		assertThat(progress.getAttemptsCount()).isEqualTo(1);
		assertThat(progress.getLastScore()).isEqualTo(100);
		assertThat(progress.getBestScore()).isEqualTo(100);
		assertThat(progress.isCompleted()).isTrue();
		assertThat(progress.getCompletedAt()).isNotNull();
	}

	@Test
	void partiallyCorrectSubmissionUsesNearestIntegerPercentage() {
		quiz = new Quiz(20L, quiz.getModule(), "Angular Quiz", 60);
		firstQuestion = new QuizQuestion(101L, quiz, "Primary language?", 1);
		secondQuestion = new QuizQuestion(102L, quiz, "What is a component?", 2);
		thirdQuestion = new QuizQuestion(103L, quiz, "Purpose of dependency injection?", 3);
		questions = List.of(firstQuestion, secondQuestion, thirdQuestion);
		options = List.of(
				option(1001L, firstQuestion, "TypeScript", true, 1),
				option(1002L, firstQuestion, "COBOL", false, 2),
				option(1003L, secondQuestion, "A UI building block", true, 1),
				option(1004L, secondQuestion, "A database", false, 2),
				option(1005L, thirdQuestion, "Provide dependencies", true, 1),
				option(1006L, thirdQuestion, "Style templates", false, 2));
		stubQuizData();

		QuizResultResponse result = quizService.submitQuiz(1L, 10L, submission(1001L, 1003L, 1006L), 1L);

		assertThat(result.score()).isEqualTo(67);
		assertThat(result.correctAnswers()).isEqualTo(2);
		assertThat(result.passed()).isTrue();
	}

	@Test
	void firstFailedSubmissionCreatesAndSavesProgressWithQuizAggregates() {
		stubQuizData();
		AppUser learner = new AppUser(1L, "Learner", "learner@example.com", "hash", Role.USER);
		when(appUserRepository.getReferenceById(1L)).thenReturn(learner);

		QuizResultResponse result = quizService.submitQuiz(1L, 10L, submission(1001L, 1003L, 1006L), 1L);

		assertThat(result.score()).isEqualTo(67);
		assertThat(result.passed()).isFalse();
		ArgumentCaptor<ModuleProgress> progressCaptor = ArgumentCaptor.forClass(ModuleProgress.class);
		verify(moduleProgressRepository).save(progressCaptor.capture());
		ModuleProgress progress = progressCaptor.getValue();
		assertThat(progress.getUser()).isSameAs(learner);
		assertThat(progress.getModule()).isSameAs(quiz.getModule());
		assertThat(progress.getAttemptsCount()).isEqualTo(1);
		assertThat(progress.getLastScore()).isEqualTo(67);
		assertThat(progress.getBestScore()).isEqualTo(67);
		assertThat(progress.isCompleted()).isFalse();
		assertThat(progress.getCompletedAt()).isNull();
	}

	@Test
	void failuresPassAndPostPassFailurePreserveTheCompleteAttemptLifecycle() {
		stubQuizData();
		Long userId = 7L;
		AppUser learner = new AppUser(userId, "Learner", "learner@example.com", "hash", Role.USER);
		ModuleProgress progress = new ModuleProgress(learner, quiz.getModule());
		when(moduleProgressRepository.findByUserIdAndModuleId(userId, 10L))
				.thenReturn(Optional.of(progress));

		quizService.submitQuiz(1L, 10L, submission(1001L, 1004L, 1006L), userId);
		assertThat(progress.getAttemptsCount()).isEqualTo(1);
		assertThat(progress.getLastScore()).isEqualTo(33);
		assertThat(progress.getBestScore()).isEqualTo(33);
		assertThat(progress.isCompleted()).isFalse();

		quizService.submitQuiz(1L, 10L, submission(1001L, 1003L, 1006L), userId);
		assertThat(progress.getAttemptsCount()).isEqualTo(2);
		assertThat(progress.getLastScore()).isEqualTo(67);
		assertThat(progress.getBestScore()).isEqualTo(67);
		assertThat(progress.isCompleted()).isFalse();

		quizService.submitQuiz(1L, 10L, submission(1001L, 1003L, 1005L), userId);
		Instant completedAt = progress.getCompletedAt();
		assertThat(progress.getAttemptsCount()).isEqualTo(3);
		assertThat(progress.getLastScore()).isEqualTo(100);
		assertThat(progress.getBestScore()).isEqualTo(100);
		assertThat(progress.isCompleted()).isTrue();
		assertThat(completedAt).isNotNull();

		quizService.submitQuiz(1L, 10L, submission(1001L, 1004L, 1006L), userId);
		assertThat(progress.getAttemptsCount()).isEqualTo(4);
		assertThat(progress.getLastScore()).isEqualTo(33);
		assertThat(progress.getBestScore()).isEqualTo(100);
		assertThat(progress.isCompleted()).isTrue();
		assertThat(progress.getCompletedAt()).isEqualTo(completedAt);
		verify(moduleProgressRepository, org.mockito.Mockito.times(4)).save(progress);
	}

	@Test
	void lowerFailedRetryUpdatesExistingProgressWithoutRemovingCompletionOrBestScore() {
		stubQuizData();
		Long userId = 7L;
		Instant firstCompletion = Instant.parse("2026-01-02T10:00:00Z");
		AppUser user = new AppUser(userId, "Learner", "learner@example.com", "hash", Role.USER);
		ModuleProgress existingProgress = new ModuleProgress(user, quiz.getModule());
		existingProgress.recordAttempt(80, true, firstCompletion);
		when(moduleProgressRepository.findByUserIdAndModuleId(userId, 10L))
				.thenReturn(Optional.of(existingProgress));

		QuizResultResponse result = quizService.submitQuiz(
				1L, 10L, submission(1001L, 1003L, 1006L), userId);

		assertThat(result.score()).isEqualTo(67);
		assertThat(result.passed()).isFalse();
		assertThat(existingProgress.getAttemptsCount()).isEqualTo(2);
		assertThat(existingProgress.getLastScore()).isEqualTo(67);
		assertThat(existingProgress.getBestScore()).isEqualTo(80);
		assertThat(existingProgress.isCompleted()).isTrue();
		assertThat(existingProgress.getCompletedAt()).isEqualTo(firstCompletion);
		verify(moduleProgressRepository).save(existingProgress);
	}

	@Test
	void rejectsDuplicateQuestionAnswers() {
		stubQuizData();
		QuizSubmissionRequest request = new QuizSubmissionRequest(List.of(
				new QuizAnswerRequest(101L, 1001L),
				new QuizAnswerRequest(101L, 1002L),
				new QuizAnswerRequest(102L, 1003L)));

		assertStatus(HttpStatus.BAD_REQUEST, () -> quizService.submitQuiz(1L, 10L, request, 1L));
		verifyNoInteractions(moduleProgressRepository);
	}

	@Test
	void rejectsMissingAnswer() {
		stubQuizData();
		QuizSubmissionRequest request = new QuizSubmissionRequest(List.of(
				new QuizAnswerRequest(101L, 1001L), new QuizAnswerRequest(102L, 1003L)));

		assertStatus(HttpStatus.BAD_REQUEST, () -> quizService.submitQuiz(1L, 10L, request, 1L));
		verifyNoInteractions(moduleProgressRepository);
	}

	@Test
	void rejectsExtraOrForeignQuestion() {
		stubQuizData();
		QuizSubmissionRequest request = new QuizSubmissionRequest(List.of(
				new QuizAnswerRequest(101L, 1001L),
				new QuizAnswerRequest(102L, 1003L),
				new QuizAnswerRequest(999L, 1005L)));

		assertStatus(HttpStatus.BAD_REQUEST, () -> quizService.submitQuiz(1L, 10L, request, 1L));
		verifyNoInteractions(moduleProgressRepository);
	}

	@Test
	void rejectsOptionThatBelongsToAnotherQuestion() {
		stubQuizData();

		assertStatus(HttpStatus.BAD_REQUEST,
				() -> quizService.submitQuiz(1L, 10L, submission(1003L, 1004L, 1006L), 1L));
		verifyNoInteractions(moduleProgressRepository);
	}

	@Test
	void rejectsEmptyQuizWithoutDividingByZero() {
		when(quizRepository.findByModuleIdAndModuleCourseId(10L, 1L)).thenReturn(Optional.of(quiz));
		when(quizQuestionRepository.findByQuizIdOrderByPositionAsc(20L)).thenReturn(List.of());

		assertStatus(HttpStatus.CONFLICT,
				() -> quizService.submitQuiz(1L, 10L, new QuizSubmissionRequest(List.of()), 1L));
		verifyNoInteractions(moduleProgressRepository);
	}

	@Test
	void rejectsNullSubmissionSafely() {
		stubQuizData();

		assertStatus(HttpStatus.BAD_REQUEST, () -> quizService.submitQuiz(1L, 10L, null, 1L));
		verifyNoInteractions(moduleProgressRepository);
	}

	@Test
	void rejectsQuestionWithFewerThanTwoOptionsAsIncompleteConfiguration() {
		stubQuizQuestions();
		when(answerOptionRepository.findByQuestionIdInOrderByQuestionIdAscPositionAsc(List.of(101L, 102L, 103L)))
				.thenReturn(List.of(option(1001L, firstQuestion, "TypeScript", true, 1)));

		assertStatus(HttpStatus.CONFLICT, () -> quizService.getQuiz(1L, 10L));
	}

	@Test
	void rejectsMultipleCorrectOptionsAsIncompleteConfiguration() {
		stubQuizQuestions();
		List<AnswerOption> invalidOptions = new java.util.ArrayList<>(options);
		invalidOptions.set(1, option(1002L, firstQuestion, "COBOL", true, 2));
		when(answerOptionRepository.findByQuestionIdInOrderByQuestionIdAscPositionAsc(List.of(101L, 102L, 103L)))
				.thenReturn(invalidOptions);

		assertStatus(HttpStatus.CONFLICT,
				() -> quizService.submitQuiz(1L, 10L, submission(1001L, 1003L, 1005L), 1L));
		verifyNoInteractions(moduleProgressRepository);
	}

	@Test
	void rejectsEmptyQuizOnGetAsIncompleteConfiguration() {
		when(quizRepository.findByModuleIdAndModuleCourseId(10L, 1L)).thenReturn(Optional.of(quiz));
		when(quizQuestionRepository.findByQuizIdOrderByPositionAsc(20L)).thenReturn(List.of());

		assertStatus(HttpStatus.CONFLICT, () -> quizService.getQuiz(1L, 10L));
	}

	@Test
	void rejectsZeroCorrectAnswersOnGetAsIncompleteConfiguration() {
		List<QuizQuestion> oneQuestion = List.of(firstQuestion);
		List<AnswerOption> noCorrectOptions = List.of(
				option(1001L, firstQuestion, "TypeScript", false, 1),
				option(1002L, firstQuestion, "COBOL", false, 2));
		when(quizRepository.findByModuleIdAndModuleCourseId(10L, 1L)).thenReturn(Optional.of(quiz));
		when(quizQuestionRepository.findByQuizIdOrderByPositionAsc(20L)).thenReturn(oneQuestion);
		when(answerOptionRepository.findByQuestionIdInOrderByQuestionIdAscPositionAsc(List.of(101L)))
				.thenReturn(noCorrectOptions);

		assertStatus(HttpStatus.CONFLICT, () -> quizService.getQuiz(1L, 10L));
	}

	@Test
	void rejectsFewerThanTwoOptionsOnSubmissionBeforeProgressChanges() {
		List<QuizQuestion> oneQuestion = List.of(firstQuestion);
		when(quizRepository.findByModuleIdAndModuleCourseId(10L, 1L)).thenReturn(Optional.of(quiz));
		when(quizQuestionRepository.findByQuizIdOrderByPositionAsc(20L)).thenReturn(oneQuestion);
		when(answerOptionRepository.findByQuestionIdInOrderByQuestionIdAscPositionAsc(List.of(101L)))
				.thenReturn(List.of(option(1001L, firstQuestion, "TypeScript", true, 1)));

		assertStatus(HttpStatus.CONFLICT, () -> quizService.submitQuiz(1L, 10L,
				new QuizSubmissionRequest(List.of(new QuizAnswerRequest(101L, 1001L))), 1L));
		verifyNoInteractions(moduleProgressRepository);
	}

	@Test
	void rejectsMultipleCorrectOptionsOnGetAsIncompleteConfiguration() {
		List<QuizQuestion> oneQuestion = List.of(firstQuestion);
		List<AnswerOption> multipleCorrectOptions = List.of(
				option(1001L, firstQuestion, "TypeScript", true, 1),
				option(1002L, firstQuestion, "COBOL", true, 2));
		when(quizRepository.findByModuleIdAndModuleCourseId(10L, 1L)).thenReturn(Optional.of(quiz));
		when(quizQuestionRepository.findByQuizIdOrderByPositionAsc(20L)).thenReturn(oneQuestion);
		when(answerOptionRepository.findByQuestionIdInOrderByQuestionIdAscPositionAsc(List.of(101L)))
				.thenReturn(multipleCorrectOptions);

		assertStatus(HttpStatus.CONFLICT, () -> quizService.getQuiz(1L, 10L));
	}

	@Test
	void rejectsZeroCorrectAnswersOnSubmissionBeforeProgressChanges() {
		List<QuizQuestion> oneQuestion = List.of(firstQuestion);
		List<AnswerOption> noCorrectOptions = List.of(
				option(1001L, firstQuestion, "TypeScript", false, 1),
				option(1002L, firstQuestion, "COBOL", false, 2));
		when(quizRepository.findByModuleIdAndModuleCourseId(10L, 1L)).thenReturn(Optional.of(quiz));
		when(quizQuestionRepository.findByQuizIdOrderByPositionAsc(20L)).thenReturn(oneQuestion);
		when(answerOptionRepository.findByQuestionIdInOrderByQuestionIdAscPositionAsc(List.of(101L)))
				.thenReturn(noCorrectOptions);

		assertStatus(HttpStatus.CONFLICT, () -> quizService.submitQuiz(1L, 10L,
				new QuizSubmissionRequest(List.of(new QuizAnswerRequest(101L, 1001L))), 1L));
		verifyNoInteractions(moduleProgressRepository);
	}

	private void stubQuizData() {
		stubQuizQuestions();
		when(answerOptionRepository.findByQuestionIdInOrderByQuestionIdAscPositionAsc(List.of(101L, 102L, 103L)))
				.thenReturn(options);
	}

	private void stubQuizQuestions() {
		when(quizRepository.findByModuleIdAndModuleCourseId(10L, 1L)).thenReturn(Optional.of(quiz));
		when(quizQuestionRepository.findByQuizIdOrderByPositionAsc(20L)).thenReturn(questions);
	}

	private QuizSubmissionRequest submission(Long firstOption, Long secondOption, Long thirdOption) {
		return new QuizSubmissionRequest(List.of(
				new QuizAnswerRequest(101L, firstOption),
				new QuizAnswerRequest(102L, secondOption),
				new QuizAnswerRequest(103L, thirdOption)));
	}

	private AnswerOption option(Long id, QuizQuestion question, String text, boolean correct, int position) {
		return new AnswerOption(id, question, text, correct, position);
	}

	private void assertStatus(HttpStatus status, org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
		assertThatThrownBy(action).isInstanceOfSatisfying(ResponseStatusException.class,
				exception -> assertThat(exception.getStatusCode()).isEqualTo(status));
	}
}
