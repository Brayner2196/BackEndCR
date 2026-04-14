package com.backendcr.residentialcomplex.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;


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

	// Paso 2 — solo si el usuario tiene múltiples tenants
	@PostMapping("/seleccionar")
	public LoginResponse seleccionarTenant(@RequestBody SeleccionTenantRequest request) {
		return authService.seleccionarTenant(request);
	}

	// Auto-registro de residentes
	@PostMapping("/registro")
	@ResponseStatus(HttpStatus.CREATED)
	public RegistroResponse registro(@Valid @RequestBody RegistroRequest request) {
		return authService.registro(request);
	}
}
