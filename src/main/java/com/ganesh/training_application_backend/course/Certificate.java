package com.ganesh.training_application_backend.course;

import java.time.Instant;
import java.time.LocalDate;

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
@Table(name = "certificates", uniqueConstraints =
		@UniqueConstraint(name = "uk_certificate_user_course", columnNames = {"user_id", "course_id"}))
public class Certificate {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private AppUser user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "course_id", nullable = false)
	private Course course;

	@Column(nullable = false, updatable = false)
	private String participantName;

	@Column(nullable = false, updatable = false)
	private String courseName;

	@Column(nullable = false, updatable = false)
	private LocalDate completionDate;

	@Column(nullable = false, updatable = false)
	private int finalScore;

	@Column(nullable = false, unique = true, updatable = false)
	private String certificateNumber;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	protected Certificate() {
	}

	public Certificate(AppUser user, Course course, String participantName, String courseName,
			LocalDate completionDate, int finalScore, String certificateNumber, Instant createdAt) {
		this.user = user;
		this.course = course;
		this.participantName = participantName;
		this.courseName = courseName;
		this.completionDate = completionDate;
		this.finalScore = finalScore;
		this.certificateNumber = certificateNumber;
		this.createdAt = createdAt;
	}

	public Long getId() {
		return id;
	}

	public AppUser getUser() {
		return user;
	}

	public Course getCourse() {
		return course;
	}

	public String getParticipantName() {
		return participantName;
	}

	public String getCourseName() {
		return courseName;
	}

	public LocalDate getCompletionDate() {
		return completionDate;
	}

	public int getFinalScore() {
		return finalScore;
	}

	public String getCertificateNumber() {
		return certificateNumber;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
