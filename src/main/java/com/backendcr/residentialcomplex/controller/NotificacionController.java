package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.dto.FcmTokenRequest;
import com.backendcr.residentialcomplex.repository.IdentidadRepository;
import com.backendcr.residentialcomplex.repository.UsuarioRepository;
import com.backendcr.residentialcomplex.service.NotificacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/notificaciones")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;
    private final IdentidadRepository identidadRepo;
    private final UsuarioRepository usuarioRepo;

    /**
     * Registra o actualiza el FCM token del usuario autenticado.
     * Flutter lo llama al iniciar sesión (y cuando el token se renueva).
     */
    @PostMapping("/token")
    public Map<String, String> registrarToken(
        @AuthenticationPrincipal String email,
        @Valid @RequestBody FcmTokenRequest request
    ) {
        Long usuarioId = resolverUsuarioId(email);
        String tenantId = TenantContext.getTenant();
        notificacionService.registrarToken(usuarioId, tenantId, request);
        return Map.of("mensaje", "Token registrado correctamente");
    }

    /**
     * Elimina el FCM token al hacer logout (evita recibir notificaciones en sesiones cerradas).
     */
    @DeleteMapping("/token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarToken(@AuthenticationPrincipal String email) {
        Long usuarioId = resolverUsuarioId(email);
        notificacionService.eliminarTokensDeUsuario(usuarioId);
    }

    // ── Helper ────────────────────────────────────────────

    private Long resolverUsuarioId(String email) {
        String tenantId = TenantContext.getTenant();
        return identidadRepo.findByEmailAndTenantId(email, tenantId)
            .flatMap(i -> usuarioRepo.findByIdentidadId(i.getId()))
            .map(u -> u.getId())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Usuario no encontrado para email=" + email));
    }
}
