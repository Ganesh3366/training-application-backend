package com.ganesh.training_application_backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

@ExtendWith(MockitoExtension.class)
class DemoCourseDataSeederTests {

	@Mock
	private CourseRepository courseRepository;

	@Mock
	private CourseModuleRepository courseModuleRepository;

	@Mock
	private ModuleContentRepository moduleContentRepository;

	@Mock
	private QuizRepository quizRepository;

	@Mock
	private QuizQuestionRepository quizQuestionRepository;

	@Mock
	private AnswerOptionRepository answerOptionRepository;

	@InjectMocks
	private DemoCourseDataSeeder seeder;

	@Test
	void createsOneCourseWithOrderedModulesAndContentsWhenRepositoryIsEmpty() {
		when(courseRepository.count()).thenReturn(0L);
		when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(courseModuleRepository.save(any(CourseModule.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(quizRepository.save(any(Quiz.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(quizQuestionRepository.save(any(QuizQuestion.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		seeder.run(new DefaultApplicationArguments(new String[0]));

		ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
		verify(courseRepository).save(courseCaptor.capture());
		Course savedCourse = courseCaptor.getValue();
		assertThat(savedCourse.getId()).isNull();
		assertThat(savedCourse.getTitle()).isEqualTo("Introduction to Angular");
		assertThat(savedCourse.getInstructor()).isEqualTo("John Doe");
		assertThat(savedCourse.getLevel()).isEqualTo(CourseLevel.BEGINNER);
		assertThat(savedCourse.getCategory()).isEqualTo(CourseCategory.INFORMATION_TECHNOLOGY);

		ArgumentCaptor<CourseModule> moduleCaptor = ArgumentCaptor.forClass(CourseModule.class);
		verify(courseModuleRepository, org.mockito.Mockito.times(2)).save(moduleCaptor.capture());
		List<CourseModule> modules = moduleCaptor.getAllValues();
		assertThat(modules).extracting(CourseModule::getPosition).containsExactly(1, 2);
		assertThat(modules).allSatisfy(module -> assertThat(module.getCourse()).isSameAs(savedCourse));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<ModuleContent>> contentCaptor = ArgumentCaptor.forClass(List.class);
		verify(moduleContentRepository).saveAll(contentCaptor.capture());
		List<ModuleContent> contents = contentCaptor.getValue();
		assertThat(contents).hasSize(5);
		assertThat(contents.subList(0, 3)).extracting(ModuleContent::getPosition).containsExactly(1, 2, 3);
		assertThat(contents.subList(3, 5)).extracting(ModuleContent::getPosition).containsExactly(1, 2);
		assertThat(contents).filteredOn(content -> content.getType() == ModuleContentType.VIDEO)
				.extracting(ModuleContent::getVideoUrl)
				.allMatch(url -> url.startsWith("https://www.youtube.com/watch?v="));
		assertThat(contents).filteredOn(content -> content.getType() == ModuleContentType.TEXT)
				.extracting(ModuleContent::getTextContent)
				.doesNotContainNull();

		ArgumentCaptor<Quiz> quizCaptor = ArgumentCaptor.forClass(Quiz.class);
		verify(quizRepository, org.mockito.Mockito.times(2)).save(quizCaptor.capture());
		List<Quiz> quizzes = quizCaptor.getAllValues();
		assertThat(quizzes).extracting(Quiz::getTitle)
				.containsExactly("Angular Fundamentals Quiz", "Components and Templates Quiz");
		assertThat(quizzes).extracting(Quiz::getPassingScore).containsExactly(70, 70);
		assertThat(quizzes.get(0).getModule()).isSameAs(modules.get(0));
		assertThat(quizzes.get(1).getModule()).isSameAs(modules.get(1));

		ArgumentCaptor<QuizQuestion> questionCaptor = ArgumentCaptor.forClass(QuizQuestion.class);
		verify(quizQuestionRepository, org.mockito.Mockito.times(6)).save(questionCaptor.capture());
		List<QuizQuestion> questions = questionCaptor.getAllValues();
		assertThat(questions).hasSize(6);
		assertThat(questions.subList(0, 3)).extracting(QuizQuestion::getPosition).containsExactly(1, 2, 3);
		assertThat(questions.subList(3, 6)).extracting(QuizQuestion::getPosition).containsExactly(1, 2, 3);
		assertThat(questions.subList(0, 3)).allSatisfy(question ->
				assertThat(question.getQuiz()).isSameAs(quizzes.get(0)));
		assertThat(questions.subList(3, 6)).allSatisfy(question ->
				assertThat(question.getQuiz()).isSameAs(quizzes.get(1)));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<AnswerOption>> optionCaptor = ArgumentCaptor.forClass(List.class);
		verify(answerOptionRepository).saveAll(optionCaptor.capture());
		List<AnswerOption> options = optionCaptor.getValue();
		assertThat(options).hasSize(24);
		assertThat(questions).allSatisfy(question -> {
			List<AnswerOption> questionOptions = options.stream()
					.filter(option -> option.getQuestion() == question)
					.toList();
			assertThat(questionOptions).hasSize(4);
			assertThat(questionOptions).extracting(AnswerOption::getPosition).containsExactly(1, 2, 3, 4);
			assertThat(questionOptions).filteredOn(AnswerOption::isCorrect).hasSize(1);
		});
	}

	@Test
	void skipsAllInsertsWhenCourseDataAlreadyExists() {
		when(courseRepository.count()).thenReturn(1L);

		seeder.run(new DefaultApplicationArguments(new String[0]));

		verify(courseRepository, never()).save(any(Course.class));
		verifyNoInteractions(courseModuleRepository, moduleContentRepository, quizRepository,
				quizQuestionRepository, answerOptionRepository);
	}
}
