package com.ganesh.training_application_backend.course;

import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "courses")
public class Course {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String description;

	@Column(nullable = false)
	private String instructor;

	@Column(nullable = false)
	private Integer duration;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CourseLevel level;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CourseCategory category;

	protected Course() {
	}

	public Course(Long id, String title, String description, String instructor, Integer duration,
			CourseLevel level, CourseCategory category) {
		this.id = id;
		this.title = title;
		this.description = description;
		this.instructor = instructor;
		this.duration = duration;
		this.level = level;
		this.category = category;
	}

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public String getInstructor() {
		return instructor;
	}

	public Integer getDuration() {
		return duration;
	}

	public CourseLevel getLevel() {
		return level;
	}

	public CourseCategory getCategory() {
		return category;
	}
}
