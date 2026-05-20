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
import com.backendcr.residentialcomplex.repository.PeriodoCobroRepository;
import com.backendcr.residentialcomplex.service.PagoService;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.preference.Preference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

/**
 * Implementación de MercadoPago para la interfaz PasarelaService.
 *
 * Las credenciales se leen EXCLUSIVAMENTE de TenantPasarela (BD encriptada).
 * Ya no existe fallback a variables de entorno; cada tenant debe tener
 * su propia configuración registrada en la tabla tenant_pasarelas.
 *
 * URLs de retorno:
 *  - Si el tenant configuró successUrl/failureUrl/pendingUrl en TenantPasarela, se usan esas.
 *  - Si no, se usan los paths estándar de este backend (interceptados por el WebView Flutter).
 *
 * Notificación MP:
 *  - Se usa una URL por-tenant: {appBaseUrl}/api/pago/webhook/mp/{tenantId}
 *  - Esto permite al controller resolver la config correcta al recibir el webhook.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MercadoPagoServiceImpl implements PasarelaService {

    @Value("${app.base-url:https://api.conjuntosapp.com}")
    private String appBaseUrl;

    private final CobroRepository cobroRepo;
    private final PagoRepository pagoRepo;
    private final PagoService pagoService;
    private final PeriodoCobroRepository periodoRepo;

    @Override
    public TipoPasarela getTipo() {
        return TipoPasarela.MERCADO_PAGO;
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

        String accessToken = config.getPrivateKey();
        boolean sandbox    = config.isSandbox();
        String successUrl  = resolverUrl(config.getSuccessUrl(), appBaseUrl + "/api/mp/pago-exito");
        String failureUrl  = resolverUrl(config.getFailureUrl(), appBaseUrl + "/api/mp/pago-fallo");
        String pendingUrl  = resolverUrl(config.getPendingUrl(), appBaseUrl + "/api/mp/pago-pendiente");

        Cobro cobro = cobroRepo.findById(cobroId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cobro no encontrado"));

        if (cobro.getEstado() == EstadoCobro.PAGADO || cobro.getEstado() == EstadoCobro.EXONERADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este cobro ya está " + cobro.getEstado());
        }

        // Limpiar pago MP abandonado si existe
        pagoRepo.findByCobroIdAndEstado(cobroId, EstadoPago.PENDIENTE_VERIFICACION)
                .ifPresent(pagoExistente -> {
                    if (pagoExistente.getMetodoPago() == MetodoPago.MERCADO_PAGO) {
                        log.info("Eliminando pago MP abandonado {} para cobro {}", pagoExistente.getId(), cobroId);
                        pagoRepo.delete(pagoExistente);
                    } else {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "Ya existe un comprobante de pago pendiente de verificación para este cobro");
                    }
                });

        BigDecimal monto = (montoPersonalizado != null && montoPersonalizado.compareTo(BigDecimal.ZERO) > 0)
                ? montoPersonalizado
                : cobro.getMontoPendiente();

        String titulo = construirTitulo(cobro);

        try {
            MercadoPagoConfig.setAccessToken(accessToken);

            PreferenceItemRequest item = PreferenceItemRequest.builder()
                    .title(titulo)
                    .quantity(1)
                    .unitPrice(monto)
                    .currencyId("COP")
                    .build();

            PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                    .success(successUrl)
                    .failure(failureUrl)
                    .pending(pendingUrl)
                    .build();

            // external_reference codifica: tenantId|cobroId|usuarioId|MERCADO_PAGO
            String externalRef = tenantId + "|" + cobroId + "|" + usuarioId + "|MERCADO_PAGO";

            // notificationUrl por-tenant: permite que el webhook resuelva la config de BD
            String notificationUrl = appBaseUrl + "/api/pago/webhook/mp/" + tenantId;

            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(List.of(item))
                    .backUrls(backUrls)
                    .autoReturn("approved")
                    .externalReference(externalRef)
                    .notificationUrl(notificationUrl)
                    .build();

            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(preferenceRequest);

            log.info("Preferencia MP creada: {} para cobro {} tenant {}", preference.getId(), cobroId, tenantId);

            String url = sandbox ? preference.getSandboxInitPoint() : preference.getInitPoint();
            return new CheckoutResponse(url, TipoPasarela.MERCADO_PAGO);

        } catch (MPException | MPApiException e) {
            log.error("Error creando preferencia MP: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al crear preferencia de pago: " + e.getMessage());
        }
    }

    // ─── Procesar Webhook / Confirmación ────────────────────────────────────

    /**
     * Procesa una notificación de MercadoPago.
     * Requiere config no nula (credenciales del tenant en BD).
     * Si config es null, se ignora el evento con un warning (flujo legacy).
     */
    @Override
    public void procesarWebhook(TenantPasarela config, String paymentIdStr, String signature) {
        if (paymentIdStr == null || paymentIdStr.isBlank()) {
            log.warn("Webhook MP recibido sin paymentId, ignorando");
            return;
        }

        if (config == null || config.getPrivateKey() == null || config.getPrivateKey().isBlank()) {
            log.warn("Webhook MP sin config de tenant para paymentId={} — ignorado. " +
                     "Verificar que el tenant tiene MercadoPago configurado en BD.", paymentIdStr);
            return;
        }

        try {
            MercadoPagoConfig.setAccessToken(config.getPrivateKey());

            PaymentClient client = new PaymentClient();
            Payment mpPayment = client.get(Long.parseLong(paymentIdStr));

            String status      = mpPayment.getStatus();
            String externalRef = mpPayment.getExternalReference();

            log.info("Webhook MP - paymentId={} status={} externalRef={}", paymentIdStr, status, externalRef);

            if (externalRef == null || !externalRef.contains("|")) {
                log.warn("external_reference inválido: {}", externalRef);
                return;
            }

            String[] partes = externalRef.split("\\|");
            if (partes.length < 3) {
                log.warn("external_reference con formato inesperado: {}", externalRef);
                return;
            }

            String tenantId = partes[0];
            Long cobroId    = Long.parseLong(partes[1]);
            Long usuarioId  = Long.parseLong(partes[2]);

            if (!"approved".equals(status)) {
                log.info("Pago MP no aprobado (status={}), no se registra", status);
                return;
            }

            TenantContext.setTenant(tenantId);
            try {
                BigDecimal montoMP = mpPayment.getTransactionAmount();
                pagoService.registrarYVerificarPagoMP(cobroId, usuarioId, paymentIdStr, montoMP);
                log.info("Pago MP registrado para cobro {} tenant {}", cobroId, tenantId);
            } finally {
                TenantContext.clear();
            }

        } catch (MPException | MPApiException e) {
            log.error("Error consultando pago MP {}: {}", paymentIdStr, e.getMessage());
        } catch (NumberFormatException e) {
            log.error("paymentId no numérico: {}", paymentIdStr);
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void validarConfig(TenantPasarela config) {
        if (config == null || config.getPrivateKey() == null || config.getPrivateKey().isBlank()) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,
                    "Este conjunto no tiene configurada la pasarela MercadoPago");
        }
    }

    private String resolverUrl(String override, String fallback) {
        return (override != null && !override.isBlank()) ? override : fallback;
    }

    private String construirTitulo(Cobro cobro) {
        if (cobro.getPeriodoId() != null) {
            String periodoDesc = periodoRepo.findById(cobro.getPeriodoId())
                    .map(p -> p.getMes() + "/" + p.getAnio())
                    .orElse("Período " + cobro.getPeriodoId());
            return "Cuota administración - propiedad " + cobro.getPropiedadId() + " - " + periodoDesc;
        } else {
            String concepto = cobro.getConcepto() != null ? cobro.getConcepto().name() : "Cobro especial";
            return concepto + " - propiedad " + cobro.getPropiedadId();
        }
    }
}
