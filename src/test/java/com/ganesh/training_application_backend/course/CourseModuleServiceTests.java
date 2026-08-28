package com.ganesh.training_application_backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.ganesh.training_application_backend.course.dto.CourseModuleResponse;
import com.ganesh.training_application_backend.course.dto.CourseModuleSummaryResponse;

@ExtendWith(MockitoExtension.class)
class CourseModuleServiceTests {

	@Mock
	private CourseRepository courseRepository;

	@Mock
	private CourseModuleRepository courseModuleRepository;

	@Mock
	private ModuleContentRepository moduleContentRepository;

	@InjectMocks
	private CourseModuleService courseModuleService;

	@Test
	void returnsModulesAsOrderedSummaryDtos() {
		Course course = course(1L);
		CourseModule first = module(10L, course, "Introduction", 1);
		CourseModule second = module(11L, course, "Core concepts", 2);
		when(courseRepository.existsById(1L)).thenReturn(true);
		when(courseModuleRepository.findByCourseIdOrderByPositionAsc(1L))
				.thenReturn(List.of(first, second));

		List<CourseModuleSummaryResponse> result = courseModuleService.getModulesByCourseId(1L);

		assertThat(result).containsExactly(
				new CourseModuleSummaryResponse(10L, "Introduction", "Introduction description", 1),
				new CourseModuleSummaryResponse(11L, "Core concepts", "Core concepts description", 2));
	}

	@Test
	void mapsOrderedTextAndVideoContentToModuleDetail() {
		CourseModule module = module(10L, course(1L), "Introduction", 1);
		ModuleContent text = new ModuleContent(
				100L, module, ModuleContentType.TEXT, "Read this", "Plain text lesson", null, 1);
		ModuleContent video = new ModuleContent(
				101L, module, ModuleContentType.VIDEO, "Watch this", null,
				"https://www.youtube.com/watch?v=example", 2);
		when(courseModuleRepository.findByIdAndCourseId(10L, 1L)).thenReturn(Optional.of(module));
		when(moduleContentRepository.findByModuleIdOrderByPositionAsc(10L))
				.thenReturn(List.of(text, video));

		CourseModuleResponse result = courseModuleService.getModuleById(1L, 10L);

		assertThat(result.contents()).extracting(content -> content.position()).containsExactly(1, 2);
		assertThat(result.contents().get(0).type()).isEqualTo(ModuleContentType.TEXT);
		assertThat(result.contents().get(0).textContent()).isEqualTo("Plain text lesson");
		assertThat(result.contents().get(0).videoUrl()).isNull();
		assertThat(result.contents().get(1).type()).isEqualTo(ModuleContentType.VIDEO);
		assertThat(result.contents().get(1).textContent()).isNull();
		assertThat(result.contents().get(1).videoUrl())
				.isEqualTo("https://www.youtube.com/watch?v=example");
		verify(moduleContentRepository).findByModuleIdOrderByPositionAsc(10L);
	}

	@Test
	void returnsNotFoundWhenModuleDoesNotExistForCourse() {
		when(courseModuleRepository.findByIdAndCourseId(10L, 2L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> courseModuleService.getModuleById(2L, 10L))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
	}

	@Test
	void returnsNotFoundWhenListingModulesForUnknownCourse() {
		when(courseRepository.existsById(99L)).thenReturn(false);

		assertThatThrownBy(() -> courseModuleService.getModulesByCourseId(99L))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
	}

	private Course course(Long id) {
		return new Course(
				id,
				"Spring Boot Foundations",
				"Build maintainable Spring applications",
				"Ganesh",
				120,
				CourseLevel.BEGINNER,
				CourseCategory.INFORMATION_TECHNOLOGY);
	}

	private CourseModule module(Long id, Course course, String title, Integer position) {
		return new CourseModule(id, course, title, title + " description", position);
	}
}
