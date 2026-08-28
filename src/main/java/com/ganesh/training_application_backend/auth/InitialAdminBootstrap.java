package com.ganesh.training_application_backend.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class InitialAdminBootstrap implements ApplicationRunner {

	private static final Logger logger = LoggerFactory.getLogger(InitialAdminBootstrap.class);

	private final AppUserRepository appUserRepository;
	private final PasswordEncoder passwordEncoder;
	private final String adminName;
	private final String adminEmail;
	private final String adminPassword;

	public InitialAdminBootstrap(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder,
			@Value("${app.admin.name:}") String adminName,
			@Value("${app.admin.email:}") String adminEmail,
			@Value("${app.admin.password:}") String adminPassword) {
		this.appUserRepository = appUserRepository;
		this.passwordEncoder = passwordEncoder;
		this.adminName = adminName;
		this.adminEmail = adminEmail;
		this.adminPassword = adminPassword;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (!StringUtils.hasText(adminName) || !StringUtils.hasText(adminEmail)
				|| !StringUtils.hasText(adminPassword)) {
			return;
		}

		String normalizedEmail = AppUser.normalizeEmail(adminEmail);
		if (appUserRepository.existsByEmail(normalizedEmail)) {
			logger.info("Initial admin bootstrap skipped because the account already exists");
			return;
		}

		appUserRepository.save(new AppUser(
				null, adminName, normalizedEmail, passwordEncoder.encode(adminPassword), Role.ADMIN));
		logger.info("Created initial administrator account");
	}
}
