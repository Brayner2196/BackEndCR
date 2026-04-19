package com.backendcr.residentialcomplex.auth;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.dto.propiedad.TipoPropiedadNodoDto;
import com.backendcr.residentialcomplex.entity.Identidad;
import com.backendcr.residentialcomplex.entity.Tenant;
import com.backendcr.residentialcomplex.entity.UsuarioPropiedad;
import com.backendcr.residentialcomplex.repository.IdentidadRepository;
import com.backendcr.residentialcomplex.repository.UsuarioPropiedadRepository;
import com.backendcr.residentialcomplex.service.PropiedadService;
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
	private final PropiedadService propiedadService;
	private final UsuarioPropiedadRepository usuarioPropiedadRepository;

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
			return generarLoginResponse(superAdmin, "public", null);
		}

		if (identidades.size() == 1) {
			Identidad identidad = identidades.get(0);

			if ("RESIDENTE_PENDIENTE".equals(identidad.getRol())) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN,
						"Tu cuenta está pendiente de aprobación por el administrador");
			}

			Tenant tenant = tenantRepository.findBySchemaName(identidad.getTenantId()).orElseThrow(
					() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Tenant no encontrado"));

			return generarLoginResponse(identidad, identidad.getTenantId(), tenant.getNombre());
		}

		List<MultiTenantLoginResponse.OpcionTenant> opciones = identidades.stream().map(i -> {
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

		Identidad identidad = new Identidad();
		identidad.setEmail(request.email());
		identidad.setPassword(passwordEncoder.encode(request.password()));
		identidad.setRol("RESIDENTE_PENDIENTE");
		identidad.setTenantId(tenant.getSchemaName());
		identidad = identidadRepository.save(identidad);

		jdbcTemplate.update("""
				INSERT INTO %s.usuarios (nombre, identidad_id, telefono, estado)
				VALUES (?, ?, ?, 'PENDIENTE')
				""".formatted(tenant.getSchemaName()),
				request.nombre(), identidad.getId(), request.telefono());

		if (request.propiedadPath() != null && !request.propiedadPath().isEmpty()) {
			try {
				TenantContext.setTenant(tenant.getSchemaName());
				Long usuarioId = jdbcTemplate.queryForObject(
						"SELECT id FROM " + tenant.getSchemaName() + ".usuarios WHERE identidad_id = ?",
						Long.class, identidad.getId());

				Long propiedadId = propiedadService.resolverOCrearPath(request.propiedadPath());

				UsuarioPropiedad up = new UsuarioPropiedad();
				up.setUsuarioId(usuarioId);
				up.setPropiedadId(propiedadId);
				up.setEsPrincipal(true);
				usuarioPropiedadRepository.save(up);
			} finally {
				TenantContext.clear();
			}
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
