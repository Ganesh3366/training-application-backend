package com.ganesh.training_application_backend.course;

import java.time.Instant;

import com.ganesh.training_application_backend.auth.AppUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "module_progress", uniqueConstraints =
		@UniqueConstraint(name = "uk_module_progress_user_module", columnNames = {"user_id", "module_id"}))
public class ModuleProgress {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private AppUser user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "module_id", nullable = false)
	private CourseModule module;

	@Column(nullable = false)
	private int attemptsCount;

	private Integer lastScore;

	private Integer bestScore;

	@Column(nullable = false)
	private boolean completed;

	private Instant completedAt;

	protected ModuleProgress() {
	}

	public ModuleProgress(AppUser user, CourseModule module) {
		this.user = user;
		this.module = module;
	}

	void recordAttempt(int score, boolean passed, Instant attemptedAt) {
		attemptsCount++;
		lastScore = score;
		bestScore = bestScore == null ? score : Math.max(bestScore, score);
		if (passed && !completed) {
			completed = true;
			completedAt = attemptedAt;
		}
	}

	public Long getId() { return id; }
	public AppUser getUser() { return user; }
	public CourseModule getModule() { return module; }
	public int getAttemptsCount() { return attemptsCount; }
	public Integer getLastScore() { return lastScore; }
	public Integer getBestScore() { return bestScore; }
	public boolean isCompleted() { return completed; }
	public Instant getCompletedAt() { return completedAt; }
}
