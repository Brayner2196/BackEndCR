package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.repository.IdentidadRepository;
import com.backendcr.residentialcomplex.repository.UsuarioRepository;
import com.backendcr.residentialcomplex.service.MercadoPagoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
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
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MercadoPagoController {

    private final MercadoPagoService mercadoPagoService;
    private final IdentidadRepository identidadRepo;
    private final UsuarioRepository usuarioRepo;
    private final ObjectMapper objectMapper;

    // ─── Crear preferencia (residente autenticado) ────────────────────────────

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
     * MercadoPago envía notificaciones en dos formatos posibles:
     *
     * Formato v1 (query params):
     *   POST /webhook?id=12345&topic=payment
     *
     * Formato v2 (body JSON + query params):
     *   POST /webhook?type=payment&data.id=12345
     *   Body: {"action":"payment.updated","type":"payment","data":{"id":"12345"}, ...}
     *
     * Spring MVC puede fallar con @RequestParam("data.id") dependiendo de la configuración,
     * así que se lee el request manualmente para máxima compatibilidad.
     */
    @PostMapping("/api/mp/webhook")
    public ResponseEntity<Void> webhook(HttpServletRequest request) {
        try {
            String rawBody = StreamUtils.copyToString(request.getInputStream(), StandardCharsets.UTF_8);
            log.info("Webhook MP - queryString={} body={}", request.getQueryString(), rawBody);

            String paymentId = extraerPaymentId(request, rawBody);

            if (paymentId != null) {
                mercadoPagoService.procesarWebhook(paymentId);
            } else {
                log.info("Webhook MP sin paymentId identificable, ignorado");
            }
        } catch (Exception e) {
            // Nunca devolver error a MP; siempre responder 200
            log.error("Error procesando webhook MP: {}", e.getMessage());
        }

        return ResponseEntity.ok().build();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Extrae el paymentId del webhook en cualquiera de los formatos que MP puede enviar.
     * Estrategia: body JSON > query param "data.id" > query param "id" (topic=payment).
     */
    private String extraerPaymentId(HttpServletRequest request, String rawBody) {
        // 1. Intentar desde el body JSON: {"data": {"id": "..."}}
        if (rawBody != null && !rawBody.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(rawBody);
                String typeNode = root.path("type").asText(null);

                // Solo procesar notificaciones de tipo "payment"
                if (typeNode != null && !"payment".equals(typeNode)) {
                    log.info("Webhook MP tipo '{}' ignorado", typeNode);
                    return null;
                }

                JsonNode dataId = root.path("data").path("id");
                if (!dataId.isMissingNode() && !dataId.isNull()) {
                    return dataId.asText();
                }
            } catch (Exception e) {
                log.debug("Body no es JSON válido, continuando con query params");
            }
        }

        // 2. Query param "data.id" (formato v2)
        String dataId = request.getParameter("data.id");
        if (dataId != null && !dataId.isBlank()) return dataId;

        // 3. Query param "id" con topic=payment (formato v1)
        String topic = request.getParameter("topic");
        String id = request.getParameter("id");
        if ("payment".equals(topic) && id != null && !id.isBlank()) return id;

        return null;
    }

    private Long resolverUsuarioId(String email, String tenantId) {
        return identidadRepo.findByEmailAndTenantId(email, tenantId)
                .flatMap(i -> usuarioRepo.findByIdentidadId(i.getId()))
                .map(u -> u.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Usuario no encontrado para email=" + email + " tenant=" + tenantId));
    }
}
