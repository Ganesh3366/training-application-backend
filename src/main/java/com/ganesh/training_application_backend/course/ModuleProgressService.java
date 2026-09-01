package com.ganesh.training_application_backend.course;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ganesh.training_application_backend.course.dto.CourseProgressResponse;
import com.ganesh.training_application_backend.course.dto.CourseProgressStatus;
import com.ganesh.training_application_backend.course.dto.ModuleProgressResponse;

@Service
public class ModuleProgressService {

	private final CourseRepository courseRepository;
	private final CourseModuleRepository courseModuleRepository;
	private final ModuleProgressRepository moduleProgressRepository;

	public ModuleProgressService(CourseRepository courseRepository, CourseModuleRepository courseModuleRepository,
			ModuleProgressRepository moduleProgressRepository) {
		this.courseRepository = courseRepository;
		this.courseModuleRepository = courseModuleRepository;
		this.moduleProgressRepository = moduleProgressRepository;
	}

	@Transactional(readOnly = true)
	public CourseProgressResponse getCourseProgress(Long courseId, Long userId) {
		if (!courseRepository.existsById(courseId)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
		}
		var modules = courseModuleRepository.findByCourseIdOrderByPositionAsc(courseId);
		Map<Long, ModuleProgress> progressByModule = moduleProgressRepository
				.findByUserIdAndModuleCourseId(userId, courseId).stream()
				.collect(Collectors.toMap(progress -> progress.getModule().getId(), Function.identity()));
		var responses = modules.stream().map(module -> toResponse(module, progressByModule.get(module.getId()))).toList();
		int completed = (int) responses.stream().filter(ModuleProgressResponse::completed).count();
		int total = responses.size();
		int progressPercentage = total == 0 ? 0 : (int) Math.round(completed * 100.0 / total);
		CourseProgressStatus status = CourseProgressStatus.from(completed, total);
		return new CourseProgressResponse(courseId, total, completed, total - completed, progressPercentage,
				status == CourseProgressStatus.COMPLETED, status, responses);
	}

	private ModuleProgressResponse toResponse(CourseModule module, ModuleProgress progress) {
		if (progress == null) {
			return new ModuleProgressResponse(module.getId(), false, 0, null, null, null);
		}
		return new ModuleProgressResponse(module.getId(), progress.isCompleted(), progress.getAttemptsCount(),
				progress.getLastScore(), progress.getBestScore(), progress.getCompletedAt());
	}
}
