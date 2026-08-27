package com.ganesh.training_application_backend.course;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ganesh.training_application_backend.course.dto.CourseResponse;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

	private final CourseService courseService;

	public CourseController(CourseService courseService) {
		this.courseService = courseService;
	}

	@GetMapping
	public List<CourseResponse> getAllCourses() {
		return courseService.getAllCourses();
	}

	@GetMapping("/{id}")
	public CourseResponse getCourseById(@PathVariable Long id) {
		return courseService.getCourseById(id);
	}
}
