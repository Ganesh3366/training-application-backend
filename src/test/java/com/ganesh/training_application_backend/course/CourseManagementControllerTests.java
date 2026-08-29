package com.ganesh.training_application_backend.course;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.ganesh.training_application_backend.auth.AppUser;
import com.ganesh.training_application_backend.auth.AppUserPrincipal;
import com.ganesh.training_application_backend.auth.Role;
import com.ganesh.training_application_backend.config.SecurityConfig;
import com.ganesh.training_application_backend.course.dto.CourseCreateRequest;
import com.ganesh.training_application_backend.course.dto.CourseManagementResponse;
import com.ganesh.training_application_backend.course.dto.CourseUpdateRequest;

@WebMvcTest(CourseManagementController.class)
@Import(SecurityConfig.class)
class CourseManagementControllerTests {

	@MockitoBean CourseManagementService service;
	@Autowired MockMvc mockMvc;

	@Test
	void anonymousAndUserCannotAccessManagement() throws Exception {
		mockMvc.perform(get("/api/management/courses")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/management/courses").with(user(principal(Role.USER))))
				.andExpect(status().isForbidden());
	}

	@Test
	void instructorAndAdminCanListCourses() throws Exception {
		when(service.getCourses()).thenReturn(List.of(response()));

		mockMvc.perform(get("/api/management/courses").with(user(principal(Role.INSTRUCTOR))))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].title").value("Managed Course"));
		mockMvc.perform(get("/api/management/courses").with(user(principal(Role.ADMIN))))
				.andExpect(status().isOk());
	}

	@Test
	void getsCourseAndReturnsNotFoundForUnknownCourse() throws Exception {
		when(service.getCourse(1L)).thenReturn(response());
		when(service.getCourse(99L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

		mockMvc.perform(get("/api/management/courses/1").with(user(principal(Role.INSTRUCTOR))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(1));
		mockMvc.perform(get("/api/management/courses/99").with(user(principal(Role.ADMIN))))
				.andExpect(status().isNotFound());
	}

	@Test
	void createsCourseWithCsrfAndRejectsInvalidInput() throws Exception {
		when(service.createCourse(any(CourseCreateRequest.class))).thenReturn(response());

		mockMvc.perform(post("/api/management/courses").with(user(principal(Role.INSTRUCTOR))).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(validBody()))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/management/courses/1"));
		mockMvc.perform(post("/api/management/courses").with(user(principal(Role.ADMIN))).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void userCannotCreateAndCsrfIsRequired() throws Exception {
		mockMvc.perform(post("/api/management/courses").with(user(principal(Role.USER))).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(validBody()))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/management/courses").with(user(principal(Role.ADMIN)))
				.contentType(MediaType.APPLICATION_JSON).content(validBody()))
				.andExpect(status().isForbidden());
	}

	@Test
	void updatesCourseWithCsrfAndValidatesInput() throws Exception {
		when(service.updateCourse(org.mockito.ArgumentMatchers.eq(1L), any(CourseUpdateRequest.class)))
				.thenReturn(response());

		mockMvc.perform(put("/api/management/courses/1").with(user(principal(Role.ADMIN))).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(validBody()))
				.andExpect(status().isOk());
		mockMvc.perform(put("/api/management/courses/1").with(user(principal(Role.INSTRUCTOR))).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void updateIsForbiddenForUserAndWithoutCsrf() throws Exception {
		mockMvc.perform(put("/api/management/courses/1").with(user(principal(Role.USER))).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(validBody()))
				.andExpect(status().isForbidden());
		mockMvc.perform(put("/api/management/courses/1").with(user(principal(Role.INSTRUCTOR)))
				.contentType(MediaType.APPLICATION_JSON).content(validBody()))
				.andExpect(status().isForbidden());
	}

	@Test
	void updateReturnsNotFoundForUnknownCourse() throws Exception {
		when(service.updateCourse(org.mockito.ArgumentMatchers.eq(99L), any(CourseUpdateRequest.class)))
				.thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

		mockMvc.perform(put("/api/management/courses/99").with(user(principal(Role.ADMIN))).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(validBody()))
				.andExpect(status().isNotFound());
	}

	@Test
	void deletesSafelyAndMapsNotFoundAndConflict() throws Exception {
		mockMvc.perform(delete("/api/management/courses/1").with(user(principal(Role.ADMIN))).with(csrf()))
				.andExpect(status().isNoContent());
		org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"))
				.when(service).deleteCourse(99L);
		org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Course has dependent records"))
				.when(service).deleteCourse(2L);
		mockMvc.perform(delete("/api/management/courses/99").with(user(principal(Role.INSTRUCTOR))).with(csrf()))
				.andExpect(status().isNotFound());
		mockMvc.perform(delete("/api/management/courses/2").with(user(principal(Role.ADMIN))).with(csrf()))
				.andExpect(status().isConflict());
	}

	@Test
	void deleteIsForbiddenForUserAndWithoutCsrf() throws Exception {
		mockMvc.perform(delete("/api/management/courses/1").with(user(principal(Role.USER))).with(csrf()))
				.andExpect(status().isForbidden());
		mockMvc.perform(delete("/api/management/courses/1").with(user(principal(Role.ADMIN))))
				.andExpect(status().isForbidden());
	}

	private CourseManagementResponse response() {
		return new CourseManagementResponse(1L, "Managed Course", "Description", "Instructor", 90,
				CourseLevel.BEGINNER, CourseCategory.INFORMATION_TECHNOLOGY);
	}

	private String validBody() {
		return """
				{"title":"Managed Course","description":"Description","instructor":"Instructor",
				 "duration":90,"level":"BEGINNER","category":"INFORMATION_TECHNOLOGY"}
				""";
	}

	private AppUserPrincipal principal(Role role) {
		return new AppUserPrincipal(new AppUser(7L, "Manager", "manager@example.com", "hash", role));
	}
}
