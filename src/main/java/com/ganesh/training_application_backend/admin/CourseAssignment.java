package com.ganesh.training_application_backend.admin;

import java.time.Instant;

import com.ganesh.training_application_backend.auth.AppUser;
import com.ganesh.training_application_backend.course.Course;

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
@Table(name = "course_assignments", uniqueConstraints = @UniqueConstraint(
		name = CourseAssignment.USER_COURSE_CONSTRAINT, columnNames = {"user_id", "course_id"}))
public class CourseAssignment {

	public static final String USER_COURSE_CONSTRAINT = "uk_course_assignment_user_course";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, updatable = false)
	private AppUser user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "course_id", nullable = false, updatable = false)
	private Course course;

	@Column(nullable = false, updatable = false)
	private Instant assignedAt;

	protected CourseAssignment() {
	}

	public CourseAssignment(AppUser user, Course course, Instant assignedAt) {
		this.user = user;
		this.course = course;
		this.assignedAt = assignedAt;
	}

	public Long getId() { return id; }
	public AppUser getUser() { return user; }
	public Course getCourse() { return course; }
	public Instant getAssignedAt() { return assignedAt; }
}
