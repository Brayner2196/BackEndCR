package com.backendcr.residentialcomplex.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "tenants", schema = "public")
public class Tenant {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false)
	private String schemaName;

	@Column(nullable = false)
	private String nombre; 

	@Column(unique = true, nullable = false)
	private String codigo;

	private boolean activo = true;

	@Column
	private String direccion;

	@Column(nullable = false, length = 50)
	private String timezone = "America/Bogota";

	@Transient
	private int cantidadUsuarios;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSchemaName() {
		return schemaName;
	}

	public void setSchemaName(String schemaName) {
		this.schemaName = schemaName;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public boolean isActivo() {
		return activo;
	}

	public void setActivo(boolean activo) {
		this.activo = activo;
	}

	public String getCodigo() {
		return codigo;
	}

	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}

	public String getDireccion() {
		return direccion;
	}

	public void setDireccion(String direccion) {
		this.direccion = direccion;
	}

	public String getTimezone() {
		return timezone;
	}

	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	public int getCantidadUsuarios() {
		return cantidadUsuarios;
	}

	public void setCantidadUsuarios(int cantidadUsuarios) {
		this.cantidadUsuarios = cantidadUsuarios;
	}
}
