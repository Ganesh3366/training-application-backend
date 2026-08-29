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
import com.ganesh.training_application_backend.course.dto.CourseModuleCreateRequest;
import com.ganesh.training_application_backend.course.dto.CourseModuleManagementResponse;
import com.ganesh.training_application_backend.course.dto.CourseModuleUpdateRequest;
import com.ganesh.training_application_backend.course.dto.ModuleContentCreateRequest;
import com.ganesh.training_application_backend.course.dto.ModuleContentManagementResponse;
import com.ganesh.training_application_backend.course.dto.ModuleContentUpdateRequest;

@WebMvcTest(CourseModuleManagementController.class)
@Import(SecurityConfig.class)
class CourseModuleManagementControllerTests {

	@MockitoBean CourseModuleManagementService service;
	@Autowired MockMvc mockMvc;

	@Test
	void anonymousAndUserCannotAccessManagementGet() throws Exception {
		mockMvc.perform(get("/api/management/courses/1/modules")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/management/courses/1/modules").with(user(principal(Role.USER))))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminAndInstructorCanReadModulesAndContents() throws Exception {
		when(service.getModules(1L)).thenReturn(List.of(moduleResponse()));
		when(service.getContents(1L, 10L)).thenReturn(List.of(contentResponse()));

		mockMvc.perform(get("/api/management/courses/1/modules").with(user(principal(Role.ADMIN))))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].position").value(1));
		mockMvc.perform(get("/api/management/courses/1/modules/10/contents")
				.with(user(principal(Role.INSTRUCTOR))))
				.andExpect(status().isOk()).andExpect(jsonPath("$[0].type").value("TEXT"));
	}

	@Test
	void createsModuleAndContentWithLocationHeaders() throws Exception {
		when(service.createModule(org.mockito.ArgumentMatchers.eq(1L), any(CourseModuleCreateRequest.class)))
				.thenReturn(moduleResponse());
		when(service.createContent(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(10L),
				any(ModuleContentCreateRequest.class))).thenReturn(contentResponse());

		mockMvc.perform(post("/api/management/courses/1/modules").with(user(principal(Role.INSTRUCTOR))).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Module\"}"))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/management/courses/1/modules/10"));
		mockMvc.perform(post("/api/management/courses/1/modules/10/contents")
				.with(user(principal(Role.ADMIN))).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(textBody()))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/api/management/courses/1/modules/10/contents/100"));
	}

	@Test
	void createValidationAndCsrfRemainEnforced() throws Exception {
		mockMvc.perform(post("/api/management/courses/1/modules").with(user(principal(Role.ADMIN))).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"title\":\" \"}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(post("/api/management/courses/1/modules").with(user(principal(Role.ADMIN)))
				.contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Module\"}"))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/management/courses/1/modules").with(user(principal(Role.USER))).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Module\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void updatesModuleAndContentWithCsrf() throws Exception {
		when(service.updateModule(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(10L),
				any(CourseModuleUpdateRequest.class))).thenReturn(moduleResponse());
		when(service.updateContent(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq(10L),
				org.mockito.ArgumentMatchers.eq(100L), any(ModuleContentUpdateRequest.class)))
				.thenReturn(contentResponse());

		mockMvc.perform(put("/api/management/courses/1/modules/10").with(user(principal(Role.ADMIN))).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Updated\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(put("/api/management/courses/1/modules/10/contents/100")
				.with(user(principal(Role.INSTRUCTOR))).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content(textBody()))
				.andExpect(status().isOk());
	}

	@Test
	void updateValidationAndCsrfRemainEnforced() throws Exception {
		mockMvc.perform(put("/api/management/courses/1/modules/10").with(user(principal(Role.ADMIN))).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(put("/api/management/courses/1/modules/10").with(user(principal(Role.INSTRUCTOR)))
				.contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Updated\"}"))
				.andExpect(status().isForbidden());
	}

	@Test
	void deletesModuleAndContentWithCsrf() throws Exception {
		mockMvc.perform(delete("/api/management/courses/1/modules/10")
				.with(user(principal(Role.ADMIN))).with(csrf())).andExpect(status().isNoContent());
		mockMvc.perform(delete("/api/management/courses/1/modules/10/contents/100")
				.with(user(principal(Role.INSTRUCTOR))).with(csrf())).andExpect(status().isNoContent());
	}

	@Test
	void deleteCsrfAndRoleRemainEnforced() throws Exception {
		mockMvc.perform(delete("/api/management/courses/1/modules/10").with(user(principal(Role.ADMIN))))
				.andExpect(status().isForbidden());
		mockMvc.perform(delete("/api/management/courses/1/modules/10")
				.with(user(principal(Role.USER))).with(csrf())).andExpect(status().isForbidden());
	}

	@Test
	void nestedNotFoundAndDependencyConflictArePreserved() throws Exception {
		when(service.getContents(1L, 99L))
				.thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Course module not found"));
		org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.CONFLICT,
				"Course module has protected dependencies")).when(service).deleteModule(1L, 10L);

		mockMvc.perform(get("/api/management/courses/1/modules/99/contents")
				.with(user(principal(Role.ADMIN)))).andExpect(status().isNotFound());
		mockMvc.perform(delete("/api/management/courses/1/modules/10")
				.with(user(principal(Role.INSTRUCTOR))).with(csrf())).andExpect(status().isConflict());
	}

	private CourseModuleManagementResponse moduleResponse() {
		return new CourseModuleManagementResponse(10L, "Module", "Description", 1);
	}

	private ModuleContentManagementResponse contentResponse() {
		return new ModuleContentManagementResponse(100L, ModuleContentType.TEXT, "Text", "Body", null, 1);
	}

	private String textBody() {
		return "{\"type\":\"TEXT\",\"title\":\"Text\",\"textContent\":\"Body\"}";
	}

	private AppUserPrincipal principal(Role role) {
		return new AppUserPrincipal(new AppUser(7L, "Manager", "manager@example.com", "hash", role));
	}
}
