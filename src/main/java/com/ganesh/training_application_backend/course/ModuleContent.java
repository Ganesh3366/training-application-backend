package com.ganesh.training_application_backend.course;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "module_contents")
public class ModuleContent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "module_id", nullable = false)
	private CourseModule module;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ModuleContentType type;

	@Column(nullable = false)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String textContent;

	private String videoUrl;

	@Column(nullable = false)
	private Integer position;

	protected ModuleContent() {
	}

	public ModuleContent(Long id, CourseModule module, ModuleContentType type, String title,
			String textContent, String videoUrl, Integer position) {
		this.id = id;
		this.module = module;
		this.type = type;
		this.title = title;
		this.textContent = textContent;
		this.videoUrl = videoUrl;
		this.position = position;
	}

	public void updateDetails(ModuleContentType type, String title, String textContent, String videoUrl) {
		this.type = type;
		this.title = title;
		this.textContent = textContent;
		this.videoUrl = videoUrl;
	}

	public Long getId() {
		return id;
	}

	public CourseModule getModule() {
		return module;
	}

	public ModuleContentType getType() {
		return type;
	}

	public String getTitle() {
		return title;
	}

	public String getTextContent() {
		return textContent;
	}

	public String getVideoUrl() {
		return videoUrl;
	}

	public Integer getPosition() {
		return position;
	}
}
