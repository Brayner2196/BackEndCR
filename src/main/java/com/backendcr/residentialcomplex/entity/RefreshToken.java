package com.backendcr.residentialcomplex.entity;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name = "refresh_tokens", schema = "public")
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 64)
	private String token;

	@Column(name = "identidad_id", nullable = false)
	private Long identidadId;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	@Column(nullable = false)
	private boolean revoked = false;

	// ─── getters / setters ───────────────────────────────────────────────

	public Long getId() { return id; }

	public String getToken() { return token; }
	public void setToken(String token) { this.token = token; }

	public Long getIdentidadId() { return identidadId; }
	public void setIdentidadId(Long identidadId) { this.identidadId = identidadId; }

	public Instant getExpiresAt() { return expiresAt; }
	public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

	public boolean isRevoked() { return revoked; }
	public void setRevoked(boolean revoked) { this.revoked = revoked; }
}
