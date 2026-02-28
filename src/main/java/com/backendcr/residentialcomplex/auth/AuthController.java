package com.backendcr.residentialcomplex.auth;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.*;

@RestController
@RequestMapping("/auth/login")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	// Paso 1 — login normal, sin tenant
	@PostMapping
	public Object login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
		// Devuelve LoginResponse → si hay un solo tenant o es admin
		// Devuelve MultiTenantResponse → si el email está en varios tenants
	}

	// Paso 2 — solo si había múltiples tenants
	@PostMapping("/seleccionar")
	public LoginResponse seleccionarTenant(@RequestBody SeleccionTenantRequest request) {
		return authService.seleccionarTenant(request);
	}
}
