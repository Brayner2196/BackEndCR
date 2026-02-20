package com.backendcr.residentialcomplex.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "tenants", schema = "public")
public class Tenant {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String schemaName;   // "acme", "nike", "adidas"

    @Column(nullable = false)
    private String nombre;       // "ACME Corp", "Nike Inc"

    private boolean activo;

    
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
}
