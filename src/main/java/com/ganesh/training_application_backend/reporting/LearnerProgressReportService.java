package com.ganesh.training_application_backend.reporting;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ganesh.training_application_backend.admin.CourseAssignment;
import com.ganesh.training_application_backend.admin.CourseAssignmentRepository;
import com.ganesh.training_application_backend.course.Certificate;
import com.ganesh.training_application_backend.course.CertificateRepository;
import com.ganesh.training_application_backend.course.CourseModule;
import com.ganesh.training_application_backend.course.CourseModuleRepository;
import com.ganesh.training_application_backend.course.ModuleProgress;
import com.ganesh.training_application_backend.course.ModuleProgressRepository;
import com.ganesh.training_application_backend.course.dto.CourseProgressStatus;
import com.ganesh.training_application_backend.reporting.dto.LearnerCourseReportResponse;
import com.ganesh.training_application_backend.reporting.dto.LearnerModuleReportResponse;

@Service
public class LearnerProgressReportService {

	private final CourseAssignmentRepository assignmentRepository;
	private final CourseModuleRepository moduleRepository;
	private final ModuleProgressRepository progressRepository;
	private final CertificateRepository certificateRepository;

	public LearnerProgressReportService(CourseAssignmentRepository assignmentRepository,
			CourseModuleRepository moduleRepository, ModuleProgressRepository progressRepository,
			CertificateRepository certificateRepository) {
		this.assignmentRepository = assignmentRepository;
		this.moduleRepository = moduleRepository;
		this.progressRepository = progressRepository;
		this.certificateRepository = certificateRepository;
	}

	@Transactional(readOnly = true)
	public List<LearnerCourseReportResponse> getReports() {
		List<CourseAssignment> assignments = assignmentRepository.findAllByOrderByIdAsc();
		if (assignments.isEmpty()) {
			return List.of();
		}

		Set<Long> learnerIds = assignments.stream().map(assignment -> assignment.getUser().getId())
				.collect(Collectors.toSet());
		Set<Long> courseIds = assignments.stream().map(assignment -> assignment.getCourse().getId())
				.collect(Collectors.toSet());
		Map<Long, List<CourseModule>> modulesByCourse = moduleRepository
				.findByCourseIdInOrderByCourseIdAscPositionAsc(courseIds).stream()
				.collect(Collectors.groupingBy(module -> module.getCourse().getId()));
		Map<LearnerCourseKey, Map<Long, ModuleProgress>> progressByAssignment = progressRepository
				.findByUserIdInAndModuleCourseIdIn(learnerIds, courseIds).stream()
				.collect(Collectors.groupingBy(this::keyFor,
						Collectors.toMap(progress -> progress.getModule().getId(), Function.identity())));
		Map<LearnerCourseKey, Certificate> certificatesByAssignment = certificateRepository
				.findByUserIdInAndCourseIdIn(learnerIds, courseIds).stream()
				.collect(Collectors.toMap(this::keyFor, Function.identity()));

		return assignments.stream().map(assignment -> toResponse(assignment,
				modulesByCourse.getOrDefault(assignment.getCourse().getId(), List.of()),
				progressByAssignment.getOrDefault(keyFor(assignment), Map.of()),
				certificatesByAssignment.get(keyFor(assignment)))).toList();
	}

	private LearnerCourseReportResponse toResponse(CourseAssignment assignment, List<CourseModule> modules,
			Map<Long, ModuleProgress> progressByModule, Certificate certificate) {
		List<LearnerModuleReportResponse> moduleResponses = modules.stream()
				.map(module -> toModuleResponse(module, progressByModule.get(module.getId()))).toList();
		int total = moduleResponses.size();
		int completed = (int) moduleResponses.stream().filter(LearnerModuleReportResponse::completed).count();
		boolean hasLearnerActivity = moduleResponses.stream().anyMatch(module -> module.attemptCount() > 0);
		CourseProgressStatus status = CourseProgressStatus.from(completed, total, hasLearnerActivity);
		int percentage = total == 0 ? 0 : (int) Math.round(completed * 100.0 / total);
		LocalDate completionDate = completionDate(status, certificate, moduleResponses);

		return new LearnerCourseReportResponse(assignment.getUser().getId(), assignment.getUser().getName(),
				assignment.getUser().getEmail(), assignment.getCourse().getId(), assignment.getCourse().getTitle(),
				completed, total, total - completed, percentage, status, completionDate,
				certificate == null ? null : certificate.getCertificateNumber(), moduleResponses);
	}

	private LearnerModuleReportResponse toModuleResponse(CourseModule module, ModuleProgress progress) {
		if (progress == null) {
			return new LearnerModuleReportResponse(module.getId(), module.getTitle(), false, null, null, 0, null);
		}
		return new LearnerModuleReportResponse(module.getId(), module.getTitle(), progress.isCompleted(),
				progress.getLastScore(), progress.getBestScore(), progress.getAttemptsCount(), progress.getCompletedAt());
	}

	private LocalDate completionDate(CourseProgressStatus status, Certificate certificate,
			List<LearnerModuleReportResponse> modules) {
		if (status != CourseProgressStatus.COMPLETED) {
			return null;
		}
		if (certificate != null) {
			return certificate.getCompletionDate();
		}
		return modules.stream().map(LearnerModuleReportResponse::completedAt).filter(java.util.Objects::nonNull)
				.max(java.time.Instant::compareTo).map(completedAt -> completedAt.atZone(ZoneOffset.UTC).toLocalDate())
				.orElse(null);
	}

	private LearnerCourseKey keyFor(CourseAssignment assignment) {
		return new LearnerCourseKey(assignment.getUser().getId(), assignment.getCourse().getId());
	}

	private LearnerCourseKey keyFor(ModuleProgress progress) {
		return new LearnerCourseKey(progress.getUser().getId(), progress.getModule().getCourse().getId());
	}

	private LearnerCourseKey keyFor(Certificate certificate) {
		return new LearnerCourseKey(certificate.getUser().getId(), certificate.getCourse().getId());
	}

	private record LearnerCourseKey(Long learnerId, Long courseId) {
	}
}
