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

	@InjectMocks
	private DemoCourseDataSeeder seeder;

	@Test
	void createsOneCourseWithOrderedModulesAndContentsWhenRepositoryIsEmpty() {
		when(courseRepository.count()).thenReturn(0L);
		when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(courseModuleRepository.save(any(CourseModule.class)))
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
	}

	@Test
	void skipsAllInsertsWhenCourseDataAlreadyExists() {
		when(courseRepository.count()).thenReturn(1L);

		seeder.run(new DefaultApplicationArguments(new String[0]));

		verify(courseRepository, never()).save(any(Course.class));
		verifyNoInteractions(courseModuleRepository, moduleContentRepository);
	}
}
