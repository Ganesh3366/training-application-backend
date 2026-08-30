package com.ganesh.training_application_backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.ganesh.training_application_backend.course.dto.AnswerOptionManagementRequest;
import com.ganesh.training_application_backend.course.dto.QuizManagementRequest;
import com.ganesh.training_application_backend.course.dto.QuizQuestionManagementRequest;

@ExtendWith(MockitoExtension.class)
class QuizManagementServiceTests {

	@Mock CourseModuleRepository moduleRepository;
	@Mock QuizRepository quizRepository;
	@Mock QuizQuestionRepository questionRepository;
	@Mock AnswerOptionRepository optionRepository;
	@InjectMocks QuizManagementService service;

	private CourseModule module;
	private Quiz quiz;
	private QuizQuestion question;

	@BeforeEach
	void setUp() {
		Course course = new Course(1L, "Course", "Description", "Instructor", 60,
				CourseLevel.BEGINNER, CourseCategory.INFORMATION_TECHNOLOGY);
		module = new CourseModule(10L, course, "Module", "Description", 1);
		quiz = new Quiz(20L, module, "Quiz", 70);
		question = new QuizQuestion(30L, quiz, "Question", 4);
	}

	@Test
	void createsQuizUsingNestedModuleAndNormalizedDetails() {
		stubModule();
		when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.createQuiz(1L, 10L, new QuizManagementRequest("  New quiz  ", 75));

		ArgumentCaptor<Quiz> captor = ArgumentCaptor.forClass(Quiz.class);
		verify(quizRepository).save(captor.capture());
		assertThat(captor.getValue().getModule()).isSameAs(module);
		assertThat(captor.getValue().getTitle()).isEqualTo("New quiz");
		assertThat(captor.getValue().getPassingScore()).isEqualTo(75);
	}

	@Test
	void rejectsDuplicateQuizWithoutSaving() {
		stubModule();
		when(quizRepository.existsByModuleId(10L)).thenReturn(true);

		assertStatus(HttpStatus.CONFLICT,
				() -> service.createQuiz(1L, 10L, new QuizManagementRequest("Quiz", 70)));
		verify(quizRepository, never()).save(any());
	}

	@Test
	void updatesQuizDetailsWithoutChangingModule() {
		stubQuiz();

		service.updateQuiz(1L, 10L, new QuizManagementRequest("  Updated  ", 85));

		assertThat(quiz.getTitle()).isEqualTo("Updated");
		assertThat(quiz.getPassingScore()).isEqualTo(85);
		assertThat(quiz.getModule()).isSameAs(module);
	}

	@Test
	void rejectsWrongCourseModuleOwnership() {
		when(moduleRepository.findByIdAndCourseId(10L, 99L)).thenReturn(Optional.empty());

		assertStatus(HttpStatus.NOT_FOUND, () -> service.getQuiz(99L, 10L));
		verifyNoInteractions(quizRepository, questionRepository, optionRepository);
	}

	@Test
	void deletesQuizChildrenBeforeQuiz() {
		stubQuiz();
		QuizQuestion second = new QuizQuestion(31L, quiz, "Second", 5);
		when(questionRepository.findByQuizIdOrderByPositionAsc(20L)).thenReturn(List.of(question, second));

		service.deleteQuiz(1L, 10L);

		InOrder order = inOrder(optionRepository, questionRepository, quizRepository);
		order.verify(optionRepository).deleteAllByQuestionIdIn(List.of(30L, 31L));
		order.verify(questionRepository).deleteAllByQuizId(20L);
		order.verify(quizRepository).delete(quiz);
	}

