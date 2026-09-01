package com.ganesh.training_application_backend.course;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {

	Optional<Certificate> findByUserIdAndCourseId(Long userId, Long courseId);

	boolean existsByCourseId(Long courseId);

	@EntityGraph(attributePaths = {"user", "course"})
	List<Certificate> findByUserIdInAndCourseIdIn(Collection<Long> userIds, Collection<Long> courseIds);
}
