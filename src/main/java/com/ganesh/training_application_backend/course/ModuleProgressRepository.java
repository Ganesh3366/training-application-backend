package com.ganesh.training_application_backend.course;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

public interface ModuleProgressRepository extends JpaRepository<ModuleProgress, Long> {

	Optional<ModuleProgress> findByUserIdAndModuleId(Long userId, Long moduleId);

	@EntityGraph(attributePaths = "module")
	List<ModuleProgress> findByUserIdAndModuleCourseId(Long userId, Long courseId);

	boolean existsByModuleId(Long moduleId);
}
