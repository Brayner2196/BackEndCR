package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.dto.pasarela.*;
import com.backendcr.residentialcomplex.entity.enums.TipoPasarela;
import com.backendcr.residentialcomplex.repository.IdentidadRepository;
import com.backendcr.residentialcomplex.repository.UsuarioRepository;
import com.backendcr.residentialcomplex.service.pasarela.PasarelaFactory;
import com.backendcr.residentialcomplex.service.pasarela.PasarelaOrchestrator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Controller unificado de pasarelas de pago.
 *
 * Endpoints residente:
 *  GET  /api/residente/pago/pasarelas          → pasarelas disponibles del tenant
 *  POST /api/residente/pago/checkout/{cobroId} → crea checkout en la pasarela elegida
 *
 * Endpoints webhook (públicos):
 *  POST /api/pago/webhook/mp/{tenantSchema} → webhook MercadoPago por-tenant (URL notificación)
 *  POST /api/pago/webhook/mp                → webhook MP legacy (sin config de tenant)
 *  POST /api/pago/webhook/wompi             → webhook Wompi
 *  POST /api/pago/webhook/bold              → webhook Bold
 *
 * Confirmación desde app (autenticado):
 *  POST /api/pago/confirmar/mp/{paymentId} → confirmar pago MP desde WebView Flutter
 *
 * Endpoints admin (TENANT_ADMIN o SUPER_ADMIN):
 *  GET    /api/admin/pasarelas                 → listar pasarelas del tenant
 *  POST   /api/admin/pasarelas                 → crear o actualizar pasarela
 *  PATCH  /api/admin/pasarelas/{id}/toggle     → activar/desactivar
 *  DELETE /api/admin/pasarelas/{id}            → eliminar
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class PasarelaController {

    private final PasarelaOrchestrator orchestrator;
    private final PasarelaFactory factory;
    private final IdentidadRepository identidadRepo;
    private final UsuarioRepository usuarioRepo;
    private final ObjectMapper objectMapper;
    private final com.backendcr.residentialcomplex.tenant.repository.TenantRepository tenantRepo;

    // ═════════════════════════════════════════════════════════════════════════
    // RESIDENTE — Pasarelas disponibles
    // ═════════════════════════════════════════════════════════════════════════

    @GetMapping("/api/residente/pago/pasarelas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PasarelaDisponibleResponse>> obtenerDisponibles() {
        String tenantSchema = TenantContext.getTenant();
        return ResponseEntity.ok(orchestrator.obtenerDisponibles(tenantSchema));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // RESIDENTE — Crear checkout
    // ═════════════════════════════════════════════════════════════════════════

    @PostMapping("/api/residente/pago/checkout/{cobroId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CheckoutResponse> crearCheckout(
            @PathVariable Long cobroId,
            @Valid @RequestBody CheckoutRequest request,
            @AuthenticationPrincipal String email) {

        String tenantSchema = TenantContext.getTenant();
        Long usuarioId = resolverUsuarioId(email, tenantSchema);

        CheckoutResponse response = orchestrator.crearCheckout(
                tenantSchema,
                cobroId,
                usuarioId,
                request.pasarela(),
                request.monto()
        );
        return ResponseEntity.ok(response);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // WEBHOOKS PÚBLICOS
    // ═════════════════════════════════════════════════════════════════════════

    /**
     * Webhook MercadoPago POR TENANT.
     * La URL de notificación se configura en MercadoPagoServiceImpl como:
     * {appBaseUrl}/api/pago/webhook/mp/{tenantSchema}
     * Esto permite resolver la config del tenant antes de llamar a la API de MP.
     */
    @PostMapping("/api/pago/webhook/mp/{tenantSchema}")
    public ResponseEntity<Void> webhookMercadoPagoTenant(
            @PathVariable String tenantSchema,
            HttpServletRequest request) {
        try {
            String rawBody = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
            log.info("Webhook MP tenant={} - queryString={} body={}", tenantSchema, request.getQueryString(), rawBody);

            String paymentId = extraerPaymentIdMP(request, rawBody);
            if (paymentId != null) {
                var config = obtenerConfigONull(tenantSchema, TipoPasarela.MERCADO_PAGO);
                factory.getServicio(TipoPasarela.MERCADO_PAGO)
                        .procesarWebhook(config, paymentId, null);
            }
        } catch (Exception e) {
            log.error("Error procesando webhook MP tenant {}: {}", tenantSchema, e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Webhook MercadoPago global (legacy / sin tenant en URL).
     * Ya no tiene config de tenant, así que procesarWebhook lo ignorará con un warning.
     * Se mantiene para compatibilidad con preferencias antiguas.
     */
    @PostMapping("/api/pago/webhook/mp")
    public ResponseEntity<Void> webhookMercadoPago(HttpServletRequest request) {
        try {
            String rawBody = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
            log.info("Webhook MP global - queryString={} body={}", request.getQueryString(), rawBody);

            String paymentId = extraerPaymentIdMP(request, rawBody);
            if (paymentId != null) {
                factory.getServicio(TipoPasarela.MERCADO_PAGO)
                        .procesarWebhook(null, paymentId, null);
            }
        } catch (Exception e) {
            log.error("Error procesando webhook MP: {}", e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Confirmación desde la app Flutter al interceptar la back_url de éxito/pendiente.
     * Requiere autenticación para poder resolver el tenant del JWT y buscar la config en BD.
     * Es idempotente: si el webhook ya procesó el pago, el backend lo ignora.
     */
    @PostMapping("/api/pago/confirmar/mp/{paymentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> confirmarMercadoPago(@PathVariable String paymentId) {
        String tenantSchema = TenantContext.getTenant();
        var config = obtenerConfigONull(tenantSchema, TipoPasarela.MERCADO_PAGO);
        factory.getServicio(TipoPasarela.MERCADO_PAGO)
                .procesarWebhook(config, paymentId, null);
        return ResponseEntity.ok().build();
    }

    /** Webhook Wompi */
    @PostMapping("/api/pago/webhook/wompi")
    public ResponseEntity<Void> webhookWompi(
            HttpServletRequest request,
            @RequestHeader(value = "X-Event-Checksum", required = false) String signature) {
        try {
            String payload = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
            log.info("Webhook Wompi recibido, tamaño={}", payload.length());

            // El tenant se extrae del campo reference dentro del payload
            // La implementación de WompiServiceImpl lo maneja internamente
            // Para obtener la config del tenant necesitamos extraer tenantId primero
            String tenantId = extraerTenantIdDePayload(payload, "reference");
            var config = tenantId != null
                    ? factory.getServicio(TipoPasarela.WOMPI) != null
                        ? obtenerConfigONull(tenantId, TipoPasarela.WOMPI)
                        : null
                    : null;

            factory.getServicio(TipoPasarela.WOMPI)
                    .procesarWebhook(config, payload, signature);
        } catch (Exception e) {
            log.error("Error procesando webhook Wompi: {}", e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    /** Webhook Bold */
    @PostMapping("/api/pago/webhook/bold")
    public ResponseEntity<Void> webhookBold(
            HttpServletRequest request,
            @RequestHeader(value = "bold-signature", required = false) String signature) {
        try {
            String payload = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
            log.info("Webhook Bold recibido, tamaño={}", payload.length());

            String tenantId = extraerTenantIdDePayload(payload, "order_id");
            var config = tenantId != null
                    ? obtenerConfigONull(tenantId, TipoPasarela.BOLD)
                    : null;

            factory.getServicio(TipoPasarela.BOLD)
                    .procesarWebhook(config, payload, signature);
        } catch (Exception e) {
            log.error("Error procesando webhook Bold: {}", e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ADMIN — Gestión de pasarelas
    // ═════════════════════════════════════════════════════════════════════════

    /** Lista las pasarelas del tenant autenticado */
    @GetMapping("/api/admin/pasarelas")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<List<PasarelaConfigResponse>> listarPasarelas(
            @RequestParam(required = false) Long tenantId) {

        Long tid = resolverTenantId(tenantId);
        return ResponseEntity.ok(orchestrator.listarPasarelas(tid));
    }

    /** Crea o actualiza la configuración de una pasarela para el tenant */
    @PostMapping("/api/admin/pasarelas")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public PasarelaConfigResponse crearOActualizarPasarela(
            @Valid @RequestBody PasarelaConfigRequest request,
            @RequestParam(required = false) Long tenantId) {

        Long tid = resolverTenantId(tenantId);
        return orchestrator.crearOActualizarPasarela(tid, request);
    }

    /** Activa o desactiva una pasarela */
    @PatchMapping("/api/admin/pasarelas/{id}/toggle")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<Void> toggleActiva(
            @PathVariable Long id,
            @RequestParam boolean activa) {
        orchestrator.toggleActiva(id, activa);
        return ResponseEntity.noContent().build();
    }

    /** Elimina una pasarela */
    @DeleteMapping("/api/admin/pasarelas/{id}")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarPasarela(@PathVariable Long id) {
        orchestrator.eliminarPasarela(id);
    }

    // ═════════════════════════════════════════════════════════════════════════
    // Helpers
    // ═════════════════════════════════════════════════════════════════════════

    private Long resolverUsuarioId(String email, String tenantId) {
        return identidadRepo.findByEmailAndTenantId(email, tenantId)
                .flatMap(i -> usuarioRepo.findByIdentidadId(i.getId()))
                .map(u -> u.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Usuario no encontrado para email=" + email));
    }

    /**
     * Para TENANT_ADMIN → resuelve el ID a partir del schema en TenantContext.
     * Para SUPER_ADMIN  → puede pasar tenantId explícito como query param.
     */
    private Long resolverTenantId(Long tenantIdParam) {
        if (tenantIdParam != null) return tenantIdParam;
        String schema = TenantContext.getTenant();
        if (schema == null || schema.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo determinar el tenant");
        }
        return tenantRepo.findBySchemaName(schema)
                .map(t -> t.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Tenant no encontrado para schema: " + schema));
    }

    private com.backendcr.residentialcomplex.entity.TenantPasarela obtenerConfigONull(
            String tenantSchema, TipoPasarela tipo) {
        try {
            return factory.getConfigTenant(tenantSchema, tipo);
        } catch (Exception e) {
            return null;
        }
    }

    private String extraerTenantIdDePayload(String payload, String referenceField) {
        try {
            var root = objectMapper.readTree(payload);
            // Intenta encontrar el campo de referencia en diferentes niveles del JSON
            String ref = root.path("data").path("transaction").path(referenceField).asText(null);
            if (ref == null) ref = root.path("data").path("payment").path(referenceField).asText(null);
            if (ref == null) ref = root.path(referenceField).asText(null);
            if (ref != null && ref.contains("|")) {
                return ref.split("\\|")[0];
            }
        } catch (Exception e) {
            log.debug("No se pudo extraer tenantId del payload: {}", e.getMessage());
        }
        return null;
    }

    private String extraerPaymentIdMP(HttpServletRequest request, String rawBody) {
        // Body JSON: {"data": {"id": "..."}}
        if (rawBody != null && !rawBody.isBlank()) {
            try {
                var root = objectMapper.readTree(rawBody);
                String typeNode = root.path("type").asText(null);
                if (typeNode != null && !"payment".equals(typeNode)) return null;
                var dataId = root.path("data").path("id");
                if (!dataId.isMissingNode() && !dataId.isNull()) return dataId.asText();
            } catch (Exception ignored) {}
        }
        // Query param "data.id"
        String dataId = request.getParameter("data.id");
        if (dataId != null && !dataId.isBlank()) return dataId;
        // Query param "id" con topic=payment
        String topic = request.getParameter("topic");
        String id    = request.getParameter("id");
        if ("payment".equals(topic) && id != null && !id.isBlank()) return id;
        return null;
    }
}
