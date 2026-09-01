package com.ganesh.training_application_backend.auth;

import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_users")
public class AppUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Role role;

	@Column(nullable = false, columnDefinition = "boolean default true")
	private boolean enabled = true;

	protected AppUser() {
	}

	public AppUser(Long id, String name, String email, String passwordHash, Role role) {
		this(id, name, email, passwordHash, role, true);
	}

	public AppUser(Long id, String name, String email, String passwordHash, Role role, boolean enabled) {
		this.id = id;
		this.name = name == null ? null : name.trim();
		this.email = normalizeEmail(email);
		this.passwordHash = passwordHash;
		this.role = role;
		this.enabled = enabled;
	}

	public static String normalizeEmail(String email) {
		return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public Role getRole() {
		return role;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void updateAccount(String name, String email, Role role) {
		this.name = name == null ? null : name.trim();
		this.email = normalizeEmail(email);
		this.role = role;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
