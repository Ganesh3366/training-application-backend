package com.ganesh.training_application_backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

import com.ganesh.training_application_backend.course.dto.CourseResponse;

@ExtendWith(MockitoExtension.class)
class CourseServiceTests {

	@Mock
	private CourseRepository courseRepository;

	@InjectMocks
	private CourseService courseService;

	@Test
	void returnsAllCoursesAsResponseDtos() {
		Course course = course(1L);
		when(courseRepository.findAll()).thenReturn(List.of(course));

		List<CourseResponse> result = courseService.getAllCourses();

		assertThat(result).containsExactly(new CourseResponse(
				1L,
				"Spring Boot Foundations",
				"Build maintainable Spring applications",
				"Ganesh",
				120,
				"Beginner",
				"Information Technology (IT)"));
	}

	@Test
	void returnsCourseByIdAsResponseDto() {
		Course course = course(7L);
		when(courseRepository.findById(7L)).thenReturn(Optional.of(course));

		CourseResponse result = courseService.getCourseById(7L);

		assertThat(result.id()).isEqualTo(7L);
		assertThat(result.title()).isEqualTo("Spring Boot Foundations");
	}

	@Test
	void throwsNotFoundWhenCourseDoesNotExist() {
		when(courseRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> courseService.getCourseById(99L))
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
}
