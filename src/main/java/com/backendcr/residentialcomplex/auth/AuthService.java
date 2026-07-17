package com.backendcr.residentialcomplex.auth;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.dto.propiedad.TipoPropiedadNodoDto;
import com.backendcr.residentialcomplex.dto.propiedad.ValorTipoPropiedadDto;
import com.backendcr.residentialcomplex.entity.Identidad;
import com.backendcr.residentialcomplex.entity.Tenant;
import com.backendcr.residentialcomplex.repository.IdentidadRepository;
import com.backendcr.residentialcomplex.repository.MiembroConsejoRepository;
import com.backendcr.residentialcomplex.repository.UsuarioRepository;
import com.backendcr.residentialcomplex.service.PropiedadService;
import com.backendcr.residentialcomplex.service.UsuarioService;
import com.backendcr.residentialcomplex.service.ValorTipoPropiedadService;
import com.backendcr.residentialcomplex.tenant.repository.TenantRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final IdentidadRepository identidadRepository;
	private final TenantRepository tenantRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final RefreshTokenService refreshTokenService;
	private final JdbcTemplate jdbcTemplate;
	private final PropiedadService propiedadService;
	private final ValorTipoPropiedadService valorService;
	private final UsuarioService usuarioService;
	private final UsuarioRepository usuarioRepository;
	private final MiembroConsejoRepository miembroConsejoRepository;

	public Object login(LoginRequest request) {
		
		List<Identidad> identidades = identidadRepository.findAllByEmail(request.email());

		if (identidades.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
		}

		boolean passwordValido = identidades.stream()
				.anyMatch(i -> passwordEncoder.matches(request.password(), i.getPassword()));

		if (!passwordValido) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
		}

		Identidad superAdmin = identidades.stream().filter(i -> i.getTenantId() == null).findFirst().orElse(null);

		if (superAdmin != null) {
			if (!superAdmin.isActivo()) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tu cuenta está inactiva");
			}
			return generarLoginResponse(superAdmin, "public", null);
		}

		// Filtrar solo identidades activas con tenants activos
		List<Identidad> activas = identidades.stream().filter(i -> {
			if (!i.isActivo()) return false;
			return tenantRepository.findBySchemaName(i.getTenantId())
					.map(Tenant::isActivo).orElse(false);
		}).toList();

		// Si todas están inactivas, determinar el motivo más específico
		if (activas.isEmpty()) {
			boolean hayUsuarioInactivo = identidades.stream().anyMatch(i -> !i.isActivo());
			if (hayUsuarioInactivo) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tu cuenta está inactiva");
			}
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"El conjunto al que perteneces está inactivo");
		}

		if (activas.size() == 1) {
			Identidad identidad = activas.get(0);

			if ("PROPIETARIO_PENDIENTE".equals(identidad.getRol())) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN,
						"Tu cuenta está pendiente de aprobación por el administrador");
			}

			Tenant tenant = tenantRepository.findBySchemaName(identidad.getTenantId()).orElseThrow(
					() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Tenant no encontrado"));

			return generarLoginResponse(identidad, identidad.getTenantId(), tenant.getNombre());
		}

		List<MultiTenantLoginResponse.OpcionTenant> opciones = activas.stream().map(i -> {
			java.util.Optional<Tenant> tenant = tenantRepository.findBySchemaName(i.getTenantId());
			String nombre = tenant.map(Tenant::getNombre).orElse(i.getTenantId());
			String direccion = tenant.map(Tenant::getDireccion).orElse(null);
			return new MultiTenantLoginResponse.OpcionTenant(i.getTenantId(), nombre, direccion);
		}).toList();

		return new MultiTenantLoginResponse(true, opciones);
	}

	public LoginResponse seleccionarTenant(SeleccionTenantRequest request) {

		Identidad identidad = identidadRepository.findByEmailAndTenantId(request.email(), request.tenantId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas"));

		if (!passwordEncoder.matches(request.password(), identidad.getPassword())) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
		}

		if (!identidad.isActivo()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tu cuenta está inactiva");
		}

		if ("PROPIETARIO_PENDIENTE".equals(identidad.getRol())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tu cuenta está pendiente de aprobación");
		}

		Tenant tenant = tenantRepository.findBySchemaName(request.tenantId()).orElseThrow(
				() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Tenant no encontrado"));

		if (!tenant.isActivo()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN,
					"El conjunto al que perteneces está inactivo");
		}

		return generarLoginResponse(identidad, request.tenantId(), tenant.getNombre());
	}

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

		// 1) Identidad en el schema public (la entidad Identidad está fijada a public).
		Identidad identidad = new Identidad();
		identidad.setEmail(request.email());
		identidad.setPassword(passwordEncoder.encode(request.password()));
		identidad.setRol("PROPIETARIO_PENDIENTE");
		identidad.setTenantId(tenant.getSchemaName());
		identidad = identidadRepository.save(identidad);

		// 2) Usuario + propiedad en el schema del tenant. Se fija el TenantContext
		//    ANTES de invocar el método @Transactional para que la sesión de
		//    Hibernate enlace el search_path del conjunto; de lo contrario la
		//    propiedad (usuario_propiedades) terminaba guardándose en 'public' y
		//    quedaba invisible tras la aprobación.
		try {
			TenantContext.setTenant(tenant.getSchemaName());
			usuarioService.crearUsuarioPendienteDesdeRegistro(
					identidad.getId(), request.nombre(), request.telefono(), request.propiedadPath());
		} catch (RuntimeException e) {
			// Compensación: evita dejar una identidad huérfana en public si falla el paso 2.
			identidadRepository.delete(identidad);
			throw e;
		} finally {
			TenantContext.clear();
		}

		return new RegistroResponse(
				"Tu solicitud fue recibida. Un administrador debe aprobar tu cuenta antes de que puedas ingresar.",
				request.email(),
				tenant.getNombre()
		);
	}

	public List<TipoPropiedadNodoDto> getTiposPropiedad(String codigoConjunto) {
		Tenant tenant = tenantRepository.findByCodigo(codigoConjunto)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"No existe un conjunto con ese código"));

		if (!tenant.isActivo()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El conjunto no está activo");
		}

		try {
			TenantContext.setTenant(tenant.getSchemaName());
			return propiedadService.obtenerArbol();
		} finally {
			TenantContext.clear();
		}
	}

	/** Valores permitidos (híbrido) de un nivel, para los dropdowns del registro público. */
	public List<ValorTipoPropiedadDto> getValoresPropiedad(String codigoConjunto, Long tipoId, Long parentValorId) {
		Tenant tenant = tenantRepository.findByCodigo(codigoConjunto)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
						"No existe un conjunto con ese código"));

		if (!tenant.isActivo()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El conjunto no está activo");
		}

		try {
			TenantContext.setTenant(tenant.getSchemaName());
			return valorService.resolverPermitidos(tipoId, parentValorId);
		} finally {
			TenantContext.clear();
		}
	}

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

	// ─── Refresh token ───────────────────────────────────────────────────

	public RefreshResponse refresh(RefreshRequest request) {
		// Rotar el refresh token (valida + revoca el actual + crea uno nuevo)
		com.backendcr.residentialcomplex.entity.RefreshToken nuevoRt =
				refreshTokenService.rotar(request.refreshToken());

		// Recuperar la identidad para regenerar el access token con sus datos actuales
		Identidad identidad = identidadRepository.findById(nuevoRt.getIdentidadId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

		if (!identidad.isActivo()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tu cuenta está inactiva");
		}

		String tenantId = identidad.getTenantId() != null ? identidad.getTenantId() : "public";

		// Re-evaluar membresía del consejo para que el token refreshado refleje cambios
		boolean esConsejero = false;
		String cargoConsejo = null;
		if (!"public".equals(tenantId)) {
			try {
				TenantContext.setTenant(tenantId);
				Long usuarioId = usuarioRepository.findByIdentidadId(identidad.getId())
						.map(u -> u.getId()).orElse(null);
				if (usuarioId != null) {
					var membresia = miembroConsejoRepository.findByUsuarioIdAndActivoTrue(usuarioId);
					if (membresia.isPresent()) {
						esConsejero = true;
						cargoConsejo = membresia.get().getCargo().name();
					}
				}
			} finally {
				TenantContext.clear();
			}
		}

		String nuevoToken = jwtService.generarToken(
				identidad.getId(), identidad.getEmail(), identidad.getRol(), tenantId,
				esConsejero, cargoConsejo);

		return new RefreshResponse(nuevoToken, nuevoRt.getToken());
	}

	private LoginResponse generarLoginResponse(Identidad identidad, String tenantId, String nombreConjunto) {
		// Verificar membresía activa en el consejo (solo para tenants reales)
		boolean esConsejero = false;
		String cargoConsejo = null;
		if (tenantId != null && !"public".equals(tenantId)) {
			try {
				TenantContext.setTenant(tenantId);
				Long usuarioId = usuarioRepository.findByIdentidadId(identidad.getId())
						.map(u -> u.getId()).orElse(null);
				if (usuarioId != null) {
					var membresía = miembroConsejoRepository.findByUsuarioIdAndActivoTrue(usuarioId);
					if (membresía.isPresent()) {
						esConsejero = true;
						cargoConsejo = membresía.get().getCargo().name();
					}
				}
			} finally {
				TenantContext.clear();
			}
		}

		String token = jwtService.generarToken(
				identidad.getId(), identidad.getEmail(), identidad.getRol(), tenantId,
				esConsejero, cargoConsejo);
		String refreshToken = refreshTokenService.crear(identidad.getId()).getToken();
		String nombre = obtenerNombreDesdeSchema(tenantId, identidad.getId());
		String timezone = tenantRepository.findBySchemaName(tenantId)
				.map(t -> t.getTimezone() != null ? t.getTimezone() : "America/Bogota")
				.orElse(null); // null para SUPER_ADMIN (tenantId = "public")
		return new LoginResponse(token, refreshToken, identidad.getEmail(), identidad.getRol(),
				tenantId, nombreConjunto, nombre, timezone, esConsejero, cargoConsejo);
	}

}
