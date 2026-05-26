package com.backendcr.residentialcomplex.service.pasarela;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.dto.pasarela.CheckoutResponse;
import com.backendcr.residentialcomplex.entity.Cobro;
import com.backendcr.residentialcomplex.entity.TenantPasarela;
import com.backendcr.residentialcomplex.entity.enums.EstadoCobro;
import com.backendcr.residentialcomplex.entity.enums.EstadoPago;
import com.backendcr.residentialcomplex.entity.enums.MetodoPago;
import com.backendcr.residentialcomplex.entity.enums.TipoPasarela;
import com.backendcr.residentialcomplex.repository.CobroRepository;
import com.backendcr.residentialcomplex.repository.PagoRepository;
import com.backendcr.residentialcomplex.service.PagoService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;

/**
 * Implementación de Wompi para la interfaz PasarelaService.
 *
 * Wompi API v1: https://docs.wompi.co
 *  - Para crear un link de pago: POST /v1/payment_links
 *  - Webhook: verifica firma HMAC-SHA256 con el events_secret del tenant
 *
 * Estrategia de referencia:
 *  Wompi no preserva el campo `reference` personalizado que enviamos al crear el link;
 *  lo sobreescribe con uno auto-generado ({linkId}_{timestamp}_{random}).
 *  La solución es encodear los datos del cobro como query param `ref` en el redirect_url.
 *  Wompi conserva el redirect_url intacto y lo retorna tanto en el webhook como
 *  en GET /transactions/{id}, por lo que podemos leerlo desde ambos flujos.
 *
 *  Formato: redirect_url?ref=tenantId__cobroId__usuarioId
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WompiServiceImpl implements PasarelaService {

    private static final String WOMPI_API_URL  = "https://sandbox.wompi.co/v1";
    private static final String WOMPI_PROD_URL = "https://production.wompi.co/v1";
    private static final String WOMPI_CHECKOUT_BASE = "https://checkout.wompi.co/l/";

    /** Separador interno para el query param ref (los IDs nunca contienen __) */
    private static final String SEP = "__";

    @Value("${app.base-url:https}")
    private String appBaseUrl;

    private final CobroRepository cobroRepo;
    private final PagoRepository pagoRepo;
    private final PagoService pagoService;
    private final ObjectMapper objectMapper;

    @Override
    public TipoPasarela getTipo() {
        return TipoPasarela.WOMPI;
    }

    // ─── Crear Checkout ──────────────────────────────────────────────────────

    @Override
    public CheckoutResponse crearCheckout(
            TenantPasarela config,
            Long cobroId,
            Long usuarioId,
            String tenantId,
            BigDecimal montoPersonalizado) {

        validarConfig(config);

        Cobro cobro = cobroRepo.findById(cobroId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cobro no encontrado"));

        if (cobro.getEstado() == EstadoCobro.PAGADO || cobro.getEstado() == EstadoCobro.EXONERADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este cobro ya está " + cobro.getEstado());
        }

        // Limpiar pago Wompi abandonado si existe
        pagoRepo.findByCobroIdAndEstado(cobroId, EstadoPago.PENDIENTE_VERIFICACION)
                .ifPresent(pagoExistente -> {
                    if (pagoExistente.getMetodoPago() == MetodoPago.WOMPI) {
                        log.info("Eliminando pago Wompi abandonado {} para cobro {}", pagoExistente.getId(), cobroId);
                        pagoRepo.delete(pagoExistente);
                    } else {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "Ya existe un comprobante de pago pendiente para este cobro");
                    }
                });

        BigDecimal monto = (montoPersonalizado != null && montoPersonalizado.compareTo(BigDecimal.ZERO) > 0)
                ? montoPersonalizado
                : cobro.getMontoPendiente();

        long montoCentavos = monto.multiply(BigDecimal.valueOf(100)).longValue();

        String baseUrl = config.isSandbox() ? WOMPI_API_URL : WOMPI_PROD_URL;

        // Encodear los datos del cobro en el redirect_url.
        // Wompi conserva este campo intacto en el webhook y en GET /transactions/{id},
        // lo que nos permite recuperar tenantId/cobroId/usuarioId sin tabla extra.
        String ref        = tenantId + SEP + cobroId + SEP + usuarioId;
        String baseRedirect = resolverUrl(config.getSuccessUrl(), appBaseUrl + "/api/mp/pago-exito");
        String redirect   = baseRedirect + (baseRedirect.contains("?") ? "&" : "?") + "ref=" + ref;

        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "name",             "Pago cobro #" + cobroId,
                    "description",      "Cuota conjuntos residenciales - tenant " + tenantId,
                    "single_use",       true,
                    "collect_shipping", false,
                    "currency",         "COP",
                    "amount_in_cents",  montoCentavos,
                    "redirect_url",     redirect,
                    "reference",        ref          // solo informativo en dashboard Wompi
            ));

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/payment_links"))
                    .header("Authorization", "Bearer " + config.getPrivateKey())
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("Wompi error {}: {}", response.statusCode(), response.body());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error al crear link de pago en Wompi: " + response.statusCode());
            }

            JsonNode json = objectMapper.readTree(response.body());
            log.debug("Wompi response body: {}", response.body());

            String linkId = json.path("data").path("id").asText("");
            if (linkId.isBlank()) {
                log.error("Wompi body sin id de link: {}", response.body());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Wompi no devolvió URL de checkout");
            }

            String checkoutUrl = WOMPI_CHECKOUT_BASE + linkId;
            log.info("Wompi link creado para cobro {} tenant {}: {} — redirect_url={}", cobroId, tenantId, checkoutUrl, redirect);
            return new CheckoutResponse(checkoutUrl, TipoPasarela.WOMPI);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creando link Wompi: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al crear link de pago Wompi: " + e.getMessage());
        }
    }

    // ─── Procesar Webhook ────────────────────────────────────────────────────

    /**
     * Wompi envía eventos firmados con HMAC-SHA256 usando el events_secret.
     * Header: X-Event-Checksum
     * Formato: payload JSON con "event" y "data.transaction"
     *
     * La referencia se extrae del campo redirect_url de la transacción,
     * donde guardamos ?ref=tenantId__cobroId__usuarioId al crear el link.
     */
    @Override
    public void procesarWebhook(TenantPasarela config, String payload, String signature) {
        if (payload == null || payload.isBlank()) {
            log.warn("Webhook Wompi sin payload");
            return;
        }

        try {
            if (config != null && config.getWebhookSecret() != null && !config.getWebhookSecret().isBlank()) {
                if (!verificarFirmaWompi(payload, signature, config.getWebhookSecret())) {
                    log.warn("Firma Wompi inválida — payload rechazado");
                    return;
                }
            }

            JsonNode root  = objectMapper.readTree(payload);
            String event   = root.path("event").asText("");
            JsonNode data  = root.path("data").path("transaction");

            if (!"transaction.updated".equals(event)) {
                log.info("Evento Wompi '{}' ignorado", event);
                return;
            }

            String status    = data.path("status").asText("");
            String wompiTxId = data.path("id").asText("");
            BigDecimal monto = data.path("amount_in_cents").decimalValue()
                    .divide(BigDecimal.valueOf(100));
            String redirectUrl = data.path("redirect_url").asText("");

            log.info("Webhook Wompi - txId={} status={} redirectUrl={}", wompiTxId, status, redirectUrl);

            if (!"APPROVED".equals(status)) {
                log.info("Transacción Wompi no aprobada (status={})", status);
                return;
            }

            String[] partes = extraerRefDeUrl(redirectUrl);
            if (partes == null) {
                log.warn("Webhook Wompi: no se pudo extraer ref del redirect_url={}", redirectUrl);
                return;
            }

            String tenantId = partes[0];
            Long cobroId    = Long.parseLong(partes[1]);
            Long usuarioId  = Long.parseLong(partes[2]);

            TenantContext.setTenant(tenantId);
            try {
                pagoService.registrarYVerificarPagoOnline(cobroId, usuarioId, wompiTxId, monto, MetodoPago.WOMPI);
                log.info("Pago Wompi registrado para cobro {} tenant {}", cobroId, tenantId);
            } finally {
                TenantContext.clear();
            }

        } catch (Exception e) {
            log.error("Error procesando webhook Wompi: {}", e.getMessage());
        }
    }

    // ─── Confirmar Transacción (desde app Flutter) ───────────────────────────

    /**
     * Consulta el estado de la transacción en Wompi y la registra si está APPROVED.
     * Se invoca cuando el WebView de Flutter intercepta la URL de éxito antes del webhook.
     * La referencia se extrae del campo redirect_url de la transacción (mismo mecanismo que el webhook).
     */
    @Override
    public void confirmarTransaccion(TenantPasarela config, String transactionId) {
        validarConfig(config);

        String baseUrl = config.isSandbox() ? WOMPI_API_URL : WOMPI_PROD_URL;

        try {
            HttpClient client = HttpClient.newHttpClient();

            HttpRequest txRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/transactions/" + transactionId))
                    .header("Authorization", "Bearer " + config.getPrivateKey())
                    .GET()
                    .build();

            HttpResponse<String> txResponse = client.send(txRequest, HttpResponse.BodyHandlers.ofString());
            log.debug("Wompi confirmar txId={} httpStatus={}", transactionId, txResponse.statusCode());

            if (txResponse.statusCode() < 200 || txResponse.statusCode() >= 300) {
                log.error("Wompi confirmar error {}: {}", txResponse.statusCode(), txResponse.body());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error consultando transacción Wompi: " + txResponse.statusCode());
            }

            JsonNode txData = objectMapper.readTree(txResponse.body()).path("data");

            String status      = txData.path("status").asText("");
            BigDecimal monto   = txData.path("amount_in_cents").decimalValue()
                    .divide(BigDecimal.valueOf(100));
            String redirectUrl = txData.path("redirect_url").asText("");

            log.info("Wompi confirmar txId={} status={} redirectUrl={}", transactionId, status, redirectUrl);

            if (!"APPROVED".equals(status)) {
                log.warn("Transacción Wompi {} no aprobada (status={})", transactionId, status);
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El pago Wompi no está aprobado (status=" + status + ")");
            }

            String[] partes = extraerRefDeUrl(redirectUrl);
            if (partes == null) {
                log.warn("Wompi confirmar: no se pudo extraer ref del redirect_url={}", redirectUrl);
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "No se pudo resolver el cobro desde la transacción Wompi");
            }

            String tenantId = partes[0];
            Long cobroId    = Long.parseLong(partes[1]);
            Long usuarioId  = Long.parseLong(partes[2]);

            log.info("Wompi confirmar: txId={} → tenant={} cobro={} usuario={}", transactionId, tenantId, cobroId, usuarioId);

            TenantContext.setTenant(tenantId);
            try {
                pagoService.registrarYVerificarPagoOnline(cobroId, usuarioId, transactionId, monto, MetodoPago.WOMPI);
                log.info("Pago Wompi confirmado (app) para cobro {} tenant {}", cobroId, tenantId);
            } finally {
                TenantContext.clear();
            }

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error confirmando transacción Wompi {}: {}", transactionId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al confirmar pago Wompi: " + e.getMessage());
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    /**
     * Extrae el query param "ref" del redirect_url y lo divide por SEP.
     * Retorna [tenantId, cobroId, usuarioId] o null si no se puede parsear.
     */
    private String[] extraerRefDeUrl(String redirectUrl) {
        if (redirectUrl == null || redirectUrl.isBlank()) return null;
        try {
            String query = redirectUrl.contains("?") ? redirectUrl.substring(redirectUrl.indexOf('?') + 1) : "";
            for (String param : query.split("&")) {
                if (param.startsWith("ref=")) {
                    String ref = URLDecoder.decode(param.substring(4), StandardCharsets.UTF_8);
                    String[] partes = ref.split(SEP);
                    if (partes.length >= 3) return partes;
                }
            }
        } catch (Exception e) {
            log.error("Error parseando redirect_url '{}': {}", redirectUrl, e.getMessage());
        }
        return null;
    }

    private void validarConfig(TenantPasarela config) {
        if (config == null || config.getPrivateKey() == null || config.getPrivateKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,
                    "Este conjunto no tiene configurada la pasarela Wompi");
        }
    }

    private boolean verificarFirmaWompi(String payload, String signature, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computed = HexFormat.of().formatHex(hash);
            return computed.equalsIgnoreCase(signature);
        } catch (Exception e) {
            log.error("Error verificando firma Wompi: {}", e.getMessage());
            return false;
        }
    }

    private String resolverUrl(String override, String fallback) {
        return (override != null && !override.isBlank()) ? override : fallback;
    }
}
