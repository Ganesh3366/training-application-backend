package com.ganesh.training_application_backend.course;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true")
public class DemoCourseDataSeeder implements ApplicationRunner {

	private static final Logger logger = LoggerFactory.getLogger(DemoCourseDataSeeder.class);

	private final CourseRepository courseRepository;
	private final CourseModuleRepository courseModuleRepository;
	private final ModuleContentRepository moduleContentRepository;
	private final QuizRepository quizRepository;
	private final QuizQuestionRepository quizQuestionRepository;
	private final AnswerOptionRepository answerOptionRepository;

	public DemoCourseDataSeeder(CourseRepository courseRepository, CourseModuleRepository courseModuleRepository,
			ModuleContentRepository moduleContentRepository, QuizRepository quizRepository,
			QuizQuestionRepository quizQuestionRepository, AnswerOptionRepository answerOptionRepository) {
		this.courseRepository = courseRepository;
		this.courseModuleRepository = courseModuleRepository;
		this.moduleContentRepository = moduleContentRepository;
		this.quizRepository = quizRepository;
		this.quizQuestionRepository = quizQuestionRepository;
		this.answerOptionRepository = answerOptionRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (courseRepository.count() > 0) {
			logger.info("Demo data seeding skipped because course data already exists");
			return;
		}

		Course course = courseRepository.save(new Course(
				null,
				"Introduction to Angular",
				"Learn the fundamentals of Angular, including components, templates, and the basic structure of an Angular application.",
				"John Doe",
				10,
				CourseLevel.BEGINNER,
				CourseCategory.INFORMATION_TECHNOLOGY));

		CourseModule fundamentals = courseModuleRepository.save(new CourseModule(
				null,
				course,
				"Angular Fundamentals",
				"Understand what Angular is and how an Angular application is structured.",
				1));
		CourseModule components = courseModuleRepository.save(new CourseModule(
				null,
				course,
				"Components and Templates",
				"Learn how Angular components and templates work together.",
				2));

		moduleContentRepository.saveAll(List.of(
				new ModuleContent(
						null,
						fundamentals,
						ModuleContentType.TEXT,
						"What is Angular?",
						"Angular is a framework for building web applications with components, templates, TypeScript, dependency injection, and an integrated set of platform features.",
						null,
						1),
				new ModuleContent(
						null,
						fundamentals,
						ModuleContentType.VIDEO,
						"Angular Introduction Video",
						null,
						"https://www.youtube.com/watch?v=k5E2AVpwsko",
						2),
				new ModuleContent(
						null,
						fundamentals,
						ModuleContentType.TEXT,
						"Angular Fundamentals Summary",
						"Angular applications organize TypeScript logic into components, connect it to templates, and use services and dependency injection to share behavior.",
						null,
						3),
				new ModuleContent(
						null,
						components,
						ModuleContentType.TEXT,
						"Understanding Components",
						"An Angular component combines a TypeScript component class with a template and a selector that identifies where the component appears in the application.",
						null,
						1),
				new ModuleContent(
						null,
						components,
						ModuleContentType.VIDEO,
						"Angular Components Video",
						null,
						"https://www.youtube.com/watch?v=3dHNOWTI7H8",
						2)));

		Quiz fundamentalsQuiz = quizRepository.save(new Quiz(null, fundamentals, "Angular Fundamentals Quiz", 70));
		QuizQuestion languageQuestion = quizQuestionRepository.save(new QuizQuestion(
				null, fundamentalsQuiz, "What is the primary programming language used by Angular?", 1));
		QuizQuestion componentQuestion = quizQuestionRepository.save(new QuizQuestion(
				null, fundamentalsQuiz, "What does an Angular component represent?", 2));
		QuizQuestion injectionQuestion = quizQuestionRepository.save(new QuizQuestion(
				null, fundamentalsQuiz, "What is the purpose of dependency injection in Angular?", 3));
		Quiz componentsQuiz = quizRepository.save(new Quiz(null, components, "Components and Templates Quiz", 70));
		QuizQuestion componentMeaningQuestion = quizQuestionRepository.save(new QuizQuestion(
				null, componentsQuiz, "What does an Angular component represent?", 1));
		QuizQuestion interpolationQuestion = quizQuestionRepository.save(new QuizQuestion(
				null, componentsQuiz, "Which Angular syntax is used for text interpolation?", 2));
		QuizQuestion templateQuestion = quizQuestionRepository.save(new QuizQuestion(
				null, componentsQuiz, "What does an Angular component template define?", 3));

		answerOptionRepository.saveAll(List.of(
				new AnswerOption(null, languageQuestion, "TypeScript", true, 1),
				new AnswerOption(null, languageQuestion, "Java", false, 2),
				new AnswerOption(null, languageQuestion, "Python", false, 3),
				new AnswerOption(null, languageQuestion, "C#", false, 4),
				new AnswerOption(null, componentQuestion, "A reusable part of the user interface", true, 1),
				new AnswerOption(null, componentQuestion, "A PostgreSQL table", false, 2),
				new AnswerOption(null, componentQuestion, "A deployment server", false, 3),
				new AnswerOption(null, componentQuestion, "A CSS preprocessor", false, 4),
				new AnswerOption(null, injectionQuestion, "To provide dependencies to classes", true, 1),
				new AnswerOption(null, injectionQuestion, "To compile HTML into SQL", false, 2),
				new AnswerOption(null, injectionQuestion, "To replace component templates", false, 3),
				new AnswerOption(null, injectionQuestion, "To store video files", false, 4),
				new AnswerOption(null, componentMeaningQuestion, "A reusable part of the user interface", true, 1),
				new AnswerOption(null, componentMeaningQuestion, "A PostgreSQL database table", false, 2),
				new AnswerOption(null, componentMeaningQuestion, "A deployment server", false, 3),
				new AnswerOption(null, componentMeaningQuestion, "A CSS preprocessor", false, 4),
				new AnswerOption(null, interpolationQuestion, "{{ value }}", true, 1),
				new AnswerOption(null, interpolationQuestion, "[value]", false, 2),
				new AnswerOption(null, interpolationQuestion, "(value)", false, 3),
				new AnswerOption(null, interpolationQuestion, "*value", false, 4),
				new AnswerOption(null, templateQuestion, "The HTML structure displayed by the component", true, 1),
				new AnswerOption(null, templateQuestion, "The database schema for the application", false, 2),
				new AnswerOption(null, templateQuestion, "The backend server configuration", false, 3),
				new AnswerOption(null, templateQuestion, "The Maven dependency configuration", false, 4)));

		logger.info("Created SkillForge Angular demo course and quiz data");
	}
}
