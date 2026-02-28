package com.backendcr.residentialcomplex.auth;

public record SeleccionTenantRequest(String email, String password, String tenantId // el que eligió en pantalla
) {

}
