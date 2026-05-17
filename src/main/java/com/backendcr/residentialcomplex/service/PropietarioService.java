package com.backendcr.residentialcomplex.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.dto.inquilino.CrearInquilinoRequest;
import com.backendcr.residentialcomplex.dto.usuario.UsuarioResponse;
import com.backendcr.residentialcomplex.entity.Identidad;
import com.backendcr.residentialcomplex.entity.Usuario;
import com.backendcr.residentialcomplex.entity.enums.EstadoUsuario;
import com.backendcr.residentialcomplex.repository.IdentidadRepository;
import com.backendcr.residentialcomplex.repository.UsuarioPropiedadRepository;
import com.backendcr.residentialcomplex.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PropietarioService {

    private final IdentidadRepository identidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioPropiedadRepository usuarioPropiedadRepository;
    private final PasswordEncoder passwordEncoder;

    /** Lista los inquilinos que pertenecen a la misma unidad (apto+torre) del propietario. */
    public List<UsuarioResponse> listarInquilinos() {
        Usuario propietario = obtenerPropietarioActual();

        if (propietario.getApto() == null || propietario.getTorre() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Tu unidad no tiene apto o torre registrados");
        }

        List<Usuario> mismoUnidad = usuarioRepository.findByAptoAndTorre(
                propietario.getApto(), propietario.getTorre());

        return mismoUnidad.stream()
                .filter(u -> !u.getId().equals(propietario.getId()))
                .map(u -> {
                    Identidad id = identidadRepository.findById(u.getIdentidadId()).orElse(null);
                    return (id != null && "INQUILINO".equals(id.getRol()))
                            ? UsuarioResponse.from(u, id.getEmail(), id.getRol())
                            : null;
                })
                .filter(r -> r != null)
                .toList();
    }

    /** Crea un inquilino con la misma unidad del propietario. */
    @Transactional
    public UsuarioResponse crearInquilino(CrearInquilinoRequest request) {
        Usuario propietario = obtenerPropietarioActual();
        String tenantId = TenantContext.getTenant();
        String emailNormalizado = request.email().trim().toLowerCase();

        if (propietario.getApto() == null || propietario.getTorre() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Tu unidad no tiene apto o torre registrados");
        }

        if (identidadRepository.existsByEmailAndTenantId(emailNormalizado, tenantId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un usuario con ese correo en este conjunto");
        }

        Identidad identidad = new Identidad();
        identidad.setEmail(emailNormalizado);
        identidad.setPassword(passwordEncoder.encode(request.password()));
        identidad.setRol("INQUILINO");
        identidad.setTenantId(tenantId);
        identidad = identidadRepository.save(identidad);

        Usuario inquilino = new Usuario();
        inquilino.setNombre(request.nombre().trim());
        inquilino.setIdentidadId(identidad.getId());
        inquilino.setApto(propietario.getApto());
        inquilino.setTorre(propietario.getTorre());
        inquilino.setTelefono(request.telefono());
        inquilino.setEstado(EstadoUsuario.ACTIVO);
        inquilino = usuarioRepository.save(inquilino);

        return UsuarioResponse.from(inquilino, identidad.getEmail(), identidad.getRol());
    }

    /** Elimina un inquilino validando que pertenezca a la misma unidad del propietario. */
    @Transactional
    public void eliminarInquilino(Long inquilinoId) {
        Usuario propietario = obtenerPropietarioActual();

        Usuario inquilino = usuarioRepository.findById(inquilinoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Inquilino no encontrado"));

        Identidad identidad = identidadRepository.findById(inquilino.getIdentidadId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Identidad no encontrada"));

        if (!"INQUILINO".equals(identidad.getRol())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El usuario no es un inquilino");
        }

        if (!propietario.getApto().equals(inquilino.getApto())
                || !propietario.getTorre().equals(inquilino.getTorre())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No tienes permiso para eliminar este inquilino");
        }

        usuarioPropiedadRepository.findByUsuarioId(inquilinoId)
                .forEach(up -> usuarioPropiedadRepository.deleteById(up.getId()));

        usuarioRepository.deleteById(inquilinoId);
        identidadRepository.deleteById(identidad.getId());
    }

    // ---------------------------------------------------------------------------

    private Usuario obtenerPropietarioActual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        String tenantId = TenantContext.getTenant();

        Identidad identidad = identidadRepository.findByEmailAndTenantId(email, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión no válida"));

        return usuarioRepository.findByIdentidadId(identidad.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró el perfil del propietario"));
    }
}
