package com.backendcr.residentialcomplex.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.dto.inquilino.PermisoInquilinoDto;
import com.backendcr.residentialcomplex.entity.Identidad;
import com.backendcr.residentialcomplex.entity.Usuario;
import com.backendcr.residentialcomplex.repository.IdentidadRepository;
import com.backendcr.residentialcomplex.repository.InquilinoPermisoRepository;
import com.backendcr.residentialcomplex.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * Endpoints accesibles por el rol INQUILINO para consultar su propia información.
 */
@RestController
@RequestMapping("/api/inquilino")
@PreAuthorize("hasRole('INQUILINO')")
@RequiredArgsConstructor
public class InquilinoController {

    private final IdentidadRepository identidadRepository;
    private final UsuarioRepository usuarioRepository;
    private final InquilinoPermisoRepository inquilinoPermisoRepository;

    /**
     * Retorna los permisos que el propietario ha otorgado al inquilino autenticado.
     */
    @GetMapping("/mis-permisos")
    public PermisoInquilinoDto misPermisos() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        String tenantId = TenantContext.getTenant();

        Identidad identidad = identidadRepository.findByEmailAndTenantId(email, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sesión no válida"));

        Usuario usuario = usuarioRepository.findByIdentidadId(identidad.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No se encontró el perfil del inquilino"));

        List<String> permisos = inquilinoPermisoRepository.findAllByInquilinoId(usuario.getId())
                .stream()
                .map(p -> p.getPermiso().name())
                .toList();

        return new PermisoInquilinoDto(permisos);
    }
}
