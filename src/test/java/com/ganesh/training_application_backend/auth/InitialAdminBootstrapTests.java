package com.ganesh.training_application_backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class InitialAdminBootstrapTests {

	@Mock
	private AppUserRepository appUserRepository;

	private BCryptPasswordEncoder passwordEncoder;

	@BeforeEach
	void setUp() {
		passwordEncoder = new BCryptPasswordEncoder();
	}

	@Test
	void createsAdminWithNormalizedEmailAndEncodedPasswordWhenConfigured() {
		when(appUserRepository.existsByEmail("admin@example.com")).thenReturn(false);
		InitialAdminBootstrap bootstrap = bootstrap(" SkillForge Admin ", " ADMIN@Example.COM ", "AdminPass123!");

		bootstrap.run(new DefaultApplicationArguments(new String[0]));

		ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
		verify(appUserRepository).save(captor.capture());
		AppUser admin = captor.getValue();
		assertThat(admin.getName()).isEqualTo("SkillForge Admin");
		assertThat(admin.getEmail()).isEqualTo("admin@example.com");
		assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
		assertThat(admin.isEnabled()).isTrue();
		assertThat(admin.getPasswordHash()).isNotEqualTo("AdminPass123!");
		assertThat(passwordEncoder.matches("AdminPass123!", admin.getPasswordHash())).isTrue();
	}

	@Test
	void skipsWhenAdminEmailAlreadyExists() {
		when(appUserRepository.existsByEmail("admin@example.com")).thenReturn(true);

		bootstrap("Admin", "admin@example.com", "AdminPass123!")
				.run(new DefaultApplicationArguments(new String[0]));

		verify(appUserRepository, never()).save(any(AppUser.class));
	}

	@Test
	void skipsSafelyWhenRequiredConfigurationIsMissing() {
		bootstrap("Admin", "", "").run(new DefaultApplicationArguments(new String[0]));

		verifyNoInteractions(appUserRepository);
	}

	private InitialAdminBootstrap bootstrap(String name, String email, String password) {
		return new InitialAdminBootstrap(appUserRepository, passwordEncoder, name, email, password);
	}
}
