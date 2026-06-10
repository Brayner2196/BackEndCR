package com.backendcr.residentialcomplex.auth;

public record LoginResponse(
		String token,
		String refreshToken,
		String email,
		String rol,
		String tenantId,
		String nombreConjunto, // null si es SUPER_ADMIN
		String nombre,         // null si es SUPER_ADMIN
		String timezone,       // ej: "America/Bogota" — null si es SUPER_ADMIN
		boolean esConsejero,   // true si tiene membresía activa en el consejo comunal
		String cargoConsejo    // "PRESIDENTE", "VOCAL", etc. — null si no es consejero
) {

}
