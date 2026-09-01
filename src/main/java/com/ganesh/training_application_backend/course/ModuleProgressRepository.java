package com.ganesh.training_application_backend.course;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

public interface ModuleProgressRepository extends JpaRepository<ModuleProgress, Long> {

	Optional<ModuleProgress> findByUserIdAndModuleId(Long userId, Long moduleId);

	@EntityGraph(attributePaths = "module")
	List<ModuleProgress> findByUserIdAndModuleCourseId(Long userId, Long courseId);

	boolean existsByModuleId(Long moduleId);

	@EntityGraph(attributePaths = {"user", "module", "module.course"})
	@Query("""
			select progress
			from ModuleProgress progress
			order by progress.user.id, progress.module.course.id, progress.module.position, progress.module.id
			""")
	List<ModuleProgress> findAllForReporting();
}
