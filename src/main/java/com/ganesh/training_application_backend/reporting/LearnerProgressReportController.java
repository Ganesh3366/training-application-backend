package com.ganesh.training_application_backend.reporting;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ganesh.training_application_backend.reporting.dto.LearnerCourseReportResponse;

@RestController
@RequestMapping("/api/management/reports")
public class LearnerProgressReportController {

	private final LearnerProgressReportService reportService;

	public LearnerProgressReportController(LearnerProgressReportService reportService) {
		this.reportService = reportService;
	}

	@GetMapping("/learner-courses")
	public List<LearnerCourseReportResponse> getLearnerCourseReports() {
		return reportService.getReports();
	}
}
