package com.backendcr.residentialcomplex.auth;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.backendcr.residentialcomplex.entity.Identidad;
import com.backendcr.residentialcomplex.entity.Tenant;
import com.backendcr.residentialcomplex.repository.IdentidadRepository;
import com.backendcr.residentialcomplex.tenant.repository.TenantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final IdentidadRepository identidadRepository;
	private final TenantRepository tenantRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final JdbcTemplate jdbcTemplate;

	public Object login(LoginRequest request) {
		
		List<Identidad> identidades = identidadRepository.findAllByEmail(request.email());

		if (identidades.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
		}

		// Validar password con la primera identidad encontrada (todas tienen el mismo
		// password)
		boolean passwordValido = identidades.stream()
				.anyMatch(i -> passwordEncoder.matches(request.password(), i.getPassword()));

		if (!passwordValido) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
		}

		// CASO 1 — Es SUPER_ADMIN (tenant_id = null)
		Identidad superAdmin = identidades.stream().filter(i -> i.getTenantId() == null).findFirst().orElse(null);

		if (superAdmin != null) {
			return generarLoginResponse(superAdmin, "public", null);
		}

		// CASO 2 — Existe en un solo tenant → login directo
		if (identidades.size() == 1) {
			Identidad identidad = identidades.get(0);

			// Bloqueado si está pendiente
			if ("RESIDENTE_PENDIENTE".equals(identidad.getRol())) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN,
						"Tu cuenta está pendiente de aprobación por el administrador");
			}

			Tenant tenant = tenantRepository.findBySchemaName(identidad.getTenantId()).orElseThrow(
					() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Tenant no encontrado"));

			return generarLoginResponse(identidad, identidad.getTenantId(), tenant.getNombre());
		}

		// CASO 3 — Existe en múltiples tenants → pedir selección
		List<MultiTenantLoginResponse.OpcionTenant> opciones = identidades.stream().map(i -> {
			java.util.Optional<Tenant> tenant = tenantRepository.findBySchemaName(i.getTenantId());
			String nombre = tenant.map(Tenant::getNombre).orElse(i.getTenantId());
			String direccion = tenant.map(Tenant::getDireccion).orElse(null);
			return new MultiTenantLoginResponse.OpcionTenant(i.getTenantId(), nombre, direccion);
		}).toList();

		return new MultiTenantLoginResponse(true, opciones);

	}// End login

	// Segunda llamada cuando el usuario ya eligió su conjunto
	public LoginResponse seleccionarTenant(SeleccionTenantRequest request) {

		Identidad identidad = identidadRepository.findByEmailAndTenantId(request.email(), request.tenantId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas"));

		if (!passwordEncoder.matches(request.password(), identidad.getPassword())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
		}

		if ("RESIDENTE_PENDIENTE".equals(identidad.getRol())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tu cuenta está pendiente de aprobación");
		}

		Tenant tenant = tenantRepository.findBySchemaName(request.tenantId()).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Tenant no encontrado"));

		return generarLoginResponse(identidad, request.tenantId(), tenant.getNombre());
	}

	@Transactional
	public RegistroResponse registro(RegistroRequest request) {

		Tenant tenant = tenantRepository.findByCodigo(request.codigoConjunto())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"No existe un conjunto con ese código"));

		if (!tenant.isActivo()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El conjunto no está activo");
		}

		if (identidadRepository.existsByEmailAndTenantId(request.email(), tenant.getSchemaName())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT,
					"Ya existe una cuenta con ese email en este conjunto");
		}

		// Crear credenciales con rol RESIDENTE_PENDIENTE
		Identidad identidad = new Identidad();
		identidad.setEmail(request.email());
		identidad.setPassword(passwordEncoder.encode(request.password()));
		identidad.setRol("RESIDENTE_PENDIENTE");
		identidad.setTenantId(tenant.getSchemaName());
		identidad = identidadRepository.save(identidad);

		// Insertar perfil en el schema del tenant con estado PENDIENTE
		jdbcTemplate.update("""
				INSERT INTO %s.usuarios (nombre, identidad_id, apto, torre, telefono, estado)
				VALUES (?, ?, ?, ?, ?, 'PENDIENTE')
				""".formatted(tenant.getSchemaName()),
				request.nombre(), identidad.getId(),
				request.apto(), request.torre(), request.telefono());

		return new RegistroResponse(
				"Tu solicitud fue recibida. Un administrador debe aprobar tu cuenta antes de que puedas ingresar.",
				request.email(),
				tenant.getNombre()
		);
	}

	/**
	 * Consulta el schema del tenant directamente via JdbcTemplate (sin pasar por el
	 * routing de Hibernate, que no está activo durante los endpoints de /auth/login)
	 * para obtener el nombre del usuario autenticado.
	 * Retorna null si tenantId es null o "public" (SUPER_ADMIN) o si no se encuentra registro.
	 */
	private String obtenerNombreDesdeSchema(String tenantId, Long identidadId) {
		if (tenantId == null || "public".equals(tenantId)) {
			return null;
		}
		if (!tenantId.matches("^[a-zA-Z0-9_]+$")) {
			return null;
		}
		try {
			return jdbcTemplate.queryForObject(
					"SELECT nombre FROM " + tenantId + ".usuarios WHERE identidad_id = ?",
					String.class,
					identidadId
			);
		} catch (Exception e) {
			return null;
		}
	}

	private LoginResponse generarLoginResponse(Identidad identidad, String tenantId, String nombreConjunto) {
		String token = jwtService.generarToken(identidad.getId(), identidad.getEmail(), identidad.getRol(), tenantId);
		String nombre = obtenerNombreDesdeSchema(tenantId, identidad.getId());
		return new LoginResponse(token, identidad.getEmail(), identidad.getRol(), tenantId, nombreConjunto, nombre);
	}

}
