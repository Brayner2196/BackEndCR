package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.repository.IdentidadRepository;
import com.backendcr.residentialcomplex.repository.UsuarioRepository;
import com.backendcr.residentialcomplex.service.MercadoPagoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MercadoPagoController {

    private final MercadoPagoService mercadoPagoService;
    private final IdentidadRepository identidadRepo;
    private final UsuarioRepository usuarioRepo;

    // ─── Crear preferencia (residente autenticado) ────────────────────────────

    /**
     * El residente llama este endpoint para iniciar el pago de un cobro con MercadoPago.
     * Devuelve la URL del checkout de MercadoPago.
     */
    @PostMapping("/api/residente/mp/preferencia/{cobroId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> crearPreferencia(
            @PathVariable Long cobroId,
            @AuthenticationPrincipal String email) {

        String tenantId = TenantContext.getTenant();
        Long usuarioId = resolverUsuarioId(email, tenantId);

        String checkoutUrl = mercadoPagoService.crearPreferencia(cobroId, usuarioId, tenantId);
        return ResponseEntity.ok(Map.of("checkoutUrl", checkoutUrl));
    }

    // ─── Webhook de MercadoPago (público, sin auth) ───────────────────────────

    /**
     * MercadoPago llama este endpoint cuando el estado de un pago cambia.
     * Es público (no requiere JWT ni X-Tenant-ID) porque viene desde los servidores de MP.
     *
     * MP envía el tipo de notificación y el id del recurso como query params.
     * Documentación: https://www.mercadopago.com.co/developers/es/docs/your-integrations/notifications/webhooks
     */
    @PostMapping("/api/mp/webhook")
    public ResponseEntity<Void> webhook(
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "data.id", required = false) String dataId,
            @RequestBody(required = false) Map<String, Object> body) {

        log.info("Webhook MP recibido - type={} data.id={} body={}", type, dataId, body);

        // MP envía notificaciones de distintos tipos; solo nos interesan los pagos
        String paymentId = dataId;

        // Algunos sistemas MP envían el id dentro del body como data.id
        if (paymentId == null && body != null) {
            Object data = body.get("data");
            if (data instanceof Map<?, ?> dataMap) {
                Object id = dataMap.get("id");
                if (id != null) paymentId = id.toString();
            }
        }

        if ("payment".equals(type) || paymentId != null) {
            mercadoPagoService.procesarWebhook(paymentId);
        } else {
            log.info("Notificación MP ignorada - type={}", type);
        }

        // MP espera siempre 200, aunque falle internamente
        return ResponseEntity.ok().build();
    }

    // ─── Helper ───────────────────────────────────────────────────────────────

    private Long resolverUsuarioId(String email, String tenantId) {
        return identidadRepo.findByEmailAndTenantId(email, tenantId)
                .flatMap(i -> usuarioRepo.findByIdentidadId(i.getId()))
                .map(u -> u.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Usuario no encontrado para email=" + email + " tenant=" + tenantId));
    }
}
