package com.ganesh.training_application_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		RequestMatcher quizSubmit = PathPatternRequestMatcher.pathPattern(
				HttpMethod.POST, "/api/courses/{courseId}/modules/{moduleId}/quiz/submit");

		// Temporary authorization boundary until the real authentication flow is implemented.
		return http
				.csrf(csrf -> csrf.ignoringRequestMatchers(quizSubmit))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(HttpMethod.GET, "/api/courses", "/api/courses/**").permitAll()
						// Temporary until learner authentication and progress tracking are implemented.
						.requestMatchers(quizSubmit).permitAll()
						.anyRequest().authenticated())
				.build();
	}
}
