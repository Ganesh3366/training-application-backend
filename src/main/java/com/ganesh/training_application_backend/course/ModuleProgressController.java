package com.ganesh.training_application_backend.course;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ganesh.training_application_backend.auth.AppUserPrincipal;
import com.ganesh.training_application_backend.course.dto.CourseProgressResponse;

@RestController
@RequestMapping("/api/courses/{courseId}/progress")
public class ModuleProgressController {

	private final ModuleProgressService moduleProgressService;

	public ModuleProgressController(ModuleProgressService moduleProgressService) {
		this.moduleProgressService = moduleProgressService;
	}

	@GetMapping
	public CourseProgressResponse getCourseProgress(@PathVariable Long courseId,
			@AuthenticationPrincipal AppUserPrincipal principal) {
		return moduleProgressService.getCourseProgress(courseId, principal.getId());
	}
}
