package com.backendcr.residentialcomplex.auth;

public record LoginResponse(String token, String email, String rol, String tenantId, String nombreConjunto // null si es
																											// SUPER_ADMIN
) {

}
