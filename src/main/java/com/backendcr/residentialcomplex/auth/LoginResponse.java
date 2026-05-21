package com.backendcr.residentialcomplex.auth;

public record LoginResponse(
		String token,
		String refreshToken,
		String email,
		String rol,
		String tenantId,
		String nombreConjunto, // null si es SUPER_ADMIN
		String nombre,         // null si es SUPER_ADMIN
		String timezone        // ej: "America/Bogota" — null si es SUPER_ADMIN
) {

}
