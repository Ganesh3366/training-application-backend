package com.ganesh.training_application_backend.course;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ganesh.training_application_backend.course.dto.CourseModuleCreateRequest;
import com.ganesh.training_application_backend.course.dto.CourseModuleManagementResponse;
import com.ganesh.training_application_backend.course.dto.CourseModuleUpdateRequest;
import com.ganesh.training_application_backend.course.dto.ModuleContentCreateRequest;
import com.ganesh.training_application_backend.course.dto.ModuleContentManagementResponse;
import com.ganesh.training_application_backend.course.dto.ModuleContentUpdateRequest;

@Service
public class CourseModuleManagementService {

	private final CourseRepository courseRepository;
	private final CourseModuleRepository courseModuleRepository;
	private final ModuleContentRepository moduleContentRepository;
	private final QuizRepository quizRepository;
	private final ModuleProgressRepository moduleProgressRepository;

	public CourseModuleManagementService(CourseRepository courseRepository,
			CourseModuleRepository courseModuleRepository, ModuleContentRepository moduleContentRepository,
			QuizRepository quizRepository, ModuleProgressRepository moduleProgressRepository) {
		this.courseRepository = courseRepository;
		this.courseModuleRepository = courseModuleRepository;
		this.moduleContentRepository = moduleContentRepository;
		this.quizRepository = quizRepository;
		this.moduleProgressRepository = moduleProgressRepository;
	}

	@Transactional(readOnly = true)
	public List<CourseModuleManagementResponse> getModules(Long courseId) {
		requireCourse(courseId);
		return courseModuleRepository.findByCourseIdOrderByPositionAsc(courseId).stream()
				.map(this::toModuleResponse).toList();
	}

	@Transactional
	public CourseModuleManagementResponse createModule(Long courseId, CourseModuleCreateRequest request) {
		Course course = requireCourse(courseId);
		int position = courseModuleRepository.findTopByCourseIdOrderByPositionDesc(courseId)
				.map(module -> module.getPosition() + 1).orElse(1);
		CourseModule module = new CourseModule(null, course, normalizeRequired(request.title()),
				normalizeOptional(request.description()), position);
		return toModuleResponse(courseModuleRepository.save(module));
	}

	@Transactional
	public CourseModuleManagementResponse updateModule(Long courseId, Long moduleId,
			CourseModuleUpdateRequest request) {
		CourseModule module = requireModule(courseId, moduleId);
		module.updateDetails(normalizeRequired(request.title()), normalizeOptional(request.description()));
		return toModuleResponse(module);
	}

	@Transactional
	public void deleteModule(Long courseId, Long moduleId) {
		CourseModule module = requireModule(courseId, moduleId);
		if (quizRepository.existsByModuleId(moduleId) || moduleProgressRepository.existsByModuleId(moduleId)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Course module has protected dependencies");
		}
		moduleContentRepository.deleteAllByModuleId(moduleId);
		courseModuleRepository.delete(module);
	}

	@Transactional(readOnly = true)
	public List<ModuleContentManagementResponse> getContents(Long courseId, Long moduleId) {
		requireModule(courseId, moduleId);
		return moduleContentRepository.findByModuleIdOrderByPositionAsc(moduleId).stream()
				.map(this::toContentResponse).toList();
	}

	@Transactional
	public ModuleContentManagementResponse createContent(Long courseId, Long moduleId,
			ModuleContentCreateRequest request) {
		CourseModule module = requireModule(courseId, moduleId);
		ContentValues values = validateContent(request.type(), request.title(), request.textContent(),
				request.videoUrl());
		int position = moduleContentRepository.findTopByModuleIdOrderByPositionDesc(moduleId)
				.map(content -> content.getPosition() + 1).orElse(1);
		ModuleContent content = new ModuleContent(null, module, values.type(), values.title(),
				values.textContent(), values.videoUrl(), position);
		return toContentResponse(moduleContentRepository.save(content));
	}

	@Transactional
	public ModuleContentManagementResponse updateContent(Long courseId, Long moduleId, Long contentId,
			ModuleContentUpdateRequest request) {
		requireModule(courseId, moduleId);
		ModuleContent content = requireContent(moduleId, contentId);
		ContentValues values = validateContent(request.type(), request.title(), request.textContent(),
				request.videoUrl());
		content.updateDetails(values.type(), values.title(), values.textContent(), values.videoUrl());
		return toContentResponse(content);
	}

	@Transactional
	public void deleteContent(Long courseId, Long moduleId, Long contentId) {
		requireModule(courseId, moduleId);
		moduleContentRepository.delete(requireContent(moduleId, contentId));
	}

	private Course requireCourse(Long courseId) {
		return courseRepository.findById(courseId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
	}

	private CourseModule requireModule(Long courseId, Long moduleId) {
		return courseModuleRepository.findByIdAndCourseId(moduleId, courseId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course module not found"));
	}

	private ModuleContent requireContent(Long moduleId, Long contentId) {
		return moduleContentRepository.findByIdAndModuleId(contentId, moduleId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Module content not found"));
	}

	private ContentValues validateContent(ModuleContentType type, String title, String textContent,
			String videoUrl) {
		String normalizedText = normalizeOptional(textContent);
		String normalizedVideoUrl = normalizeOptional(videoUrl);
		if (type == ModuleContentType.TEXT) {
			if (normalizedText == null || normalizedVideoUrl != null) {
				throw badRequest("TEXT content requires textContent and must not include videoUrl");
			}
			return new ContentValues(type, normalizeRequired(title), normalizedText, null);
		}
		if (type == ModuleContentType.VIDEO) {
			if (normalizedVideoUrl == null || normalizedText != null) {
				throw badRequest("VIDEO content requires videoUrl and must not include textContent");
			}
			return new ContentValues(type, normalizeRequired(title), null, normalizedVideoUrl);
		}
		throw badRequest("Unsupported module content type");
	}

	private CourseModuleManagementResponse toModuleResponse(CourseModule module) {
		return new CourseModuleManagementResponse(module.getId(), module.getTitle(), module.getDescription(),
				module.getPosition());
	}

	private ModuleContentManagementResponse toContentResponse(ModuleContent content) {
		return new ModuleContentManagementResponse(content.getId(), content.getType(), content.getTitle(),
				content.getTextContent(), content.getVideoUrl(), content.getPosition());
	}

	private String normalizeRequired(String value) {
		return value.trim();
	}

	private String normalizeOptional(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private ResponseStatusException badRequest(String reason) {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
	}

	private record ContentValues(ModuleContentType type, String title, String textContent, String videoUrl) {
	}
}
