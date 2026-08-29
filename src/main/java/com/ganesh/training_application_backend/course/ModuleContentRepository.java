package com.ganesh.training_application_backend.course;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ModuleContentRepository extends JpaRepository<ModuleContent, Long> {

	List<ModuleContent> findByModuleIdOrderByPositionAsc(Long moduleId);

	Optional<ModuleContent> findByIdAndModuleId(Long id, Long moduleId);

	Optional<ModuleContent> findTopByModuleIdOrderByPositionDesc(Long moduleId);

	void deleteAllByModuleId(Long moduleId);
}
