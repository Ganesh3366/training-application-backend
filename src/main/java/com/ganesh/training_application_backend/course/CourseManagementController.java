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

import com.ganesh.training_application_backend.course.dto.CourseCreateRequest;
import com.ganesh.training_application_backend.course.dto.CourseManagementResponse;
import com.ganesh.training_application_backend.course.dto.CourseUpdateRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/management/courses")
public class CourseManagementController {

	private final CourseManagementService courseManagementService;

	public CourseManagementController(CourseManagementService courseManagementService) {
		this.courseManagementService = courseManagementService;
	}

	@GetMapping
	public List<CourseManagementResponse> getCourses() {
		return courseManagementService.getCourses();
	}

	@GetMapping("/{courseId}")
	public CourseManagementResponse getCourse(@PathVariable Long courseId) {
		return courseManagementService.getCourse(courseId);
	}

	@PostMapping
	public ResponseEntity<CourseManagementResponse> createCourse(
			@Valid @RequestBody CourseCreateRequest request) {
		CourseManagementResponse response = courseManagementService.createCourse(request);
		return ResponseEntity.created(URI.create("/api/management/courses/" + response.id())).body(response);
	}

	@PutMapping("/{courseId}")
	public CourseManagementResponse updateCourse(@PathVariable Long courseId,
			@Valid @RequestBody CourseUpdateRequest request) {
		return courseManagementService.updateCourse(courseId, request);
	}

	@DeleteMapping("/{courseId}")
	public ResponseEntity<Void> deleteCourse(@PathVariable Long courseId) {
		courseManagementService.deleteCourse(courseId);
		return ResponseEntity.noContent().build();
	}
}
