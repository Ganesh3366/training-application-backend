package com.ganesh.training_application_backend.course;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.ganesh.training_application_backend.course.dto.CourseResponse;

@Service
public class CourseService {

	private final CourseRepository courseRepository;

	public CourseService(CourseRepository courseRepository) {
		this.courseRepository = courseRepository;
	}

	public List<CourseResponse> getAllCourses() {
		return courseRepository.findAll().stream()
				.map(this::toResponse)
				.toList();
	}

	public CourseResponse getCourseById(Long id) {
		return courseRepository.findById(id)
				.map(this::toResponse)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
	}

	private CourseResponse toResponse(Course course) {
		return new CourseResponse(
				course.getId(),
				course.getTitle(),
				course.getDescription(),
				course.getInstructor(),
				course.getDuration(),
				course.getLevel().getDisplayValue(),
				course.getCategory().getDisplayValue());
	}
}
