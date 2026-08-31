package com.ganesh.training_application_backend.admin;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseAssignmentRepository extends JpaRepository<CourseAssignment, Long> {

	boolean existsByUserIdAndCourseId(Long userId, Long courseId);

	boolean existsByCourseId(Long courseId);

	@EntityGraph(attributePaths = "course")
	List<CourseAssignment> findAllByUserIdOrderByIdAsc(Long userId);
}