	@Test
	void createsFirstQuestionAtPositionOne() {
		stubQuiz();
		when(questionRepository.save(any(QuizQuestion.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.createQuestion(1L, 10L, new QuizQuestionManagementRequest("  First?  "));

		ArgumentCaptor<QuizQuestion> captor = ArgumentCaptor.forClass(QuizQuestion.class);
		verify(questionRepository).save(captor.capture());
		assertThat(captor.getValue().getQuiz()).isSameAs(quiz);
		assertThat(captor.getValue().getQuestionText()).isEqualTo("First?");
		assertThat(captor.getValue().getPosition()).isEqualTo(1);
	}

	@Test
	void createsLaterQuestionAfterMaximumPosition() {
		stubQuiz();
		when(questionRepository.findTopByQuizIdOrderByPositionDesc(20L)).thenReturn(Optional.of(question));
		when(questionRepository.save(any(QuizQuestion.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.createQuestion(1L, 10L, new QuizQuestionManagementRequest("Later?"));

		ArgumentCaptor<QuizQuestion> captor = ArgumentCaptor.forClass(QuizQuestion.class);
		verify(questionRepository).save(captor.capture());
		assertThat(captor.getValue().getPosition()).isEqualTo(5);
	}

	@Test
	void updatesQuestionTextWithoutChangingPosition() {
		stubQuestion();

		service.updateQuestion(1L, 10L, 30L, new QuizQuestionManagementRequest("  Updated?  "));

		assertThat(question.getQuestionText()).isEqualTo("Updated?");
		assertThat(question.getPosition()).isEqualTo(4);
	}

	@Test
	void rejectsQuestionBelongingToAnotherQuiz() {
		stubQuiz();
		when(questionRepository.findByIdAndQuizId(30L, 20L)).thenReturn(Optional.empty());

		assertStatus(HttpStatus.NOT_FOUND,
				() -> service.updateQuestion(1L, 10L, 30L, new QuizQuestionManagementRequest("Updated")));
	}

	@Test
	void deletesQuestionOptionsBeforeQuestion() {
		stubQuestion();

		service.deleteQuestion(1L, 10L, 30L);

		InOrder order = inOrder(optionRepository, questionRepository);
		order.verify(optionRepository).deleteAllByQuestionId(30L);
		order.verify(questionRepository).delete(question);
	}

	@Test
	void createsFirstOptionAtPositionOne() {
		stubQuestion();
		when(optionRepository.save(any(AnswerOption.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.createOption(1L, 10L, 30L, new AnswerOptionManagementRequest("  First  ", false));

		ArgumentCaptor<AnswerOption> captor = ArgumentCaptor.forClass(AnswerOption.class);
		verify(optionRepository).save(captor.capture());
		assertThat(captor.getValue().getQuestion()).isSameAs(question);
		assertThat(captor.getValue().getOptionText()).isEqualTo("First");
		assertThat(captor.getValue().getPosition()).isEqualTo(1);
	}

	@Test
	void createsLaterOptionAfterMaximumPosition() {
		stubQuestion();
		AnswerOption existing = option(40L, question, "Existing", false, 6);
		when(optionRepository.findTopByQuestionIdOrderByPositionDesc(30L)).thenReturn(Optional.of(existing));
		when(optionRepository.save(any(AnswerOption.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.createOption(1L, 10L, 30L, new AnswerOptionManagementRequest("Later", false));

		ArgumentCaptor<AnswerOption> captor = ArgumentCaptor.forClass(AnswerOption.class);
		verify(optionRepository).save(captor.capture());
		assertThat(captor.getValue().getPosition()).isEqualTo(7);
	}

	@Test
	void updatesOptionDetailsWithoutChangingPosition() {
		stubQuestion();
		AnswerOption target = option(40L, question, "Old", true, 3);
		when(optionRepository.findByIdAndQuestionId(40L, 30L)).thenReturn(Optional.of(target));

		service.updateOption(1L, 10L, 30L, 40L, new AnswerOptionManagementRequest("  Updated  ", false));

		assertThat(target.getOptionText()).isEqualTo("Updated");
		assertThat(target.isCorrect()).isFalse();
		assertThat(target.getPosition()).isEqualTo(3);
	}

	@Test
	void rejectsOptionBelongingToAnotherQuestion() {
		stubQuestion();
		when(optionRepository.findByIdAndQuestionId(40L, 30L)).thenReturn(Optional.empty());

		assertStatus(HttpStatus.NOT_FOUND, () -> service.updateOption(1L, 10L, 30L, 40L,
				new AnswerOptionManagementRequest("Updated", false)));
	}

	@Test
	void creatingCorrectOptionClearsExistingCorrectOptionForQuestion() {
		stubQuestion();
		AnswerOption previous = option(40L, question, "Previous", true, 1);
		when(optionRepository.findByQuestionIdAndCorrectTrue(30L)).thenReturn(List.of(previous));
		when(optionRepository.save(any(AnswerOption.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.createOption(1L, 10L, 30L, new AnswerOptionManagementRequest("New", true));

		assertThat(previous.isCorrect()).isFalse();
		ArgumentCaptor<AnswerOption> captor = ArgumentCaptor.forClass(AnswerOption.class);
		verify(optionRepository).save(captor.capture());
		assertThat(captor.getValue().isCorrect()).isTrue();
	}

	@Test
	void updatingCorrectOptionClearsOnlyOtherCorrectOptionForSameQuestion() {
		stubQuestion();
		AnswerOption target = option(40L, question, "Target", false, 1);
		AnswerOption previous = option(41L, question, "Previous", true, 2);
		QuizQuestion otherQuestion = new QuizQuestion(31L, quiz, "Other", 5);
		AnswerOption unrelated = option(42L, otherQuestion, "Unrelated", true, 1);
		when(optionRepository.findByIdAndQuestionId(40L, 30L)).thenReturn(Optional.of(target));
		when(optionRepository.findByQuestionIdAndCorrectTrue(30L)).thenReturn(List.of(target, previous));

		service.updateOption(1L, 10L, 30L, 40L, new AnswerOptionManagementRequest("Target", true));

		assertThat(target.isCorrect()).isTrue();
		assertThat(previous.isCorrect()).isFalse();
		assertThat(unrelated.isCorrect()).isTrue();
		verify(optionRepository, never()).findByQuestionIdAndCorrectTrue(31L);
	}

	@Test
	void settingCorrectFalseDoesNotClearOtherOptions() {
		stubQuestion();
		AnswerOption target = option(40L, question, "Target", true, 1);
		AnswerOption other = option(41L, question, "Other", false, 2);
		when(optionRepository.findByIdAndQuestionId(40L, 30L)).thenReturn(Optional.of(target));

		service.updateOption(1L, 10L, 30L, 40L, new AnswerOptionManagementRequest("Target", false));

		assertThat(target.isCorrect()).isFalse();
		assertThat(other.isCorrect()).isFalse();
		verify(optionRepository, never()).findByQuestionIdAndCorrectTrue(any());
	}

	@Test
	void deletesOnlyCorrectlyNestedOption() {
		stubQuestion();
		AnswerOption target = option(40L, question, "Target", false, 1);
		when(optionRepository.findByIdAndQuestionId(40L, 30L)).thenReturn(Optional.of(target));

		service.deleteOption(1L, 10L, 30L, 40L);

		verify(optionRepository).delete(target);
		verify(optionRepository, never()).deleteAllByQuestionId(any());
	}

	private void stubModule() {
		when(moduleRepository.findByIdAndCourseId(10L, 1L)).thenReturn(Optional.of(module));
	}

	private void stubQuiz() {
		stubModule();
		when(quizRepository.findByModuleIdAndModuleCourseId(10L, 1L)).thenReturn(Optional.of(quiz));
	}

	private void stubQuestion() {
		stubQuiz();
		when(questionRepository.findByIdAndQuizId(30L, 20L)).thenReturn(Optional.of(question));
	}

	private AnswerOption option(Long id, QuizQuestion owner, String text, boolean correct, int position) {
		return new AnswerOption(id, owner, text, correct, position);
	}

	private void assertStatus(HttpStatus status, org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
		assertThatThrownBy(action).isInstanceOfSatisfying(ResponseStatusException.class,
				exception -> assertThat(exception.getStatusCode()).isEqualTo(status));
	}
}
