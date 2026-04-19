package com.backendcr.residentialcomplex.auth;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.backendcr.residentialcomplex.dto.propiedad.TipoPropiedadNodoDto;

import jakarta.validation.Valid;
import lombok.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/login")
	public Object login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@PostMapping("/login/seleccionar")
	public LoginResponse seleccionarTenant(@RequestBody SeleccionTenantRequest request) {
		return authService.seleccionarTenant(request);
	}

	@PostMapping("/registro")
	@ResponseStatus(HttpStatus.CREATED)
	public RegistroResponse registro(@Valid @RequestBody RegistroRequest request) {
		return authService.registro(request);
	}

	@GetMapping("/tiposPropiedad")
	public List<TipoPropiedadNodoDto> tiposPropiedad(@RequestParam String codigo) {
		return authService.getTiposPropiedad(codigo);
	}
}
