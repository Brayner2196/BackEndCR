package com.backendcr.residentialcomplex.tenant.dto;

public record CrearTenantRequest (
		String schemaName,
		String nombre,
		String codigo
) {

}
