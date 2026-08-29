package com.ganesh.training_application_backend.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.ganesh.training_application_backend.auth.AppUser;
import com.ganesh.training_application_backend.auth.AppUserRepository;
import com.ganesh.training_application_backend.auth.Role;
import com.ganesh.training_application_backend.course.dto.CertificateResponse;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTests {

	@Mock CourseRepository courseRepository;
	@Mock CourseModuleRepository courseModuleRepository;
	@Mock ModuleProgressRepository moduleProgressRepository;
	@Mock CertificateRepository certificateRepository;
	@Mock AppUserRepository appUserRepository;
	@InjectMocks CertificateService service;

	private Course course;
	private CourseModule firstModule;
	private CourseModule secondModule;
	private AppUser user;

	@BeforeEach
	void setUp() {
		course = course(1L, "Introduction to Angular");
		firstModule = module(10L, course, 1);
		secondModule = module(11L, course, 2);
		user = new AppUser(7L, "Learner Name", "learner@example.com", "hash", Role.USER);
	}

	@Test
	void incompleteCourseReturnsConflictWithoutCreatingCertificate() {
		stubCourse(List.of(firstModule, secondModule));
		when(moduleProgressRepository.findByUserIdAndModuleCourseId(7L, 1L))
				.thenReturn(List.of(completed(firstModule, 80, 40, "2026-08-27T10:00:00Z")));

		assertConflict(() -> service.getOrCreateCertificate(1L, 7L, "Learner Name"));
		verify(certificateRepository, never()).save(any());
	}

	@Test
	void zeroModuleCourseReturnsConflict() {
		stubCourse(List.of());

		assertConflict(() -> service.getOrCreateCertificate(1L, 7L, "Learner Name"));
		verify(moduleProgressRepository, never()).findByUserIdAndModuleCourseId(any(), any());
		verify(certificateRepository, never()).save(any());
	}

	@Test
	void completedCourseCreatesCertificateFromBestScoresAndLatestCompletion() {
		stubEligibleCourse();
		when(appUserRepository.getReferenceById(7L)).thenReturn(user);
		when(certificateRepository.save(any(Certificate.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CertificateResponse response = service.getOrCreateCertificate(1L, 7L, "Learner Name");

		assertThat(response.participantName()).isEqualTo("Learner Name");
		assertThat(response.courseName()).isEqualTo("Introduction to Angular");
		assertThat(response.finalScore()).isEqualTo(90);
		assertThat(response.completionDate()).isEqualTo(LocalDate.of(2026, 8, 29));
		assertThat(response.certificateNumber()).matches("SF-\\d{4}-[0-9A-F]{12}");

		ArgumentCaptor<Certificate> captor = ArgumentCaptor.forClass(Certificate.class);
		verify(certificateRepository).save(captor.capture());
		Certificate stored = captor.getValue();
		assertThat(stored.getUser()).isSameAs(user);
		assertThat(stored.getCourse()).isSameAs(course);
		assertThat(stored.getParticipantName()).isEqualTo("Learner Name");
		assertThat(stored.getCourseName()).isEqualTo("Introduction to Angular");
		assertThat(stored.getFinalScore()).isEqualTo(90);
		assertThat(stored.getCompletionDate()).isEqualTo(LocalDate.of(2026, 8, 29));
		assertThat(stored.getCreatedAt()).isNotNull();
	}

	@Test
	void finalScoreUsesBestScoreRatherThanLatestScore() {
		stubEligibleCourse();
		when(appUserRepository.getReferenceById(7L)).thenReturn(user);
		when(certificateRepository.save(any(Certificate.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CertificateResponse response = service.getOrCreateCertificate(1L, 7L, "Learner Name");

		assertThat(response.finalScore()).isEqualTo(90);
	}

	@Test
	void repeatedRequestReturnsExistingCertificateWithoutSecondInsert() {
		when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
		Certificate existing = new Certificate(user, course, "Original Learner", "Original Course",
				LocalDate.of(2026, 8, 29), 90,
				"SF-2026-ABCDEF123456", Instant.parse("2026-08-29T12:00:00Z"));
		when(certificateRepository.findByUserIdAndCourseId(7L, 1L)).thenReturn(Optional.of(existing));

		CertificateResponse first = service.getOrCreateCertificate(1L, 7L, "Current Learner Name");
		CertificateResponse second = service.getOrCreateCertificate(1L, 7L, "Current Learner Name");

		assertThat(first).isEqualTo(second);
		assertThat(first.certificateNumber()).isEqualTo("SF-2026-ABCDEF123456");
		assertThat(first.participantName()).isEqualTo("Original Learner");
		assertThat(first.courseName()).isEqualTo("Original Course");
		verifyNoInteractions(courseModuleRepository, moduleProgressRepository, appUserRepository);
		verify(certificateRepository, never()).save(any());
	}

	@Test
	void existingCertificateWinsWithoutRecheckingCurrentEligibility() {
		Course changedCourse = course(1L, "Renamed Current Course");
		when(courseRepository.findById(1L)).thenReturn(Optional.of(changedCourse));
		Certificate existing = new Certificate(user, course, "Issued Learner", "Issued Course",
				LocalDate.of(2026, 8, 29), 90, "SF-2026-ABCDEF123456",
				Instant.parse("2026-08-29T12:00:00Z"));
		when(certificateRepository.findByUserIdAndCourseId(7L, 1L)).thenReturn(Optional.of(existing));

		CertificateResponse response = service.getOrCreateCertificate(1L, 7L, "Different Current Name");

		assertThat(response).isEqualTo(new CertificateResponse(
				"SF-2026-ABCDEF123456", "Issued Learner", "Issued Course", LocalDate.of(2026, 8, 29), 90));
		verifyNoInteractions(courseModuleRepository, moduleProgressRepository, appUserRepository);
		verify(certificateRepository, never()).save(any());
	}

	@Test
	void userAndCourseArePartOfCertificateLookupIdentity() {
		when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
		Certificate existing = new Certificate(user, course, "Learner Name", "Introduction to Angular",
				LocalDate.of(2026, 8, 29), 90,
				"SF-2026-ABCDEF123456", Instant.now());
		when(certificateRepository.findByUserIdAndCourseId(7L, 1L)).thenReturn(Optional.of(existing));

		service.getOrCreateCertificate(1L, 7L, "Learner Name");

		verify(certificateRepository).findByUserIdAndCourseId(7L, 1L);
	}

	@Test
	void differentUsersAndCoursesReceiveSeparateCertificates() {
		AppUser secondUser = new AppUser(8L, "Second Learner", "second@example.com", "hash", Role.USER);
		Course secondCourse = course(2L, "Spring Boot");
		CourseModule secondCourseModule = module(20L, secondCourse, 1);
		stubCourse(List.of(firstModule));
		when(courseRepository.findById(2L)).thenReturn(Optional.of(secondCourse));
		when(courseModuleRepository.findByCourseIdOrderByPositionAsc(2L)).thenReturn(List.of(secondCourseModule));
		when(moduleProgressRepository.findByUserIdAndModuleCourseId(7L, 1L)).thenReturn(List.of(
				completed(user, firstModule, 100, 100, "2026-08-27T10:00:00Z")));
		when(moduleProgressRepository.findByUserIdAndModuleCourseId(8L, 1L)).thenReturn(List.of(
				completed(secondUser, firstModule, 80, 80, "2026-08-28T10:00:00Z")));
		when(moduleProgressRepository.findByUserIdAndModuleCourseId(7L, 2L)).thenReturn(List.of(
				completed(user, secondCourseModule, 90, 90, "2026-08-29T10:00:00Z")));
		when(appUserRepository.getReferenceById(7L)).thenReturn(user);
		when(appUserRepository.getReferenceById(8L)).thenReturn(secondUser);
		when(certificateRepository.save(any(Certificate.class))).thenAnswer(invocation -> invocation.getArgument(0));

		CertificateResponse firstUserFirstCourse = service.getOrCreateCertificate(1L, 7L, "Learner Name");
		CertificateResponse secondUserFirstCourse = service.getOrCreateCertificate(1L, 8L, "Second Learner");
		CertificateResponse firstUserSecondCourse = service.getOrCreateCertificate(2L, 7L, "Learner Name");

		assertThat(Set.of(firstUserFirstCourse.certificateNumber(), secondUserFirstCourse.certificateNumber(),
				firstUserSecondCourse.certificateNumber())).hasSize(3);
		verify(certificateRepository).findByUserIdAndCourseId(7L, 1L);
		verify(certificateRepository).findByUserIdAndCourseId(8L, 1L);
		verify(certificateRepository).findByUserIdAndCourseId(7L, 2L);
	}

	@Test
	void inconsistentCompletedProgressReturnsConflict() {
		stubCourse(List.of(firstModule));
		ModuleProgress progress = new ModuleProgress(user, firstModule);
		when(moduleProgressRepository.findByUserIdAndModuleCourseId(7L, 1L)).thenReturn(List.of(progress));

		assertConflict(() -> service.getOrCreateCertificate(1L, 7L, "Learner Name"));
		verify(certificateRepository, never()).save(any());
	}

	@Test
	void missingCourseReturnsNotFound() {
		when(courseRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.getOrCreateCertificate(99L, 7L, "Learner Name"))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
	}

	private void stubEligibleCourse() {
		stubCourse(List.of(firstModule, secondModule));
		when(moduleProgressRepository.findByUserIdAndModuleCourseId(7L, 1L)).thenReturn(List.of(
				completed(firstModule, 100, 50, "2026-08-27T10:00:00Z"),
				completed(secondModule, 80, 60, "2026-08-29T10:00:00Z")));
	}

	private void stubCourse(List<CourseModule> modules) {
		when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
		when(courseModuleRepository.findByCourseIdOrderByPositionAsc(1L)).thenReturn(modules);
	}

	private ModuleProgress completed(CourseModule module, int bestScore, int lastScore, String completedAt) {
		return completed(user, module, bestScore, lastScore, completedAt);
	}

	private ModuleProgress completed(AppUser owner, CourseModule module, int bestScore, int lastScore,
			String completedAt) {
		ModuleProgress progress = new ModuleProgress(owner, module);
		progress.recordAttempt(bestScore, true, Instant.parse(completedAt));
		if (lastScore != bestScore) {
			progress.recordAttempt(lastScore, false, Instant.parse(completedAt).plusSeconds(60));
		}
		return progress;
	}

	private Course course(Long id, String title) {
		return new Course(id, title, "Description", "Instructor", 60,
				CourseLevel.BEGINNER, CourseCategory.INFORMATION_TECHNOLOGY);
	}

	private CourseModule module(Long id, Course owner, int position) {
		return new CourseModule(id, owner, "Module " + position, null, position);
	}

	private void assertConflict(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
		assertThatThrownBy(action).isInstanceOfSatisfying(ResponseStatusException.class,
				exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
	}
}
