package com.ganesh.training_application_backend.course;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ganesh.training_application_backend.course.dto.CourseCreateRequest;
import com.ganesh.training_application_backend.course.dto.CourseManagementResponse;
import com.ganesh.training_application_backend.course.dto.CourseUpdateRequest;

@Service
public class CourseManagementService {

	private final CourseRepository courseRepository;
	private final CourseModuleRepository courseModuleRepository;
	private final CertificateRepository certificateRepository;

	public CourseManagementService(CourseRepository courseRepository,
			CourseModuleRepository courseModuleRepository, CertificateRepository certificateRepository) {
		this.courseRepository = courseRepository;
		this.courseModuleRepository = courseModuleRepository;
		this.certificateRepository = certificateRepository;
	}

	@Transactional(readOnly = true)
	public List<CourseManagementResponse> getCourses() {
		return courseRepository.findAllByOrderByIdAsc().stream().map(this::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public CourseManagementResponse getCourse(Long courseId) {
		return toResponse(findCourse(courseId));
	}

	@Transactional
	public CourseManagementResponse createCourse(CourseCreateRequest request) {
		Course course = new Course(null, normalize(request.title()), normalize(request.description()),
				normalize(request.instructor()), request.duration(), request.level(), request.category());
		return toResponse(courseRepository.save(course));
	}

	@Transactional
	public CourseManagementResponse updateCourse(Long courseId, CourseUpdateRequest request) {
		Course course = findCourse(courseId);
		course.updateDetails(normalize(request.title()), normalize(request.description()),
				normalize(request.instructor()), request.duration(), request.level(), request.category());
		return toResponse(course);
	}

	@Transactional
	public void deleteCourse(Long courseId) {
		Course course = findCourse(courseId);
		if (courseModuleRepository.existsByCourseId(courseId) || certificateRepository.existsByCourseId(courseId)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Course has dependent records");
		}
		courseRepository.delete(course);
	}

	private Course findCourse(Long courseId) {
		return courseRepository.findById(courseId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
	}

	private CourseManagementResponse toResponse(Course course) {
		return new CourseManagementResponse(course.getId(), course.getTitle(), course.getDescription(),
				course.getInstructor(), course.getDuration(), course.getLevel(), course.getCategory());
	}

	private String normalize(String value) {
		return value.trim();
	}
}
