package com.ganesh.training_application_backend.admin;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ganesh.training_application_backend.admin.dto.AdminUserCreateRequest;
import com.ganesh.training_application_backend.admin.dto.AdminUserEnabledRequest;
import com.ganesh.training_application_backend.admin.dto.AdminUserUpdateRequest;
import com.ganesh.training_application_backend.admin.dto.CourseAssignmentRequest;
import com.ganesh.training_application_backend.admin.dto.CourseAssignmentResponse;
import com.ganesh.training_application_backend.auth.AppUserPrincipal;
import com.ganesh.training_application_backend.auth.dto.UserResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

	private final AdminUserService service;

	public AdminUserController(AdminUserService service) {
		this.service = service;
	}

	@GetMapping
	public List<UserResponse> getUsers() { return service.getUsers(); }

	@PostMapping
	public ResponseEntity<UserResponse> createUser(@Valid @RequestBody AdminUserCreateRequest request) {
		UserResponse response = service.createUser(request);
		return ResponseEntity.created(URI.create("/api/admin/users/" + response.id())).body(response);
	}

	@GetMapping("/{userId}")
	public UserResponse getUser(@PathVariable Long userId) { return service.getUser(userId); }

	@PutMapping("/{userId}")
	public UserResponse updateUser(@PathVariable Long userId, @Valid @RequestBody AdminUserUpdateRequest request,
			@AuthenticationPrincipal AppUserPrincipal currentAdmin) {
		return service.updateUser(userId, request, currentAdmin.getId());
	}

	@PatchMapping("/{userId}/enabled")
	public UserResponse setUserEnabled(@PathVariable Long userId,
			@Valid @RequestBody AdminUserEnabledRequest request,
			@AuthenticationPrincipal AppUserPrincipal currentAdmin) {
		return service.setUserEnabled(userId, request, currentAdmin.getId());
	}

	@GetMapping("/{userId}/assignments")
	public List<CourseAssignmentResponse> getAssignments(@PathVariable Long userId) {
		return service.getAssignments(userId);
	}

	@PostMapping("/{userId}/assignments")
	public ResponseEntity<CourseAssignmentResponse> assignCourse(@PathVariable Long userId,
			@Valid @RequestBody CourseAssignmentRequest request) {
		CourseAssignmentResponse response = service.assignCourse(userId, request);
		return ResponseEntity.created(URI.create("/api/admin/users/" + userId + "/assignments/" + response.id()))
				.body(response);
	}
}
