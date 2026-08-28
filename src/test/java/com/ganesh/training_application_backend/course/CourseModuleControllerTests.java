package com.ganesh.training_application_backend.course;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.ganesh.training_application_backend.config.SecurityConfig;
import com.ganesh.training_application_backend.course.dto.CourseModuleResponse;
import com.ganesh.training_application_backend.course.dto.CourseModuleSummaryResponse;
import com.ganesh.training_application_backend.course.dto.ModuleContentResponse;

@WebMvcTest(CourseModuleController.class)
@Import(SecurityConfig.class)
class CourseModuleControllerTests {

	@MockitoBean
	private CourseModuleService courseModuleService;

	private final MockMvc mockMvc;

	@Autowired
	CourseModuleControllerTests(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void getsCourseModulesWithoutAuthentication() throws Exception {
		when(courseModuleService.getModulesByCourseId(1L)).thenReturn(List.of(
				new CourseModuleSummaryResponse(10L, "Introduction", "Start here", 1)));

		mockMvc.perform(get("/api/courses/1/modules"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(10L))
				.andExpect(jsonPath("$[0].title").value("Introduction"))
				.andExpect(jsonPath("$[0].position").value(1));
	}

	@Test
	void getsModuleDetailWithTextAndVideoContentWithoutAuthentication() throws Exception {
		when(courseModuleService.getModuleById(1L, 10L)).thenReturn(moduleResponse());

		mockMvc.perform(get("/api/courses/1/modules/10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(10L))
				.andExpect(jsonPath("$.contents[0].type").value("TEXT"))
				.andExpect(jsonPath("$.contents[0].textContent").value("Plain text lesson"))
				.andExpect(jsonPath("$.contents[0].videoUrl").isEmpty())
				.andExpect(jsonPath("$.contents[1].type").value("VIDEO"))
				.andExpect(jsonPath("$.contents[1].textContent").isEmpty())
				.andExpect(jsonPath("$.contents[1].videoUrl")
						.value("https://www.youtube.com/watch?v=example"));
	}

	@Test
	void returnsNotFoundForUnknownOrWrongCourseModule() throws Exception {
		when(courseModuleService.getModuleById(2L, 10L))
				.thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Course module not found"));

		mockMvc.perform(get("/api/courses/2/modules/10"))
				.andExpect(status().isNotFound());
	}

	private CourseModuleResponse moduleResponse() {
		return new CourseModuleResponse(
				10L,
				"Introduction",
				"Start here",
				1,
				List.of(
						new ModuleContentResponse(
								100L, ModuleContentType.TEXT, "Read this", "Plain text lesson", null, 1),
						new ModuleContentResponse(
								101L, ModuleContentType.VIDEO, "Watch this", null,
								"https://www.youtube.com/watch?v=example", 2)));
	}
}
