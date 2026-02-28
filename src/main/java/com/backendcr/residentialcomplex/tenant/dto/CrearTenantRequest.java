package com.backendcr.residentialcomplex.tenant.dto;

public class CrearTenantRequest {

	private String schemaName;
	private String nombre;

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

}
