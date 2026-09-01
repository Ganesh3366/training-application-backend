package com.ganesh.training_application_backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;

class AppUserSchemaTests {

	@Test
	void enabledIsNonNullDefaultsTrueAndDeclaresDatabaseDefault() throws Exception {
		Column column = AppUser.class.getDeclaredField("enabled").getAnnotation(Column.class);

		assertThat(column).isNotNull();
		assertThat(column.nullable()).isFalse();
		assertThat(column.columnDefinition()).isEqualToIgnoringCase("boolean default true");
		assertThat(new AppUser(1L, "User", "user@example.com", "hash", Role.USER).isEnabled()).isTrue();
	}
}
