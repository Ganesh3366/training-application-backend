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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import com.ganesh.training_application_backend.config.SecurityConfig;
import com.ganesh.training_application_backend.course.dto.CourseResponse;

@WebMvcTest(CourseController.class)
@Import(SecurityConfig.class)
class CourseControllerTests {

	@MockitoBean
	private CourseService courseService;

	private final MockMvc mockMvc;

	@Autowired
	CourseControllerTests(MockMvc mockMvc) {
		this.mockMvc = mockMvc;
	}

	@Test
	void getsAllCourses() throws Exception {
		when(courseService.getAllCourses()).thenReturn(List.of(response(1L)));

		mockMvc.perform(get("/api/courses"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1L))
				.andExpect(jsonPath("$[0].level").value("Beginner"))
				.andExpect(jsonPath("$[0].category").value("Information Technology (IT)"));
	}

	@Test
	void getsCourseById() throws Exception {
		when(courseService.getCourseById(1L)).thenReturn(response(1L));

		mockMvc.perform(get("/api/courses/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Spring Boot Foundations"));
	}

	@Test
	void returnsNotFoundForUnknownCourse() throws Exception {
		when(courseService.getCourseById(99L))
				.thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

		mockMvc.perform(get("/api/courses/99"))
				.andExpect(status().isNotFound());
	}

	private CourseResponse response(Long id) {
		return new CourseResponse(
				id,
				"Spring Boot Foundations",
				"Build maintainable Spring applications",
				"Ganesh",
				120,
				"Beginner",
				"Information Technology (IT)");
	}
}
