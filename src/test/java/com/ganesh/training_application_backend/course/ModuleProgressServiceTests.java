package com.ganesh.training_application_backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.ganesh.training_application_backend.auth.AppUser;
import com.ganesh.training_application_backend.auth.Role;
import com.ganesh.training_application_backend.course.dto.CourseProgressResponse;
import com.ganesh.training_application_backend.course.dto.CourseProgressStatus;

@ExtendWith(MockitoExtension.class)
class ModuleProgressServiceTests {

	@Mock CourseRepository courseRepository;
	@Mock CourseModuleRepository courseModuleRepository;
	@Mock ModuleProgressRepository moduleProgressRepository;
	@InjectMocks ModuleProgressService service;

	@Test
	void returnsOrderedCompletedAndPendingModulesUsingOneProgressQuery() {
		Course course = new Course(1L, "Course", "Description", "Instructor", 60,
				CourseLevel.BEGINNER, CourseCategory.INFORMATION_TECHNOLOGY);
		CourseModule first = new CourseModule(10L, course, "First", null, 1);
		CourseModule second = new CourseModule(11L, course, "Second", null, 2);
		ModuleProgress progress = new ModuleProgress(
				new AppUser(7L, "Learner", "learner@example.com", "hash", Role.USER), first);
		progress.recordAttempt(100, true, Instant.parse("2026-01-01T10:00:00Z"));
		when(courseRepository.existsById(1L)).thenReturn(true);
		when(courseModuleRepository.findByCourseIdOrderByPositionAsc(1L)).thenReturn(List.of(first, second));
		when(moduleProgressRepository.findByUserIdAndModuleCourseId(7L, 1L)).thenReturn(List.of(progress));

		CourseProgressResponse response = service.getCourseProgress(1L, 7L);

		assertThat(response.totalModules()).isEqualTo(2);
		assertThat(response.completedModules()).isEqualTo(1);
		assertThat(response.pendingModules()).isEqualTo(1);
		assertThat(response.progressPercentage()).isEqualTo(50);
		assertThat(response.completed()).isFalse();
		assertThat(response.status()).isEqualTo(CourseProgressStatus.IN_PROGRESS);
		assertThat(response.modules()).extracting(module -> module.moduleId()).containsExactly(10L, 11L);
		assertThat(response.modules().get(1).attemptsCount()).isZero();
		assertThat(response.modules().get(1).lastScore()).isNull();
		verify(moduleProgressRepository).findByUserIdAndModuleCourseId(7L, 1L);
	}

	@Test
	void returnsNotStartedForZeroOfTwoCompletedModules() {
		Course course = course();
		CourseModule first = new CourseModule(10L, course, "First", null, 1);
		CourseModule second = new CourseModule(11L, course, "Second", null, 2);
		when(courseRepository.existsById(1L)).thenReturn(true);
		when(courseModuleRepository.findByCourseIdOrderByPositionAsc(1L)).thenReturn(List.of(first, second));
		when(moduleProgressRepository.findByUserIdAndModuleCourseId(7L, 1L)).thenReturn(List.of());

		CourseProgressResponse response = service.getCourseProgress(1L, 7L);

		assertThat(response.progressPercentage()).isZero();
		assertThat(response.completed()).isFalse();
		assertThat(response.status()).isEqualTo(CourseProgressStatus.NOT_STARTED);
		assertThat(response.pendingModules()).isEqualTo(2);
	}

	@Test
	void returnsInProgressWhenAQuizWasAttemptedButNoModuleIsComplete() {
		Course course = course();
		CourseModule first = new CourseModule(10L, course, "First", null, 1);
		CourseModule second = new CourseModule(11L, course, "Second", null, 2);
		ModuleProgress progress = new ModuleProgress(
				new AppUser(7L, "Learner", "learner@example.com", "hash", Role.USER), first);
		progress.recordAttempt(40, false, Instant.parse("2026-01-01T10:00:00Z"));
		when(courseRepository.existsById(1L)).thenReturn(true);
		when(courseModuleRepository.findByCourseIdOrderByPositionAsc(1L)).thenReturn(List.of(first, second));
		when(moduleProgressRepository.findByUserIdAndModuleCourseId(7L, 1L)).thenReturn(List.of(progress));

		CourseProgressResponse response = service.getCourseProgress(1L, 7L);

		assertThat(response.completedModules()).isZero();
		assertThat(response.pendingModules()).isEqualTo(2);
		assertThat(response.progressPercentage()).isZero();
		assertThat(response.completed()).isFalse();
		assertThat(response.status()).isEqualTo(CourseProgressStatus.IN_PROGRESS);
		assertThat(response.modules().get(0).attemptsCount()).isEqualTo(1);
	}

	@Test
	void returnsCompletedForTwoOfTwoCompletedModules() {
		Course course = course();
		CourseModule first = new CourseModule(10L, course, "First", null, 1);
		CourseModule second = new CourseModule(11L, course, "Second", null, 2);
		AppUser learner = new AppUser(7L, "Learner", "learner@example.com", "hash", Role.USER);
		ModuleProgress firstProgress = new ModuleProgress(learner, first);
		ModuleProgress secondProgress = new ModuleProgress(learner, second);
		firstProgress.recordAttempt(90, true, Instant.parse("2026-01-01T10:00:00Z"));
		secondProgress.recordAttempt(80, true, Instant.parse("2026-01-02T10:00:00Z"));
		when(courseRepository.existsById(1L)).thenReturn(true);
		when(courseModuleRepository.findByCourseIdOrderByPositionAsc(1L)).thenReturn(List.of(first, second));
		when(moduleProgressRepository.findByUserIdAndModuleCourseId(7L, 1L))
				.thenReturn(List.of(firstProgress, secondProgress));

		CourseProgressResponse response = service.getCourseProgress(1L, 7L);

		assertThat(response.progressPercentage()).isEqualTo(100);
		assertThat(response.completed()).isTrue();
		assertThat(response.status()).isEqualTo(CourseProgressStatus.COMPLETED);
		assertThat(response.pendingModules()).isZero();
	}

	@Test
	void zeroModuleCourseRemainsNotStarted() {
		when(courseRepository.existsById(1L)).thenReturn(true);
		when(courseModuleRepository.findByCourseIdOrderByPositionAsc(1L)).thenReturn(List.of());
		when(moduleProgressRepository.findByUserIdAndModuleCourseId(7L, 1L)).thenReturn(List.of());

		CourseProgressResponse response = service.getCourseProgress(1L, 7L);

		assertThat(response.totalModules()).isZero();
		assertThat(response.progressPercentage()).isZero();
		assertThat(response.completed()).isFalse();
		assertThat(response.status()).isEqualTo(CourseProgressStatus.NOT_STARTED);
	}

	@Test
	void unknownCourseUsesExistingNotFoundStyle() {
		when(courseRepository.existsById(99L)).thenReturn(false);
		assertThatThrownBy(() -> service.getCourseProgress(99L, 7L))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
	}

	private Course course() {
		return new Course(1L, "Course", "Description", "Instructor", 60,
				CourseLevel.BEGINNER, CourseCategory.INFORMATION_TECHNOLOGY);
	}
}
