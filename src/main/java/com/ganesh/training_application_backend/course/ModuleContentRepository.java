package com.ganesh.training_application_backend.course;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ModuleContentRepository extends JpaRepository<ModuleContent, Long> {

	List<ModuleContent> findByModuleIdOrderByPositionAsc(Long moduleId);
}
