package com.ganesh.training_application_backend.course;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseModuleRepository extends JpaRepository<CourseModule, Long> {

	List<CourseModule> findByCourseIdOrderByPositionAsc(Long courseId);

	Optional<CourseModule> findByIdAndCourseId(Long id, Long courseId);
}
