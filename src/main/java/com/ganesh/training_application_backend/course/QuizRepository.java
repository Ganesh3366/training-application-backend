package com.ganesh.training_application_backend.course;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

	Optional<Quiz> findByModuleId(Long moduleId);

	Optional<Quiz> findByModuleIdAndModuleCourseId(Long moduleId, Long courseId);

	boolean existsByModuleId(Long moduleId);
}
