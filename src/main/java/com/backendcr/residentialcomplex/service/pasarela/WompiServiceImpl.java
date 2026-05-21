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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WompiServiceImpl implements PasarelaService {

    private static final String WOMPI_API_URL  = "https://sandbox.wompi.co/v1";
    private static final String WOMPI_PROD_URL = "https://production.wompi.co/v1";

    // Wompi usa el mismo dominio de checkout para sandbox y producción.
    // La diferencia es que en sandbox las llaves tienen prefijo pub_test_ y
    // los IDs de link tienen prefijo test_. No existe checkout-sandbox.wompi.co.
    private static final String WOMPI_CHECKOUT_BASE = "https://checkout.wompi.co/l/";

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

        // Wompi trabaja en centavos (COP)
        long montoCentavos = monto.multiply(BigDecimal.valueOf(100)).longValue();

        String baseUrl  = config.isSandbox() ? WOMPI_API_URL : WOMPI_PROD_URL;
        String ref      = tenantId + "|" + cobroId + "|" + usuarioId + "|WOMPI";
        // redirect_url debe ser HTTPS válido. Se usa el endpoint de retorno MP
        // (interceptado por el WebView Flutter en _exitoPath=/api/mp/pago-exito).
        // Si el tenant configuró su propia URL en TenantPasarela.successUrl, esa toma precedencia.
        String redirect = resolverUrl(config.getSuccessUrl(), appBaseUrl + "/api/mp/pago-exito");

        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "name",           "Pago cobro #" + cobroId,
                    "description",    "Cuota conjuntos residenciales - tenant " + tenantId,
                    "single_use",     true,
                    "collect_shipping", false,
                    "currency",       "COP",
                    "amount_in_cents", montoCentavos,
                    "redirect_url",   redirect,
                    "reference",      ref
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

            // Wompi devuelve solo el `id` del link; la URL de checkout se construye
            // concatenando la base correspondiente (sandbox o producción) + el id.
            String linkId = json.path("data").path("id").asText("");

            if (linkId.isBlank()) {
                log.error("Wompi body sin id de link: {}", response.body());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Wompi no devolvió URL de checkout");
            }

            String checkoutUrl = WOMPI_CHECKOUT_BASE + linkId;

            log.info("Wompi link creado para cobro {} tenant {}: {}", cobroId, tenantId, checkoutUrl);
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
     */
    @Override
    public void procesarWebhook(TenantPasarela config, String payload, String signature) {
        if (payload == null || payload.isBlank()) {
            log.warn("Webhook Wompi sin payload");
            return;
        }

        try {
            // Verificar firma si tenemos el secret
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

            String status     = data.path("status").asText("");
            String reference  = data.path("reference").asText("");
            String wompiTxId  = data.path("id").asText("");
            BigDecimal monto  = data.path("amount_in_cents").decimalValue()
                    .divide(BigDecimal.valueOf(100));

            log.info("Webhook Wompi - txId={} status={} reference={}", wompiTxId, status, reference);

            if (!"APPROVED".equals(status)) {
                log.info("Transacción Wompi no aprobada (status={})", status);
                return;
            }

            if (reference == null || !reference.contains("|")) {
                log.warn("Referencia Wompi inválida: {}", reference);
                return;
            }

            String[] partes = reference.split("\\|");
            if (partes.length < 3) return;

            String tenantId  = partes[0];
            Long cobroId     = Long.parseLong(partes[1]);
            Long usuarioId   = Long.parseLong(partes[2]);

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
     * Consulta el estado de una transacción Wompi en tiempo real y la registra si está APPROVED.
     * Se usa cuando el WebView intercepta la URL de éxito y necesita confirmación síncrona
     * antes de que llegue el webhook asincrónico.
     */
    @Override
    public void confirmarTransaccion(TenantPasarela config, String transactionId) {
        validarConfig(config);

        String baseUrl = config.isSandbox() ? WOMPI_API_URL : WOMPI_PROD_URL;

        try {
            HttpClient client = HttpClient.newHttpClient();

            // ── 1. Obtener detalles de la transacción ────────────────────────────
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

            String status       = txData.path("status").asText("");
            BigDecimal monto    = txData.path("amount_in_cents").decimalValue()
                    .divide(BigDecimal.valueOf(100));
            // NOTA: txData.reference es la referencia auto-generada por Wompi (ej: test_XYZ_timestamp_random).
            // Nuestra referencia personalizada (tenantId|cobroId|usuarioId|WOMPI) está en el payment_link.
            String paymentLinkId = txData.path("payment_link_id").asText("");

            log.info("Wompi confirmar txId={} status={} paymentLinkId={}", transactionId, status, paymentLinkId);

            if (!"APPROVED".equals(status)) {
                log.warn("Transacción Wompi {} no aprobada (status={})", transactionId, status);
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El pago Wompi no está aprobado (status=" + status + ")");
            }

            // ── 2. Obtener la referencia personalizada desde el payment link ─────
            // GET /v1/payment_links/{id} → data.reference = "tenantId|cobroId|usuarioId|WOMPI"
            if (paymentLinkId.isBlank()) {
                log.error("Wompi: transacción {} sin payment_link_id", transactionId);
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "La transacción Wompi no tiene payment_link_id asociado");
            }

            HttpRequest linkRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/payment_links/" + paymentLinkId))
                    .header("Authorization", "Bearer " + config.getPrivateKey())
                    .GET()
                    .build();

            HttpResponse<String> linkResponse = client.send(linkRequest, HttpResponse.BodyHandlers.ofString());
            log.debug("Wompi payment_link {} httpStatus={}", paymentLinkId, linkResponse.statusCode());

            if (linkResponse.statusCode() < 200 || linkResponse.statusCode() >= 300) {
                log.error("Wompi: error obteniendo payment_link {}: {}", paymentLinkId, linkResponse.body());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error consultando link de pago Wompi");
            }

            String reference = objectMapper.readTree(linkResponse.body())
                    .path("data").path("reference").asText("");

            log.info("Wompi referencia del payment_link {}: {}", paymentLinkId, reference);

            // ── 3. Parsear la referencia y registrar el pago ─────────────────────
            if (reference == null || !reference.contains("|")) {
                log.warn("Referencia Wompi inválida en payment_link {}: {}", paymentLinkId, reference);
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Referencia de pago Wompi inválida: " + reference);
            }

            String[] partes = reference.split("\\|");
            if (partes.length < 3) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Formato de referencia Wompi inválido: " + reference);
            }

            String tenantId = partes[0];
            Long cobroId    = Long.parseLong(partes[1]);
            Long usuarioId  = Long.parseLong(partes[2]);

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
