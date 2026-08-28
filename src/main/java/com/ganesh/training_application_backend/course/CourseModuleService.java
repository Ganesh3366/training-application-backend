package com.ganesh.training_application_backend.course;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.ganesh.training_application_backend.course.dto.CourseModuleResponse;
import com.ganesh.training_application_backend.course.dto.CourseModuleSummaryResponse;
import com.ganesh.training_application_backend.course.dto.ModuleContentResponse;

@Service
public class CourseModuleService {

	private final CourseRepository courseRepository;
	private final CourseModuleRepository courseModuleRepository;
	private final ModuleContentRepository moduleContentRepository;

	public CourseModuleService(CourseRepository courseRepository, CourseModuleRepository courseModuleRepository,
			ModuleContentRepository moduleContentRepository) {
		this.courseRepository = courseRepository;
		this.courseModuleRepository = courseModuleRepository;
		this.moduleContentRepository = moduleContentRepository;
	}

	public List<CourseModuleSummaryResponse> getModulesByCourseId(Long courseId) {
		if (!courseRepository.existsById(courseId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
		}

		return courseModuleRepository.findByCourseIdOrderByPositionAsc(courseId).stream()
				.map(this::toSummaryResponse)
				.toList();
	}

	public CourseModuleResponse getModuleById(Long courseId, Long moduleId) {
		CourseModule module = courseModuleRepository.findByIdAndCourseId(moduleId, courseId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course module not found"));

		List<ModuleContentResponse> contents = moduleContentRepository
				.findByModuleIdOrderByPositionAsc(moduleId).stream()
				.map(this::toContentResponse)
				.toList();

		return new CourseModuleResponse(
				module.getId(),
				module.getTitle(),
				module.getDescription(),
				module.getPosition(),
				contents);
	}

	private CourseModuleSummaryResponse toSummaryResponse(CourseModule module) {
		return new CourseModuleSummaryResponse(
				module.getId(),
				module.getTitle(),
				module.getDescription(),
				module.getPosition());
	}

	private ModuleContentResponse toContentResponse(ModuleContent content) {
		return new ModuleContentResponse(
				content.getId(),
				content.getType(),
				content.getTitle(),
				content.getTextContent(),
				content.getVideoUrl(),
				content.getPosition());
	}
}
