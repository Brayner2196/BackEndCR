package com.backendcr.residentialcomplex.auth;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.backendcr.residentialcomplex.dto.propiedad.TipoPropiedadNodoDto;
import com.backendcr.residentialcomplex.dto.propiedad.ValorTipoPropiedadDto;

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

	@PostMapping("/refresh")
	public RefreshResponse refresh(@Valid @RequestBody RefreshRequest request) {
		return authService.refresh(request);
	}

	@GetMapping("/tiposPropiedad")
	public List<TipoPropiedadNodoDto> tiposPropiedad(@RequestParam String codigo) {
		return authService.getTiposPropiedad(codigo);
	}

	/** Valores permitidos de un nivel, para los dropdowns del registro público. */
	@GetMapping("/tiposPropiedad/{tipoId}/valores")
	public List<ValorTipoPropiedadDto> valoresPropiedad(
			@PathVariable Long tipoId,
			@RequestParam String codigo,
			@RequestParam(required = false) Long parentValorId) {
		return authService.getValoresPropiedad(codigo, tipoId, parentValorId);
	}
}
