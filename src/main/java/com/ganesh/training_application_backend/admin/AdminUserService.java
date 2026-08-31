package com.ganesh.training_application_backend.admin;

import java.time.Instant;
import java.util.List;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ganesh.training_application_backend.admin.dto.CourseAssignmentRequest;
import com.ganesh.training_application_backend.admin.dto.CourseAssignmentResponse;
import com.ganesh.training_application_backend.auth.AppUser;
import com.ganesh.training_application_backend.auth.AppUserRepository;
import com.ganesh.training_application_backend.auth.dto.UserResponse;
import com.ganesh.training_application_backend.course.Course;
import com.ganesh.training_application_backend.course.CourseRepository;
import com.ganesh.training_application_backend.course.dto.CourseManagementResponse;

@Service
public class AdminUserService {

	private final AppUserRepository userRepository;
	private final CourseRepository courseRepository;
	private final CourseAssignmentRepository assignmentRepository;

	public AdminUserService(AppUserRepository userRepository, CourseRepository courseRepository,
			CourseAssignmentRepository assignmentRepository) {
		this.userRepository = userRepository;
		this.courseRepository = courseRepository;
		this.assignmentRepository = assignmentRepository;
	}

	@Transactional(readOnly = true)
	public List<UserResponse> getUsers() {
		return userRepository.findAllByOrderByIdAsc().stream().map(this::toUserResponse).toList();
	}

	@Transactional(readOnly = true)
	public UserResponse getUser(Long userId) {
		return toUserResponse(requireUser(userId));
	}

	@Transactional(readOnly = true)
	public List<CourseAssignmentResponse> getAssignments(Long userId) {
		requireUser(userId);
		return assignmentRepository.findAllByUserIdOrderByIdAsc(userId).stream()
				.map(this::toAssignmentResponse).toList();
	}

	@Transactional
	public CourseAssignmentResponse assignCourse(Long userId, CourseAssignmentRequest request) {
		AppUser user = requireUser(userId);
		Course course = courseRepository.findById(request.courseId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
		if (assignmentRepository.existsByUserIdAndCourseId(userId, request.courseId())) {
			throw duplicateAssignment();
		}
		try {
			CourseAssignment assignment = new CourseAssignment(user, course, Instant.now());
			return toAssignmentResponse(assignmentRepository.saveAndFlush(assignment));
		} catch (DataIntegrityViolationException exception) {
			if (isDuplicateAssignment(exception)) throw duplicateAssignment();
			throw exception;
		}
	}

	private AppUser requireUser(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
	}

	private boolean isDuplicateAssignment(Throwable exception) {
		for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
			if (cause instanceof ConstraintViolationException constraintViolation
					&& CourseAssignment.USER_COURSE_CONSTRAINT.equalsIgnoreCase(constraintViolation.getConstraintName())) {
				return true;
			}
			if (cause.getMessage() != null && cause.getMessage().toLowerCase()
					.contains(CourseAssignment.USER_COURSE_CONSTRAINT)) return true;
		}
		return false;
	}

	private ResponseStatusException duplicateAssignment() {
		return new ResponseStatusException(HttpStatus.CONFLICT, "Course is already assigned to this user");
	}

	private UserResponse toUserResponse(AppUser user) {
		return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
	}

	private CourseAssignmentResponse toAssignmentResponse(CourseAssignment assignment) {
		Course course = assignment.getCourse();
		CourseManagementResponse courseResponse = new CourseManagementResponse(course.getId(), course.getTitle(),
				course.getDescription(), course.getInstructor(), course.getDuration(), course.getLevel(), course.getCategory());
		return new CourseAssignmentResponse(assignment.getId(), courseResponse, assignment.getAssignedAt());
	}
}
