package com.ganesh.training_application_backend.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ganesh.training_application_backend.admin.CourseAssignment;
import com.ganesh.training_application_backend.admin.CourseAssignmentRepository;
import com.ganesh.training_application_backend.auth.AppUser;
import com.ganesh.training_application_backend.auth.Role;
import com.ganesh.training_application_backend.course.Certificate;
import com.ganesh.training_application_backend.course.CertificateRepository;
import com.ganesh.training_application_backend.course.Course;
import com.ganesh.training_application_backend.course.CourseCategory;
import com.ganesh.training_application_backend.course.CourseLevel;
import com.ganesh.training_application_backend.course.CourseModule;
import com.ganesh.training_application_backend.course.CourseModuleRepository;
import com.ganesh.training_application_backend.course.ModuleProgress;
import com.ganesh.training_application_backend.course.ModuleProgressRepository;
import com.ganesh.training_application_backend.course.dto.CourseProgressStatus;
import com.ganesh.training_application_backend.reporting.dto.LearnerCourseReportResponse;

@ExtendWith(MockitoExtension.class)
class LearnerProgressReportServiceTests {

	@Mock CourseAssignmentRepository assignmentRepository;
	@Mock CourseModuleRepository moduleRepository;
	@Mock ModuleProgressRepository progressRepository;
	@Mock CertificateRepository certificateRepository;
	@InjectMocks LearnerProgressReportService service;

	@Test
	void assignedLearnerWithoutProgressAppearsAsNotStartedWithModuleDefaults() {
		TestData data = testData();
		stubReads(data, List.of(), List.of());

		LearnerCourseReportResponse response = service.getReports().get(0);

		assertThat(response.learnerId()).isEqualTo(7L);
		assertThat(response.learnerName()).isEqualTo("Learner");
		assertThat(response.learnerEmail()).isEqualTo("learner@example.com");
		assertThat(response.status()).isEqualTo(CourseProgressStatus.NOT_STARTED);
		assertThat(response.completedModules()).isZero();
		assertThat(response.pendingModules()).isEqualTo(2);
		assertThat(response.progressPercentage()).isZero();
		assertThat(response.modules()).hasSize(2).allSatisfy(module -> {
			assertThat(module.completed()).isFalse();
			assertThat(module.lastScore()).isNull();
			assertThat(module.bestScore()).isNull();
			assertThat(module.attemptCount()).isZero();
		});
	}

	@Test
	void partialProgressReturnsCountsAndModuleQuizAggregates() {
		TestData data = testData();
		ModuleProgress progress = new ModuleProgress(data.learner(), data.modules().get(0));
		recordAttempt(progress, 90, true, Instant.parse("2026-01-01T10:00:00Z"));
		recordAttempt(progress, 70, false, Instant.parse("2026-01-02T10:00:00Z"));
		stubReads(data, List.of(progress), List.of());

		LearnerCourseReportResponse response = service.getReports().get(0);

		assertThat(response.completedModules()).isEqualTo(1);
		assertThat(response.pendingModules()).isEqualTo(1);
		assertThat(response.progressPercentage()).isEqualTo(50);
		assertThat(response.status()).isEqualTo(CourseProgressStatus.IN_PROGRESS);
		assertThat(response.completionDate()).isNull();
		assertThat(response.modules().get(0).lastScore()).isEqualTo(70);
		assertThat(response.modules().get(0).bestScore()).isEqualTo(90);
		assertThat(response.modules().get(0).attemptCount()).isEqualTo(2);
	}

	@Test
	void unassignedFailedOnlyProgressCreatesAnInProgressReportRow() {
		TestData data = testData();
		ModuleProgress progress = new ModuleProgress(data.learner(), data.modules().get(0));
		recordAttempt(progress, 60, false, Instant.parse("2026-01-01T10:00:00Z"));
		recordAttempt(progress, 40, false, Instant.parse("2026-01-02T10:00:00Z"));
		stubReads(data, List.of(), List.of(progress), List.of());

		LearnerCourseReportResponse response = service.getReports().get(0);

		assertThat(response.learnerId()).isEqualTo(data.learner().getId());
		assertThat(response.courseId()).isEqualTo(data.course().getId());
		assertThat(response.status()).isEqualTo(CourseProgressStatus.IN_PROGRESS);
		assertThat(response.progressPercentage()).isZero();
		assertThat(response.completedModules()).isZero();
		assertThat(response.pendingModules()).isEqualTo(2);
		assertThat(response.modules().get(0).completed()).isFalse();
		assertThat(response.modules().get(0).attemptCount()).isEqualTo(2);
		assertThat(response.modules().get(0).lastScore()).isEqualTo(40);
		assertThat(response.modules().get(0).bestScore()).isEqualTo(60);
		assertThat(response.modules().get(0).completedAt()).isNull();
	}

