package com.ganesh.training_application_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	SecurityContextRepository securityContextRepository() {
		return new HttpSessionSecurityContextRepository();
	}

	@Bean
	SessionAuthenticationStrategy sessionAuthenticationStrategy() {
		return new ChangeSessionIdAuthenticationStrategy();
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http,
			SecurityContextRepository securityContextRepository) throws Exception {
		RequestMatcher quizRead = PathPatternRequestMatcher.pathPattern(
				HttpMethod.GET, "/api/courses/{courseId}/modules/{moduleId}/quiz");
		RequestMatcher quizSubmit = PathPatternRequestMatcher.pathPattern(
				HttpMethod.POST, "/api/courses/{courseId}/modules/{moduleId}/quiz/submit");
		RequestMatcher signup = PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/auth/signup");
		RequestMatcher login = PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/auth/login");
		RequestMatcher csrfToken = PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/api/auth/csrf");
		AuthenticationEntryPoint unauthorized = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
		CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		csrfTokenRepository.setCookiePath("/");

		return http
				.securityContext(context -> context
						.securityContextRepository(securityContextRepository)
						.requireExplicitSave(true))
				.csrf(csrf -> csrf
						.csrfTokenRepository(csrfTokenRepository)
						.spa()
						.ignoringRequestMatchers(signup, login))
				.exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(unauthorized))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(signup, login, csrfToken).permitAll()
						.requestMatchers(quizRead, quizSubmit).authenticated()
						.requestMatchers(HttpMethod.GET,
								"/api/courses",
								"/api/courses/{courseId}",
								"/api/courses/{courseId}/modules",
								"/api/courses/{courseId}/modules/{moduleId}").permitAll()
						.anyRequest().authenticated())
				.build();
	}
}
