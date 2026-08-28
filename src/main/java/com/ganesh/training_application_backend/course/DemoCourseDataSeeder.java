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

	public DemoCourseDataSeeder(CourseRepository courseRepository, CourseModuleRepository courseModuleRepository,
			ModuleContentRepository moduleContentRepository) {
		this.courseRepository = courseRepository;
		this.courseModuleRepository = courseModuleRepository;
		this.moduleContentRepository = moduleContentRepository;
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

		logger.info("Created SkillForge Angular demo course data");
	}
}
