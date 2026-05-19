package com.backendcr.residentialcomplex.service;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.dto.usuario.ActualizarUsuarioRequest;
import com.backendcr.residentialcomplex.dto.usuario.CrearUsuarioRequest;
import com.backendcr.residentialcomplex.dto.usuario.UsuarioResponse;
import com.backendcr.residentialcomplex.entity.Identidad;
import com.backendcr.residentialcomplex.entity.Usuario;
import com.backendcr.residentialcomplex.entity.UsuarioPropiedad;
import com.backendcr.residentialcomplex.entity.enums.EstadoUsuario;
import com.backendcr.residentialcomplex.repository.IdentidadRepository;
import com.backendcr.residentialcomplex.repository.UsuarioPropiedadRepository;
import com.backendcr.residentialcomplex.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final Set<String> ROLES_VALIDOS = Set.of(
            "PROPIETARIO", "INQUILINO", "TENANT_ADMIN",
            "VIGILANTE", "PORTERO", "PISCINERO", "CONTADOR");

    private final UsuarioRepository usuarioRepository;
    private final IdentidadRepository identidadRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioPropiedadRepository usuarioPropiedadRepository;
    private final PropiedadService propiedadService;

    // ── Consultas ──────────────────────────────────────────────────────────────

    public List<UsuarioResponse> listarTodos() {
        return usuarioRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<UsuarioResponse> listarPendientes() {
        return usuarioRepository.findAllByEstado(EstadoUsuario.PENDIENTE).stream()
                .map(this::toResponse)
                .toList();
    }

    public UsuarioResponse buscarPorId(Long id) {
        Usuario usuario = obtenerUsuario(id);
        return toResponse(usuario);
    }

    // ── Crear ──────────────────────────────────────────────────────────────────

    @Transactional
    public UsuarioResponse crear(CrearUsuarioRequest request) {
        String tenantId = TenantContext.getTenant();
        String emailNormalizado = request.email().trim().toLowerCase();

        if (identidadRepository.existsByEmailAndTenantId(emailNormalizado, tenantId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un usuario con ese correo en este conjunto");
        }

        EstadoUsuario estadoInicial = "PROPIETARIO_PENDIENTE".equals(request.rol())
                ? EstadoUsuario.PENDIENTE
                : EstadoUsuario.ACTIVO;

        Identidad identidad = new Identidad();
        identidad.setEmail(emailNormalizado);
        identidad.setPassword(passwordEncoder.encode(request.password()));
        identidad.setRol(request.rol());
        identidad.setTenantId(tenantId);
        identidad = identidadRepository.save(identidad);

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre().trim());
        usuario.setIdentidadId(identidad.getId());
        usuario.setTelefono(request.telefono());
        usuario.setEstado(estadoInicial);
        usuario = usuarioRepository.save(usuario);

        if (request.propiedadPath() != null && !request.propiedadPath().isEmpty()) {
            Long propiedadId = "INQUILINO".equals(request.rol())
                    ? propiedadService.resolverPathSoloExistente(request.propiedadPath())
                    : propiedadService.resolverOCrearPath(request.propiedadPath());

            UsuarioPropiedad up = new UsuarioPropiedad();
            up.setUsuarioId(usuario.getId());
            up.setPropiedadId(propiedadId);
            up.setEsPrincipal(true);
            usuarioPropiedadRepository.save(up);
        }

        return toResponse(usuario, identidad);
    }

    // ── Actualizar datos personales ────────────────────────────────────────────

    @Transactional
    public UsuarioResponse actualizar(Long id, ActualizarUsuarioRequest request) {
        Usuario usuario = obtenerUsuario(id);

        usuario.setNombre(request.nombre());
        usuario.setTelefono(request.telefono());
        if (request.estado() != null) {
            usuario.setEstado(request.estado());
        }
        usuario = usuarioRepository.save(usuario);

        return toResponse(usuario);
    }

    // ── Flujo de aprobación ────────────────────────────────────────────────────

    @Transactional
    public UsuarioResponse aprobar(Long id, String rolDestino) {
        Usuario usuario = obtenerUsuario(id);

        if (usuario.getEstado() != EstadoUsuario.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se pueden aprobar usuarios con estado PENDIENTE");
        }

        if (!"PROPIETARIO".equals(rolDestino) && !"INQUILINO".equals(rolDestino)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "rolDestino debe ser PROPIETARIO o INQUILINO");
        }

        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario = usuarioRepository.save(usuario);

        Identidad identidad = obtenerIdentidad(usuario.getIdentidadId());
        identidad.setRol(rolDestino);
        identidadRepository.save(identidad);

        return toResponse(usuario, identidad);
    }

    @Transactional
    public UsuarioResponse rechazar(Long id) {
        Usuario usuario = obtenerUsuario(id);

        if (usuario.getEstado() != EstadoUsuario.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se pueden rechazar usuarios con estado PENDIENTE");
        }

        usuario.setEstado(EstadoUsuario.RECHAZADO);
        usuario = usuarioRepository.save(usuario);

        Identidad identidad = obtenerIdentidad(usuario.getIdentidadId());
        return toResponse(usuario, identidad);
    }

    // ── Activar / Desactivar acceso ────────────────────────────────────────────

    @Transactional
    public UsuarioResponse activar(Long id, String emailAdmin) {
        Usuario usuario = obtenerUsuario(id);
        Identidad identidad = obtenerIdentidad(usuario.getIdentidadId());

        // No permitir activar/desactivar al propio TENANT_ADMIN que hace la petición
        if (identidad.getEmail().equalsIgnoreCase(emailAdmin)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No puedes modificar tu propio acceso");
        }

        identidad.setActivo(true);
        identidadRepository.save(identidad);

        return toResponse(usuario, identidad);
    }

    @Transactional
    public UsuarioResponse desactivar(Long id, String emailAdmin) {
        Usuario usuario = obtenerUsuario(id);
        Identidad identidad = obtenerIdentidad(usuario.getIdentidadId());

        if (identidad.getEmail().equalsIgnoreCase(emailAdmin)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No puedes desactivar tu propio acceso");
        }

        identidad.setActivo(false);
        identidadRepository.save(identidad);

        return toResponse(usuario, identidad);
    }

    // ── Cambiar rol ────────────────────────────────────────────────────────────

    @Transactional
    public UsuarioResponse cambiarRol(Long id, String nuevoRol, String emailAdmin) {
        if (!ROLES_VALIDOS.contains(nuevoRol)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Rol no válido: " + nuevoRol + ". Opciones: " + ROLES_VALIDOS);
        }

        Usuario usuario = obtenerUsuario(id);
        Identidad identidad = obtenerIdentidad(usuario.getIdentidadId());

        if (identidad.getEmail().equalsIgnoreCase(emailAdmin)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No puedes cambiar tu propio rol");
        }

        identidad.setRol(nuevoRol);
        identidadRepository.save(identidad);

        return toResponse(usuario, identidad);
    }

    // ── Helpers privados ───────────────────────────────────────────────────────

    private UsuarioResponse toResponse(Usuario usuario) {
        Identidad identidad = obtenerIdentidad(usuario.getIdentidadId());
        return toResponse(usuario, identidad);
    }

    private UsuarioResponse toResponse(Usuario usuario, Identidad identidad) {
        return UsuarioResponse.from(usuario, identidad.getEmail(), identidad.getRol(), identidad.isActivo());
    }

    private Usuario obtenerUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private Identidad obtenerIdentidad(Long identidadId) {
        return identidadRepository.findById(identidadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Identidad no encontrada: " + identidadId));
    }
}
