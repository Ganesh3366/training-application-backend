package com.ganesh.training_application_backend.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.ganesh.training_application_backend.admin.dto.CourseAssignmentRequest;
import com.ganesh.training_application_backend.auth.AppUser;
import com.ganesh.training_application_backend.auth.AppUserRepository;
import com.ganesh.training_application_backend.auth.Role;
import com.ganesh.training_application_backend.course.Course;
import com.ganesh.training_application_backend.course.CourseCategory;
import com.ganesh.training_application_backend.course.CourseLevel;
import com.ganesh.training_application_backend.course.CourseRepository;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTests {

	@Mock AppUserRepository userRepository;
	@Mock CourseRepository courseRepository;
	@Mock CourseAssignmentRepository assignmentRepository;
	@InjectMocks AdminUserService service;

	@Test
	void listsSafeUsersInRepositoryOrderAndGetsOne() {
		when(userRepository.findAllByOrderByIdAsc()).thenReturn(List.of(user(1L), user(2L)));
		when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));

		assertThat(service.getUsers()).extracting(response -> response.id()).containsExactly(1L, 2L);
		assertThat(service.getUser(1L).email()).isEqualTo("user1@example.com");
	}

	@Test
	void assignsExistingCourseAndPersistsAuthoritativeReferences() {
		AppUser user = user(1L);
		Course course = course(5L);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(courseRepository.findById(5L)).thenReturn(Optional.of(course));
		when(assignmentRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		var response = service.assignCourse(1L, new CourseAssignmentRequest(5L));

		ArgumentCaptor<CourseAssignment> captor = ArgumentCaptor.forClass(CourseAssignment.class);
		verify(assignmentRepository).saveAndFlush(captor.capture());
		assertThat(captor.getValue().getUser()).isSameAs(user);
		assertThat(captor.getValue().getCourse()).isSameAs(course);
		assertThat(response.course().id()).isEqualTo(5L);
	}

	@Test
	void rejectsDuplicateBeforeSaving() {
		when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
		when(courseRepository.findById(5L)).thenReturn(Optional.of(course(5L)));
		when(assignmentRepository.existsByUserIdAndCourseId(1L, 5L)).thenReturn(true);

		assertStatus(HttpStatus.CONFLICT, () -> service.assignCourse(1L, new CourseAssignmentRequest(5L)));
		verify(assignmentRepository, never()).saveAndFlush(any());
	}

	@Test
	void translatesOnlyNamedUniqueConstraintRaceToConflict() {
		when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
		when(courseRepository.findById(5L)).thenReturn(Optional.of(course(5L)));
		when(assignmentRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException(
				"duplicate", new RuntimeException(CourseAssignment.USER_COURSE_CONSTRAINT)));

		assertStatus(HttpStatus.CONFLICT, () -> service.assignCourse(1L, new CourseAssignmentRequest(5L)));
	}

	@Test
	void propagatesDataIntegrityViolationForUnrelatedConstraint() {
		when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
		when(courseRepository.findById(5L)).thenReturn(Optional.of(course(5L)));
		DataIntegrityViolationException exception = new DataIntegrityViolationException(
				"insert failed", new RuntimeException("uk_other_constraint"));
		when(assignmentRepository.saveAndFlush(any())).thenThrow(exception);

		assertThatThrownBy(() -> service.assignCourse(1L, new CourseAssignmentRequest(5L)))
				.isSameAs(exception);
	}

	@Test
	void allowsDifferentUsersToBeAssignedTheSameCourse() {
		AppUser firstUser = user(1L);
		AppUser secondUser = user(2L);
		Course sharedCourse = course(5L);
		when(userRepository.findById(1L)).thenReturn(Optional.of(firstUser));
		when(userRepository.findById(2L)).thenReturn(Optional.of(secondUser));
		when(courseRepository.findById(5L)).thenReturn(Optional.of(sharedCourse));
		when(assignmentRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.assignCourse(1L, new CourseAssignmentRequest(5L));
		service.assignCourse(2L, new CourseAssignmentRequest(5L));

		verify(assignmentRepository).existsByUserIdAndCourseId(1L, 5L);
		verify(assignmentRepository).existsByUserIdAndCourseId(2L, 5L);
		ArgumentCaptor<CourseAssignment> captor = ArgumentCaptor.forClass(CourseAssignment.class);
		verify(assignmentRepository, times(2)).saveAndFlush(captor.capture());
		assertThat(captor.getAllValues()).extracting(CourseAssignment::getUser)
				.containsExactly(firstUser, secondUser);
		assertThat(captor.getAllValues()).extracting(CourseAssignment::getCourse)
				.containsExactly(sharedCourse, sharedCourse);
	}

	@Test
	void allowsOneUserToBeAssignedMultipleDifferentCourses() {
		AppUser sharedUser = user(1L);
		Course firstCourse = course(5L);
		Course secondCourse = course(6L);
		when(userRepository.findById(1L)).thenReturn(Optional.of(sharedUser));
		when(courseRepository.findById(5L)).thenReturn(Optional.of(firstCourse));
		when(courseRepository.findById(6L)).thenReturn(Optional.of(secondCourse));
		when(assignmentRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.assignCourse(1L, new CourseAssignmentRequest(5L));
		service.assignCourse(1L, new CourseAssignmentRequest(6L));

		verify(assignmentRepository).existsByUserIdAndCourseId(1L, 5L);
		verify(assignmentRepository).existsByUserIdAndCourseId(1L, 6L);
		ArgumentCaptor<CourseAssignment> captor = ArgumentCaptor.forClass(CourseAssignment.class);
		verify(assignmentRepository, times(2)).saveAndFlush(captor.capture());
		assertThat(captor.getAllValues()).extracting(CourseAssignment::getUser)
				.containsExactly(sharedUser, sharedUser);
		assertThat(captor.getAllValues()).extracting(CourseAssignment::getCourse)
				.containsExactly(firstCourse, secondCourse);
	}

	@Test
	void rejectsUnknownUserAndCourse() {
		when(userRepository.findById(99L)).thenReturn(Optional.empty());
		assertStatus(HttpStatus.NOT_FOUND, () -> service.assignCourse(99L, new CourseAssignmentRequest(5L)));

		when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
		when(courseRepository.findById(99L)).thenReturn(Optional.empty());
		assertStatus(HttpStatus.NOT_FOUND, () -> service.assignCourse(1L, new CourseAssignmentRequest(99L)));
	}

	@Test
	void listsAssignmentsOrEmptyAndRejectsUnknownUser() {
		when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L)));
		when(assignmentRepository.findAllByUserIdOrderByIdAsc(1L)).thenReturn(List.of());
		assertThat(service.getAssignments(1L)).isEmpty();

		when(userRepository.findById(99L)).thenReturn(Optional.empty());
		assertStatus(HttpStatus.NOT_FOUND, () -> service.getAssignments(99L));
	}

	private AppUser user(Long id) {
		return new AppUser(id, "User " + id, "user" + id + "@example.com", "secret-hash", Role.USER);
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
