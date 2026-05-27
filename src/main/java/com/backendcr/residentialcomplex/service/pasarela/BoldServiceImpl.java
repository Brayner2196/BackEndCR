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
 * Implementación de Bold para la interfaz PasarelaService.
 *
 * Bold API: https://docs.bold.co
 *  - Para crear un link de pago: POST /online/link/v1
 *  - Webhook: verifica firma SHA-256 con el secreto del evento
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BoldServiceImpl implements PasarelaService {

    private static final String BOLD_API_URL  = "https://integrations.api.bold.co/online/link/v1";
    
    // URL base del backend — se usa como fallback para redirect_url de Bold
    @Value("${app.base-url}")
    private String appBaseUrl;

    private final CobroRepository cobroRepo;
    private final PagoRepository pagoRepo;
    private final PagoService pagoService;
    private final ObjectMapper objectMapper;

    @Override
    public TipoPasarela getTipo() {
        return TipoPasarela.BOLD;
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

        // Limpiar pago Bold abandonado si existe
        pagoRepo.findByCobroIdAndEstado(cobroId, EstadoPago.PENDIENTE_VERIFICACION)
                .ifPresent(pagoExistente -> {
                    if (pagoExistente.getMetodoPago() == MetodoPago.BOLD) {
                        log.info("Eliminando pago Bold abandonado {} para cobro {}", pagoExistente.getId(), cobroId);
                        pagoRepo.delete(pagoExistente);
                    } else {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "Ya existe un comprobante de pago pendiente para este cobro");
                    }
                });

        BigDecimal monto = (montoPersonalizado != null && montoPersonalizado.compareTo(BigDecimal.ZERO) > 0)
                ? montoPersonalizado
                : cobro.getMontoPendiente();

        // Bold trabaja en centavos
        long montoCentavos = monto.multiply(BigDecimal.valueOf(100)).longValue();

        String ref       = tenantId + "|" + cobroId + "|" + usuarioId + "|BOLD";
        // redirect_url debe ser HTTPS válido (Bold lo valida). Usa el endpoint de retorno
        // estándar, interceptado por el WebView Flutter en _exitoPath.
        String redirect  = resolverUrl(config.getSuccessUrl(), appBaseUrl + "/api/mp/pago-exito");
        String apiKey    = config.getPrivateKey();

        try {
            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "amount_type",    "CLOSE",
                    "amount",         montoCentavos,
                    "currency",       "COP",
                    "description",    "Pago cobro #" + cobroId + " - " + tenantId,
                    "order_id",       ref,
                    "redirect_url",   redirect,
                    "expiration_date", System.currentTimeMillis() / 1000 + 3600  // 1 hora
            ));

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BOLD_API_URL))
                    .header("Authorization", "x-api-key " + apiKey)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("Bold error {}: {}", response.statusCode(), response.body());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error al crear link de pago en Bold: " + response.statusCode());
            }

            JsonNode json = objectMapper.readTree(response.body());
            String checkoutUrl = json.path("url").asText();

            if (checkoutUrl == null || checkoutUrl.isBlank()) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Bold no devolvió URL de checkout");
            }

            log.info("Bold link creado para cobro {} tenant {}: {}", cobroId, tenantId, checkoutUrl);
            return new CheckoutResponse(checkoutUrl, TipoPasarela.BOLD);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creando link Bold: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al crear link de pago Bold: " + e.getMessage());
        }
    }

    // ─── Procesar Webhook ────────────────────────────────────────────────────

    /**
     * Bold firma los webhooks con SHA-256 usando el secreto del evento.
     * Header: bold-signature
     * Formato: payload JSON con "type" = "PAYMENT" y "data.payment"
     */
    @Override
    public void procesarWebhook(TenantPasarela config, String payload, String signature) {
        if (payload == null || payload.isBlank()) {
            log.warn("Webhook Bold sin payload");
            return;
        }

        try {
            // Verificar firma si tenemos el secret
            if (config != null && config.getWebhookSecret() != null && !config.getWebhookSecret().isBlank()) {
                if (!verificarFirmaBold(payload, signature, config.getWebhookSecret())) {
                    log.warn("Firma Bold inválida — payload rechazado");
                    return;
                }
            }

            JsonNode root  = objectMapper.readTree(payload);
            String type    = root.path("type").asText("");
            JsonNode data  = root.path("data").path("payment");

            if (!"PAYMENT".equals(type)) {
                log.info("Evento Bold '{}' ignorado", type);
                return;
            }

            String status    = data.path("status").asText("");
            String orderId   = data.path("order_id").asText("");
            String boldTxId  = data.path("id").asText("");
            BigDecimal monto = data.path("amount").decimalValue()
                    .divide(BigDecimal.valueOf(100));

            log.info("Webhook Bold - txId={} status={} orderId={}", boldTxId, status, orderId);

            if (!"APPROVED".equals(status)) {
                log.info("Transacción Bold no aprobada (status={})", status);
                return;
            }

            if (orderId == null || !orderId.contains("|")) {
                log.warn("order_id Bold inválido: {}", orderId);
                return;
            }

            String[] partes = orderId.split("\\|");
            if (partes.length < 3) return;

            String tenantId  = partes[0];
            Long cobroId     = Long.parseLong(partes[1]);
            Long usuarioId   = Long.parseLong(partes[2]);

            TenantContext.setTenant(tenantId);
            try {
                pagoService.registrarYVerificarPagoOnline(cobroId, usuarioId, boldTxId, monto, MetodoPago.BOLD);
                log.info("Pago Bold registrado para cobro {} tenant {}", cobroId, tenantId);
            } finally {
                TenantContext.clear();
            }

        } catch (Exception e) {
            log.error("Error procesando webhook Bold: {}", e.getMessage());
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void validarConfig(TenantPasarela config) {
        if (config == null || config.getPrivateKey() == null || config.getPrivateKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,
                    "Este conjunto no tiene configurada la pasarela Bold");
        }
    }

    private boolean verificarFirmaBold(String payload, String signature, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String computed = HexFormat.of().formatHex(hash);
            return computed.equalsIgnoreCase(signature);
        } catch (Exception e) {
            log.error("Error verificando firma Bold: {}", e.getMessage());
            return false;
        }
    }

    private String resolverUrl(String override, String fallback) {
        return (override != null && !override.isBlank()) ? override : fallback;
    }
}
