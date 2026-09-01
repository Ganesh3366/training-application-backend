package com.ganesh.training_application_backend.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ganesh.training_application_backend.admin.dto.CourseAssignmentResponse;
import com.ganesh.training_application_backend.auth.Role;
import com.ganesh.training_application_backend.auth.dto.UserResponse;
import com.ganesh.training_application_backend.config.SecurityConfig;
import com.ganesh.training_application_backend.course.CourseCategory;
import com.ganesh.training_application_backend.course.CourseLevel;
import com.ganesh.training_application_backend.course.dto.CourseManagementResponse;

@WebMvcTest(AdminUserController.class)
@Import(SecurityConfig.class)
class AdminUserControllerTests {

	@MockitoBean AdminUserService service;
	@Autowired MockMvc mockMvc;

	@Test
	void adminCanCreateUserAndReceivesOnlySafeAccountFields() throws Exception {
		when(service.createUser(any())).thenReturn(
				new UserResponse(12L, "Ada Lovelace", "ada@example.com", Role.INSTRUCTOR));

		mockMvc.perform(post("/api/admin/users")
				.with(user("admin").roles("ADMIN")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(validCreateRequest()))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/admin/users/12"))
				.andExpect(jsonPath("$.id").value(12))
				.andExpect(jsonPath("$.name").value("Ada Lovelace"))
				.andExpect(jsonPath("$.email").value("ada@example.com"))
				.andExpect(jsonPath("$.role").value("INSTRUCTOR"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void userCreationRequiresAdminRole() throws Exception {
		mockMvc.perform(post("/api/admin/users").with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(validCreateRequest()))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/admin/users")
				.with(user("user").roles("USER")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(validCreateRequest()))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/admin/users")
				.with(user("instructor").roles("INSTRUCTOR")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(validCreateRequest()))
				.andExpect(status().isForbidden());
	}

	@Test
	void userCreationRejectsInvalidFieldsAndUnknownRole() throws Exception {
		mockMvc.perform(post("/api/admin/users")
				.with(user("admin").roles("ADMIN")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"firstName\":\" \",\"lastName\":\"\",\"email\":\"invalid\","
						+ "\"password\":\"short\",\"role\":null}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/admin/users")
				.with(user("admin").roles("ADMIN")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\","
						+ "\"email\":\"ada@example.com\",\"password\":\"strong-password\","
						+ "\"role\":\"SUPER_ADMIN\"}"))
				.andExpect(status().isBadRequest());

		verify(service, never()).createUser(any());
	}

	@Test
	void onlyAdminCanListUsers() throws Exception {
		mockMvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/admin/users").with(user("user").roles("USER")))
				.andExpect(status().isForbidden());

		when(service.getUsers()).thenReturn(List.of(new UserResponse(1L, "Admin", "admin@example.com", Role.ADMIN)));
		mockMvc.perform(get("/api/admin/users").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1))
				.andExpect(jsonPath("$[0].email").value("admin@example.com"))
				.andExpect(jsonPath("$[0].password").doesNotExist())
				.andExpect(jsonPath("$[0].passwordHash").doesNotExist());
	}

	@Test
	void adminCanGetUserAndAssignments() throws Exception {
		when(service.getUser(1L)).thenReturn(new UserResponse(1L, "User", "user@example.com", Role.USER));
		when(service.getAssignments(1L)).thenReturn(List.of(assignment()));

		mockMvc.perform(get("/api/admin/users/1").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.name").value("User"));
		mockMvc.perform(get("/api/admin/users/1/assignments").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].course.id").value(5));
	}

	@Test
	void adminCanAssignCourseAndReceivesCreatedLocation() throws Exception {
		when(service.assignCourse(org.mockito.ArgumentMatchers.eq(1L), any())).thenReturn(assignment());

		mockMvc.perform(post("/api/admin/users/1/assignments")
				.with(user("admin").roles("ADMIN")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"courseId\":5}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/admin/users/1/assignments/10"))
				.andExpect(jsonPath("$.course.title").value("Course"));
	}

	@Test
	void authenticatedAdminCannotAssignCourseWithoutCsrfToken() throws Exception {
		mockMvc.perform(post("/api/admin/users/1/assignments")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON).content("{\"courseId\":5}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void assignmentRequiresAdminCsrfAndValidCourseId() throws Exception {
		mockMvc.perform(post("/api/admin/users/1/assignments").with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"courseId\":5}"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post("/api/admin/users/1/assignments")
				.with(user("user").roles("USER")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"courseId\":5}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/admin/users/1/assignments")
				.with(user("admin").roles("ADMIN")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/admin/users/1/assignments")
				.with(user("admin").roles("ADMIN")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"courseId\":0}"))
				.andExpect(status().isBadRequest());
	}

	private CourseAssignmentResponse assignment() {
		return new CourseAssignmentResponse(10L, new CourseManagementResponse(5L, "Course", "Description",
				"Instructor", 60, CourseLevel.BEGINNER, CourseCategory.INFORMATION_TECHNOLOGY), Instant.EPOCH);
	}

	private String validCreateRequest() {
		return "{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\","
				+ "\"email\":\"ada@example.com\",\"password\":\"strong-password\","
				+ "\"role\":\"INSTRUCTOR\"}";
	}
}
