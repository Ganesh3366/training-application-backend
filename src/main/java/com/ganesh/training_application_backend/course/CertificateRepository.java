package com.ganesh.training_application_backend.course;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {

	Optional<Certificate> findByUserIdAndCourseId(Long userId, Long courseId);
}
