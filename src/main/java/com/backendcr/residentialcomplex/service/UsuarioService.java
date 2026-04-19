package com.backendcr.residentialcomplex.service;

import java.util.List;

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

    private final UsuarioRepository usuarioRepository;
    private final IdentidadRepository identidadRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioPropiedadRepository usuarioPropiedadRepository;
    private final PropiedadService propiedadService;

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
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
        return toResponse(usuario);
    }

    @Transactional
    public UsuarioResponse crear(CrearUsuarioRequest request) {
        String tenantId = TenantContext.getTenant();
        String emailNormalizado = request.email().trim().toLowerCase();

        if (identidadRepository.existsByEmailAndTenantId(emailNormalizado, tenantId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un usuario con ese correo en este conjunto");
        }

        EstadoUsuario estadoInicial = "RESIDENTE_PENDIENTE".equals(request.rol())
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
            Long propiedadId = propiedadService.resolverOCrearPath(request.propiedadPath());
            UsuarioPropiedad up = new UsuarioPropiedad();
            up.setUsuarioId(usuario.getId());
            up.setPropiedadId(propiedadId);
            up.setEsPrincipal(true);
            usuarioPropiedadRepository.save(up);
        }

        return UsuarioResponse.from(usuario, identidad.getEmail(), identidad.getRol());
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, ActualizarUsuarioRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        usuario.setNombre(request.nombre());
        usuario.setApto(request.apto());
        usuario.setTorre(request.torre());
        usuario.setTelefono(request.telefono());
        if (request.estado() != null) {
            usuario.setEstado(request.estado());
        }
        usuario = usuarioRepository.save(usuario);

        return toResponse(usuario);
    }

    @Transactional
    public UsuarioResponse aprobar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (usuario.getEstado() != EstadoUsuario.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se pueden aprobar usuarios con estado PENDIENTE");
        }

        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario = usuarioRepository.save(usuario);

        Identidad identidad = obtenerIdentidad(usuario.getIdentidadId());
        identidad.setRol("RESIDENTE");
        identidadRepository.save(identidad);

        return UsuarioResponse.from(usuario, identidad.getEmail(), identidad.getRol());
    }

    @Transactional
    public UsuarioResponse rechazar(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        if (usuario.getEstado() != EstadoUsuario.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se pueden rechazar usuarios con estado PENDIENTE");
        }

        usuario.setEstado(EstadoUsuario.RECHAZADO);
        usuario = usuarioRepository.save(usuario);

        Identidad identidad = obtenerIdentidad(usuario.getIdentidadId());
        return UsuarioResponse.from(usuario, identidad.getEmail(), identidad.getRol());
    }

    private UsuarioResponse toResponse(Usuario usuario) {
        Identidad identidad = obtenerIdentidad(usuario.getIdentidadId());
        return UsuarioResponse.from(usuario, identidad.getEmail(), identidad.getRol());
    }

    private Identidad obtenerIdentidad(Long identidadId) {
        return identidadRepository.findById(identidadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Identidad no encontrada: " + identidadId));
    }
}
