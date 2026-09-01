package com.ganesh.training_application_backend.auth;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ganesh.training_application_backend.auth.dto.SignupRequest;
import com.ganesh.training_application_backend.auth.dto.UserResponse;

@Service
public class AuthService {

	private final AppUserRepository appUserRepository;
	private final PasswordEncoder passwordEncoder;

	public AuthService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
		this.appUserRepository = appUserRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public UserResponse signup(SignupRequest request) {
		String email = AppUser.normalizeEmail(request.email());
		if (appUserRepository.existsByEmail(email)) {
			throw duplicateEmail();
		}

		AppUser user = new AppUser(
				null,
				request.name(),
				email,
				passwordEncoder.encode(request.password()),
				Role.USER);
		try {
			return toResponse(appUserRepository.saveAndFlush(user));
		} catch (DataIntegrityViolationException exception) {
			throw duplicateEmail();
		}
	}

	public UserResponse toResponse(AppUserPrincipal principal) {
		return new UserResponse(principal.getId(), principal.getName(), principal.getUsername(), principal.getRole(),
				principal.isEnabled());
	}

	private UserResponse toResponse(AppUser user) {
		return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.isEnabled());
	}

	private ResponseStatusException duplicateEmail() {
		return new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists");
	}
}
