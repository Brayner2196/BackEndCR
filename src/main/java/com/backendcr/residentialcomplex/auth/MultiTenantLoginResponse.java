package com.backendcr.residentialcomplex.auth;

import java.util.List;

public record MultiTenantLoginResponse(boolean requiereSeleccion, List<OpcionTenant> conjuntos) {
	public record OpcionTenant(String tenantId, String nombre, String direccion) {
	}
}
