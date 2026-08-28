package com.ganesh.training_application_backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.server.ResponseStatusException;


import com.ganesh.training_application_backend.auth.dto.SignupRequest;
import com.ganesh.training_application_backend.auth.dto.UserResponse;
import com.ganesh.training_application_backend.config.AuthenticationConfig;
import com.ganesh.training_application_backend.config.SecurityConfig;

import jakarta.servlet.http.Cookie;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, AuthenticationConfig.class})
class AuthControllerTests {

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private UserDetailsService userDetailsService;

	private final MockMvc mockMvc;
	private final PasswordEncoder passwordEncoder;

	@Autowired
	AuthControllerTests(MockMvc mockMvc, PasswordEncoder passwordEncoder) {
		this.mockMvc = mockMvc;
		this.passwordEncoder = passwordEncoder;
	}

	@Test
	void signupIsPublicReturnsCreatedAndNeverSerializesPasswordHash() throws Exception {
		when(authService.signup(any(SignupRequest.class)))
				.thenReturn(new UserResponse(1L, "Ganesh", "ganesh@example.com", Role.USER));

		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"Ganesh","email":"ganesh@example.com","password":"SecurePass123!","role":"ADMIN"}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value("ganesh@example.com"))
				.andExpect(jsonPath("$.role").value("USER"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void invalidSignupReturnsBadRequest() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"","email":"not-an-email","password":"short"}
						"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void duplicateSignupReturnsConflict() throws Exception {
		when(authService.signup(any(SignupRequest.class)))
				.thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Account already exists"));

		mockMvc.perform(post("/api/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"Ganesh","email":"ganesh@example.com","password":"SecurePass123!"}
						"""))
				.andExpect(status().isConflict());
	}

	@Test
	void validLoginCreatesSessionAndMeUsesAuthenticatedPrincipal() throws Exception {
		AppUserPrincipal principal = principal();
		when(userDetailsService.loadUserByUsername("ganesh@example.com")).thenReturn(principal);
		when(authService.toResponse(any(AppUserPrincipal.class)))
				.thenReturn(new UserResponse(1L, "Ganesh", "ganesh@example.com", Role.USER));

		MvcResult login = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":" GANESH@EXAMPLE.COM ","password":"SecurePass123!"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andReturn();

		MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
		assertThat(session).isNotNull();
		mockMvc.perform(get("/api/auth/me").session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("ganesh@example.com"))
				.andExpect(jsonPath("$.role").value("USER"))
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void invalidLoginReturnsSafeUnauthorizedResponse() throws Exception {
		when(userDetailsService.loadUserByUsername("ganesh@example.com"))
				.thenThrow(new UsernameNotFoundException("not found"));

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"ganesh@example.com","password":"WrongPassword"}
						"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void unauthenticatedMeReturnsUnauthorized() throws Exception {
		mockMvc.perform(get("/api/auth/me"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void csrfEndpointIsPublicAndCreatesAngularCompatibleCookie() throws Exception {
		mockMvc.perform(get("/api/auth/csrf"))
				.andExpect(status().isOk())
				.andExpect(cookie().exists("XSRF-TOKEN"))
				.andExpect(cookie().httpOnly("XSRF-TOKEN", false))
				.andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"));
	}

	@Test
	void logoutRejectsMissingCsrfAndAcceptsAngularCookieAndHeader() throws Exception {
		MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
				.andExpect(status().isOk())
				.andExpect(cookie().exists("XSRF-TOKEN"))
				.andReturn();
		Cookie csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");
		MockHttpSession session = loginSession();

		mockMvc.perform(post("/api/auth/logout").session(session))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/auth/logout")
				.session(session)
				.cookie(csrfCookie)
				.header("X-XSRF-TOKEN", csrfCookie.getValue()))
				.andExpect(status().isNoContent());

		assertThat(session.isInvalid()).isTrue();
	}

	@Test
	void unrelatedEndpointRemainsProtectedWhileExistingPublicRoutesStayPublic() throws Exception {
		mockMvc.perform(post("/api/unrelated"))
				.andExpect(status().isForbidden());
		MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf")).andReturn();
		Cookie csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");
		mockMvc.perform(post("/api/unrelated")
				.cookie(csrfCookie)
				.header("X-XSRF-TOKEN", csrfCookie.getValue()))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/courses"))
				.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/courses/1/modules/10/quiz/submit"))
				.andExpect(status().isNotFound());
	}

	private MockHttpSession loginSession() throws Exception {
		when(userDetailsService.loadUserByUsername("ganesh@example.com")).thenReturn(principal());
		when(authService.toResponse(any(AppUserPrincipal.class)))
				.thenReturn(new UserResponse(1L, "Ganesh", "ganesh@example.com", Role.USER));
		MvcResult result = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"email":"ganesh@example.com","password":"SecurePass123!"}
						"""))
				.andExpect(status().isOk())
				.andReturn();
		return (MockHttpSession) result.getRequest().getSession(false);
	}

	private AppUserPrincipal principal() {
		return new AppUserPrincipal(new AppUser(
				1L,
				"Ganesh",
				"ganesh@example.com",
				passwordEncoder.encode("SecurePass123!"),
				Role.USER));
	}
}
