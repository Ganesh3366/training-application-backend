package com.ganesh.training_application_backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.ganesh.training_application_backend.course.dto.CourseCreateRequest;
import com.ganesh.training_application_backend.course.dto.CourseManagementResponse;
import com.ganesh.training_application_backend.course.dto.CourseUpdateRequest;

@ExtendWith(MockitoExtension.class)
class CourseManagementServiceTests {

	@Mock CourseRepository courseRepository;
	@Mock CourseModuleRepository courseModuleRepository;
	@Mock CertificateRepository certificateRepository;
	@InjectMocks CourseManagementService service;

	@Test
	void listsLightweightCoursesInRepositoryOrder() {
		when(courseRepository.findAllByOrderByIdAsc()).thenReturn(List.of(course(1L), course(2L)));

		List<CourseManagementResponse> response = service.getCourses();

		assertThat(response).extracting(CourseManagementResponse::id).containsExactly(1L, 2L);
	}

	@Test
	void getsExistingCourseAndRejectsUnknownCourse() {
		when(courseRepository.findById(1L)).thenReturn(Optional.of(course(1L)));
		when(courseRepository.findById(99L)).thenReturn(Optional.empty());

		assertThat(service.getCourse(1L).title()).isEqualTo("Course 1");
		assertStatus(HttpStatus.NOT_FOUND, () -> service.getCourse(99L));
	}

	@Test
	void createsValidatedCourseDataWithoutClientControlledId() {
		when(courseRepository.save(org.mockito.ArgumentMatchers.any(Course.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		service.createCourse(new CourseCreateRequest(" New Course ", " Description ", " Instructor ", 90,
				CourseLevel.INTERMEDIATE, CourseCategory.BUSINESS));

		ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
		verify(courseRepository).save(captor.capture());
		assertThat(captor.getValue().getId()).isNull();
		assertThat(captor.getValue().getTitle()).isEqualTo("New Course");
	}

	@Test
	void updatesExistingCourseAndRejectsUnknownCourse() {
		Course course = course(1L);
		CourseUpdateRequest request = new CourseUpdateRequest("Updated", "Updated description", "New Instructor",
				120, CourseLevel.ADVANCED, CourseCategory.ENGINEERING);
		when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
		when(courseRepository.findById(99L)).thenReturn(Optional.empty());

		CourseManagementResponse response = service.updateCourse(1L, request);

		assertThat(response.title()).isEqualTo("Updated");
		assertThat(response.level()).isEqualTo(CourseLevel.ADVANCED);
		assertStatus(HttpStatus.NOT_FOUND, () -> service.updateCourse(99L, request));
	}

	@Test
	void deletesCourseWithoutDependentRecords() {
		Course course = course(1L);
		when(courseRepository.findById(1L)).thenReturn(Optional.of(course));

		service.deleteCourse(1L);

		verify(courseRepository).delete(course);
	}

	@Test
	void rejectsDeletionWhenCourseHasModulesOrCertificates() {
		Course course = course(1L);
		when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
		when(courseModuleRepository.existsByCourseId(1L)).thenReturn(true);

		assertStatus(HttpStatus.CONFLICT, () -> service.deleteCourse(1L));
		verify(courseRepository, never()).delete(course);
	}

	@Test
	void rejectsDeletionWhenCourseHasCertificates() {
		Course course = course(1L);
		when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
		when(certificateRepository.existsByCourseId(1L)).thenReturn(true);

		assertStatus(HttpStatus.CONFLICT, () -> service.deleteCourse(1L));
		verify(courseRepository, never()).delete(course);
	}

	@Test
	void rejectsDeletionOfUnknownCourse() {
		when(courseRepository.findById(99L)).thenReturn(Optional.empty());

		assertStatus(HttpStatus.NOT_FOUND, () -> service.deleteCourse(99L));
	}

	private Course course(Long id) {
		return new Course(id, "Course " + id, "Description", "Instructor", 60,
				CourseLevel.BEGINNER, CourseCategory.INFORMATION_TECHNOLOGY);
	}

	private void assertStatus(HttpStatus status, org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
		assertThatThrownBy(action).isInstanceOfSatisfying(ResponseStatusException.class,
				exception -> assertThat(exception.getStatusCode()).isEqualTo(status));
	}
}
