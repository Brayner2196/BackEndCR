package com.backendcr.residentialcomplex.config;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.repository.IdentidadRepository;
import com.backendcr.residentialcomplex.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

/**
 * Utilidad centralizada para resolver el ID de usuario a partir del email
 * extraído del JWT. Elimina la duplicación del patrón resolverAdminId /
 * resolverResidenteId presente en múltiples controllers.
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final IdentidadRepository identidadRepo;
    private final UsuarioRepository usuarioRepo;

    /**
     * Resuelve el ID de usuario (en el schema del tenant activo) a partir del
     * email del principal autenticado.
     *
     * @param email email extraído de {@code @AuthenticationPrincipal}
     * @return ID del usuario en la tabla usuarios del tenant
     * @throws ResponseStatusException 401 si no se encuentra
     */
    public Long resolverUsuarioId(String email) {
        String tenantId = TenantContext.getTenant();
        return identidadRepo.findByEmailAndTenantId(email, tenantId)
                .flatMap(i -> usuarioRepo.findByIdentidadId(i.getId()))
                .map(u -> u.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Usuario no encontrado para email=" + email + " tenant=" + tenantId));
    }
}
