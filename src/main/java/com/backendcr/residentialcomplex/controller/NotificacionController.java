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
     * Elimina el FCM token al hacer logout.
     */
    @DeleteMapping("/token")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarToken(@AuthenticationPrincipal String email) {
        notificacionService.eliminarTokensDeUsuario(resolverUsuarioId(email));
    }

    /**
     * Diagnóstico: verifica si Firebase está inicializado y qué tokens hay en DB para el usuario.
     * Útil para depurar por qué no llegan notificaciones.
     * GET /api/notificaciones/diagnostico
     */
    @GetMapping("/diagnostico")
    public Map<String, Object> diagnostico(@AuthenticationPrincipal String email) {
        Long usuarioId = resolverUsuarioId(email);
        return notificacionService.diagnostico(usuarioId);
    }

    /**
     * Envía una notificación de prueba al usuario autenticado.
     * POST /api/notificaciones/test
     */
    @PostMapping("/test")
    public Map<String, String> test(@AuthenticationPrincipal String email) {
        Long usuarioId = resolverUsuarioId(email);
        notificacionService.enviarAUsuario(
            usuarioId,
            "🔔 Notificación de prueba",
            "Si ves esto, FCM está funcionando correctamente.",
            Map.of("tipo", "TEST")
        );
        return Map.of("mensaje", "Notificación enviada — revisa el dispositivo");
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
