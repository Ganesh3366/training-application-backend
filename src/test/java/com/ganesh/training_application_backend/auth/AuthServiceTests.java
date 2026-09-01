package com.ganesh.training_application_backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.ganesh.training_application_backend.auth.dto.SignupRequest;
import com.ganesh.training_application_backend.auth.dto.UserResponse;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

	@Mock
	private AppUserRepository appUserRepository;

	private BCryptPasswordEncoder passwordEncoder;
	private AuthService authService;

	@BeforeEach
	void setUp() {
		passwordEncoder = new BCryptPasswordEncoder();
		authService = new AuthService(appUserRepository, passwordEncoder);
	}

	@Test
	void signupNormalizesEmailEncodesPasswordAndAlwaysCreatesUserRole() {
		when(appUserRepository.existsByEmail("ganesh@example.com")).thenReturn(false);
		when(appUserRepository.saveAndFlush(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

		UserResponse response = authService.signup(
				new SignupRequest(" Ganesh ", "  Ganesh@Example.COM ", "SecurePass123!"));

		ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
		verify(appUserRepository).saveAndFlush(captor.capture());
		AppUser stored = captor.getValue();
		assertThat(stored.getName()).isEqualTo("Ganesh");
		assertThat(stored.getEmail()).isEqualTo("ganesh@example.com");
		assertThat(stored.getPasswordHash()).isNotEqualTo("SecurePass123!");
		assertThat(passwordEncoder.matches("SecurePass123!", stored.getPasswordHash())).isTrue();
		assertThat(stored.getRole()).isEqualTo(Role.USER);
		assertThat(stored.isEnabled()).isTrue();
		assertThat(response.role()).isEqualTo(Role.USER);
		assertThat(response.enabled()).isTrue();
		assertThat(SignupRequest.class.getRecordComponents())
				.extracting(component -> component.getName())
				.doesNotContain("role", "passwordHash");
	}

	@Test
	void rejectsDuplicateNormalizedEmailWithConflict() {
		when(appUserRepository.existsByEmail("ganesh@example.com")).thenReturn(true);

		assertThatThrownBy(() -> authService.signup(
				new SignupRequest("Ganesh", " GANESH@example.com ", "SecurePass123!")))
				.isInstanceOfSatisfying(ResponseStatusException.class,
						exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
		verify(appUserRepository, never()).saveAndFlush(any(AppUser.class));
	}
}
