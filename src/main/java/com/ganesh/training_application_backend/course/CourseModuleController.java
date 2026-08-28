package com.ganesh.training_application_backend.course;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ganesh.training_application_backend.course.dto.CourseModuleResponse;
import com.ganesh.training_application_backend.course.dto.CourseModuleSummaryResponse;

@RestController
@RequestMapping("/api/courses/{courseId}/modules")
public class CourseModuleController {

	private final CourseModuleService courseModuleService;

	public CourseModuleController(CourseModuleService courseModuleService) {
		this.courseModuleService = courseModuleService;
	}

	@GetMapping
	public List<CourseModuleSummaryResponse> getModulesByCourseId(@PathVariable Long courseId) {
		return courseModuleService.getModulesByCourseId(courseId);
	}

	@GetMapping("/{moduleId}")
	public CourseModuleResponse getModuleById(@PathVariable Long courseId, @PathVariable Long moduleId) {
		return courseModuleService.getModuleById(courseId, moduleId);
	}
}