	@Test
	void assignedCourseWithActivityProducesOnlyOneReportRow() {
		TestData data = testData();
		ModuleProgress progress = new ModuleProgress(data.learner(), data.modules().get(0));
		recordAttempt(progress, 50, false, Instant.parse("2026-01-01T10:00:00Z"));
		stubReads(data, List.of(data.assignment()), List.of(progress), List.of());

		List<LearnerCourseReportResponse> responses = service.getReports();

		assertThat(responses).hasSize(1);
		assertThat(responses.get(0).learnerId()).isEqualTo(data.learner().getId());
		assertThat(responses.get(0).courseId()).isEqualTo(data.course().getId());
		assertThat(responses.get(0).modules().get(0).attemptCount()).isEqualTo(1);
	}

	@Test
	void completedCourseReturnsCertificateAndCompletionDate() {
		TestData data = testData();
		ModuleProgress first = new ModuleProgress(data.learner(), data.modules().get(0));
		ModuleProgress second = new ModuleProgress(data.learner(), data.modules().get(1));
		recordAttempt(first, 90, true, Instant.parse("2026-01-01T10:00:00Z"));
		recordAttempt(second, 85, true, Instant.parse("2026-01-02T10:00:00Z"));
		Certificate certificate = new Certificate(data.learner(), data.course(), "Learner", "Course",
				LocalDate.of(2026, 1, 2), 85, "SF-2026-001", Instant.parse("2026-01-02T10:01:00Z"));
		stubReads(data, List.of(first, second), List.of(certificate));

		LearnerCourseReportResponse response = service.getReports().get(0);

		assertThat(response.status()).isEqualTo(CourseProgressStatus.COMPLETED);
		assertThat(response.progressPercentage()).isEqualTo(100);
		assertThat(response.pendingModules()).isZero();
		assertThat(response.completionDate()).isEqualTo(LocalDate.of(2026, 1, 2));
		assertThat(response.certificateNumber()).isEqualTo("SF-2026-001");
	}

	private void stubReads(TestData data, List<ModuleProgress> progress, List<Certificate> certificates) {
		stubReads(data, List.of(data.assignment()), progress, certificates);
	}

	private void stubReads(TestData data, List<CourseAssignment> assignments, List<ModuleProgress> progress,
			List<Certificate> certificates) {
		when(assignmentRepository.findAllByOrderByIdAsc()).thenReturn(assignments);
		when(progressRepository.findAllForReporting()).thenReturn(progress);
		when(moduleRepository.findByCourseIdInOrderByCourseIdAscPositionAsc(Set.of(1L)))
				.thenReturn(data.modules());
		when(certificateRepository.findByUserIdInAndCourseIdIn(Set.of(7L), Set.of(1L)))
				.thenReturn(certificates);
	}

	private void recordAttempt(ModuleProgress progress, int score, boolean passed, Instant attemptedAt) {
		org.springframework.test.util.ReflectionTestUtils.invokeMethod(
				progress, "recordAttempt", score, passed, attemptedAt);
	}

	private TestData testData() {
		AppUser learner = new AppUser(7L, "Learner", "learner@example.com", "secret-hash", Role.USER);
		Course course = new Course(1L, "Course", "Description", "Instructor", 60,
				CourseLevel.BEGINNER, CourseCategory.INFORMATION_TECHNOLOGY);
		List<CourseModule> modules = List.of(
				new CourseModule(10L, course, "First", null, 1),
				new CourseModule(11L, course, "Second", null, 2));
		return new TestData(learner, course, modules,
				new CourseAssignment(learner, course, Instant.parse("2025-12-01T10:00:00Z")));
	}

	private record TestData(AppUser learner, Course course, List<CourseModule> modules,
			CourseAssignment assignment) {
	}
}
