package com.ganesh.training_application_backend.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "quizzes")
public class Quiz {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "module_id", nullable = false, unique = true)
	private CourseModule module;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private Integer passingScore;

	protected Quiz() {
	}

	public Quiz(Long id, CourseModule module, String title, Integer passingScore) {
		if (passingScore == null || passingScore < 0 || passingScore > 100) {
			throw new IllegalArgumentException("Passing score must be between 0 and 100");
		}
		this.id = id;
		this.module = module;
		this.title = title;
		this.passingScore = passingScore;
	}

	public Long getId() {
		return id;
	}

	public CourseModule getModule() {
		return module;
	}

	public String getTitle() {
		return title;
	}

	public Integer getPassingScore() {
		return passingScore;
	}

	public void updateDetails(String title, Integer passingScore) {
		if (passingScore == null || passingScore < 0 || passingScore > 100) {
			throw new IllegalArgumentException("Passing score must be between 0 and 100");
		}
		this.title = title;
		this.passingScore = passingScore;
	}
}
