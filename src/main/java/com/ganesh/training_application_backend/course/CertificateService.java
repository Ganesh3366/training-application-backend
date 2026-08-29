package com.ganesh.training_application_backend.course;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ganesh.training_application_backend.auth.AppUserRepository;
import com.ganesh.training_application_backend.course.dto.CertificateResponse;

@Service
public class CertificateService {

	private final CourseRepository courseRepository;
	private final CourseModuleRepository courseModuleRepository;
	private final ModuleProgressRepository moduleProgressRepository;
	private final CertificateRepository certificateRepository;
	private final AppUserRepository appUserRepository;

	public CertificateService(CourseRepository courseRepository, CourseModuleRepository courseModuleRepository,
			ModuleProgressRepository moduleProgressRepository, CertificateRepository certificateRepository,
			AppUserRepository appUserRepository) {
		this.courseRepository = courseRepository;
		this.courseModuleRepository = courseModuleRepository;
		this.moduleProgressRepository = moduleProgressRepository;
		this.certificateRepository = certificateRepository;
		this.appUserRepository = appUserRepository;
	}

	@Transactional
	public CertificateResponse getOrCreateCertificate(Long courseId, Long userId, String participantName) {
		Course course = courseRepository.findById(courseId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
		var existingCertificate = certificateRepository.findByUserIdAndCourseId(userId, courseId);
		if (existingCertificate.isPresent()) {
			return toResponse(existingCertificate.get());
		}

		var modules = courseModuleRepository.findByCourseIdOrderByPositionAsc(courseId);
		if (modules.isEmpty()) {
			throw notCompleted();
		}

		Map<Long, ModuleProgress> progressByModule = moduleProgressRepository
				.findByUserIdAndModuleCourseId(userId, courseId).stream()
				.collect(Collectors.toMap(progress -> progress.getModule().getId(), Function.identity()));
		var completedProgress = modules.stream().map(module -> progressByModule.get(module.getId())).toList();
		if (completedProgress.stream().anyMatch(progress -> progress == null || !progress.isCompleted()
				|| progress.getBestScore() == null || progress.getCompletedAt() == null)) {
			throw notCompleted();
		}

		Certificate certificate = certificateRepository.save(createCertificate(
				course, userId, participantName, completedProgress));
		return toResponse(certificate);
	}

	private Certificate createCertificate(Course course, Long userId, String participantName,
			List<ModuleProgress> progress) {
		int finalScore = (int) Math.round(progress.stream().mapToInt(ModuleProgress::getBestScore).average()
				.orElseThrow());
		Instant latestCompletion = progress.stream().map(ModuleProgress::getCompletedAt).max(Instant::compareTo)
				.orElseThrow();
		LocalDate completionDate = latestCompletion.atZone(ZoneOffset.UTC).toLocalDate();
		Instant createdAt = Instant.now();
		String certificateNumber = "SF-" + createdAt.atZone(ZoneOffset.UTC).getYear() + "-"
				+ UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
		return new Certificate(appUserRepository.getReferenceById(userId), course, participantName,
				course.getTitle(), completionDate, finalScore, certificateNumber, createdAt);
	}

	private CertificateResponse toResponse(Certificate certificate) {
		return new CertificateResponse(certificate.getCertificateNumber(), certificate.getParticipantName(),
				certificate.getCourseName(),
				certificate.getCompletionDate(), certificate.getFinalScore());
	}

	private ResponseStatusException notCompleted() {
		return new ResponseStatusException(HttpStatus.CONFLICT, "Course is not completed");
	}
}
