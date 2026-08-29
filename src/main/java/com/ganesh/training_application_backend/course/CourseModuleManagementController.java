package com.ganesh.training_application_backend.course;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ganesh.training_application_backend.course.dto.CourseModuleCreateRequest;
import com.ganesh.training_application_backend.course.dto.CourseModuleManagementResponse;
import com.ganesh.training_application_backend.course.dto.CourseModuleUpdateRequest;
import com.ganesh.training_application_backend.course.dto.ModuleContentCreateRequest;
import com.ganesh.training_application_backend.course.dto.ModuleContentManagementResponse;
import com.ganesh.training_application_backend.course.dto.ModuleContentUpdateRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/management/courses/{courseId}/modules")
public class CourseModuleManagementController {

	private final CourseModuleManagementService service;

	public CourseModuleManagementController(CourseModuleManagementService service) {
		this.service = service;
	}

	@GetMapping
	public List<CourseModuleManagementResponse> getModules(@PathVariable Long courseId) {
		return service.getModules(courseId);
	}

	@PostMapping
	public ResponseEntity<CourseModuleManagementResponse> createModule(@PathVariable Long courseId,
			@Valid @RequestBody CourseModuleCreateRequest request) {
		CourseModuleManagementResponse response = service.createModule(courseId, request);
		return ResponseEntity.created(URI.create("/api/management/courses/" + courseId + "/modules/"
				+ response.id())).body(response);
	}

	@PutMapping("/{moduleId}")
	public CourseModuleManagementResponse updateModule(@PathVariable Long courseId, @PathVariable Long moduleId,
			@Valid @RequestBody CourseModuleUpdateRequest request) {
		return service.updateModule(courseId, moduleId, request);
	}

	@DeleteMapping("/{moduleId}")
	public ResponseEntity<Void> deleteModule(@PathVariable Long courseId, @PathVariable Long moduleId) {
		service.deleteModule(courseId, moduleId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/{moduleId}/contents")
	public List<ModuleContentManagementResponse> getContents(@PathVariable Long courseId,
			@PathVariable Long moduleId) {
		return service.getContents(courseId, moduleId);
	}

	@PostMapping("/{moduleId}/contents")
	public ResponseEntity<ModuleContentManagementResponse> createContent(@PathVariable Long courseId,
			@PathVariable Long moduleId, @Valid @RequestBody ModuleContentCreateRequest request) {
		ModuleContentManagementResponse response = service.createContent(courseId, moduleId, request);
		return ResponseEntity.created(URI.create("/api/management/courses/" + courseId + "/modules/"
				+ moduleId + "/contents/" + response.id())).body(response);
	}

	@PutMapping("/{moduleId}/contents/{contentId}")
	public ModuleContentManagementResponse updateContent(@PathVariable Long courseId, @PathVariable Long moduleId,
			@PathVariable Long contentId, @Valid @RequestBody ModuleContentUpdateRequest request) {
		return service.updateContent(courseId, moduleId, contentId, request);
	}

	@DeleteMapping("/{moduleId}/contents/{contentId}")
	public ResponseEntity<Void> deleteContent(@PathVariable Long courseId, @PathVariable Long moduleId,
			@PathVariable Long contentId) {
		service.deleteContent(courseId, moduleId, contentId);
		return ResponseEntity.noContent().build();
	}
}
