package com.ganesh.training_application_backend.course;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.ganesh.training_application_backend.auth.AppUser;
import com.ganesh.training_application_backend.auth.Role;

class ModuleProgressTests {

	@Test
	void retriesPreserveBestScoreCompletionAndFirstCompletionTime() {
		ModuleProgress progress = progress(1L, 10L);
		Instant failedAt = Instant.parse("2026-01-01T10:00:00Z");
		Instant passedAt = Instant.parse("2026-01-02T10:00:00Z");
		Instant laterAt = Instant.parse("2026-01-03T10:00:00Z");

		progress.recordAttempt(40, false, failedAt);
		assertThat(progress.getAttemptsCount()).isEqualTo(1);
		assertThat(progress.getLastScore()).isEqualTo(40);
		assertThat(progress.getBestScore()).isEqualTo(40);
		assertThat(progress.isCompleted()).isFalse();
		assertThat(progress.getCompletedAt()).isNull();

		progress.recordAttempt(80, true, passedAt);
		progress.recordAttempt(60, false, laterAt);

		assertThat(progress.getAttemptsCount()).isEqualTo(3);
		assertThat(progress.getLastScore()).isEqualTo(60);
		assertThat(progress.getBestScore()).isEqualTo(80);
		assertThat(progress.isCompleted()).isTrue();
		assertThat(progress.getCompletedAt()).isEqualTo(passedAt);
	}

	@Test
	void usersAndModulesHaveIndependentProgressObjects() {
		ModuleProgress firstUserFirstModule = progress(1L, 10L);
		ModuleProgress secondUserFirstModule = progress(2L, 10L);
		ModuleProgress firstUserSecondModule = progress(1L, 11L);

		firstUserFirstModule.recordAttempt(100, true, Instant.now());

		assertThat(firstUserFirstModule.isCompleted()).isTrue();
		assertThat(secondUserFirstModule.getAttemptsCount()).isZero();
		assertThat(firstUserSecondModule.getAttemptsCount()).isZero();
	}

	private ModuleProgress progress(Long userId, Long moduleId) {
		AppUser user = new AppUser(userId, "Learner", "learner" + userId + "@example.com", "hash", Role.USER);
		Course course = new Course(1L, "Course", "Description", "Instructor", 60,
				CourseLevel.BEGINNER, CourseCategory.INFORMATION_TECHNOLOGY);
		return new ModuleProgress(user, new CourseModule(moduleId, course, "Module", "Description", 1));
	}
}
