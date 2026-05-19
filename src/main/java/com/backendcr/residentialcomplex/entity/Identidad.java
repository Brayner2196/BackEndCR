package com.backendcr.residentialcomplex.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "identidades", schema = "public")
public class Identidad {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String email;

	@Column(nullable = false)
	private String password;

	@Column(nullable = false)
	private String rol; // SUPER_ADMIN, TENANT_ADMIN, RESIDENTE, RESIDENTE_PENDIENTE, PROPIETARIO,
						// INQUILINO, VIGILANTE, PORTERO, PISCINERO, CONTADOR

	@Column(name = "tenant_id")
	private String tenantId; // NULL si es SUPER_ADMIN

	@Column(nullable = false)
	private boolean activo = true;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getRol() {
		return rol;
	}

	public void setRol(String rol) {
		this.rol = rol;
	}

	public String getTenantId() {
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	public boolean isActivo() {
		return activo;
	}

	public void setActivo(boolean activo) {
		this.activo = activo;
	}

}
