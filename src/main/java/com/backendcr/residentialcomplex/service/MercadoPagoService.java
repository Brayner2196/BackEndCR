package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.entity.Cobro;
import com.backendcr.residentialcomplex.entity.Pago;
import com.backendcr.residentialcomplex.entity.enums.EstadoCobro;
import com.backendcr.residentialcomplex.entity.enums.EstadoPago;
import com.backendcr.residentialcomplex.entity.enums.MetodoPago;
import com.backendcr.residentialcomplex.repository.CobroRepository;
import com.backendcr.residentialcomplex.repository.PagoRepository;
import com.backendcr.residentialcomplex.repository.PeriodoCobroRepository;
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
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MercadoPagoService {

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @Value("${mercadopago.webhook-url}")
    private String webhookUrl;

    @Value("${mercadopago.success-url:conjuntosapp://pago/exito}")
    private String successUrl;

    @Value("${mercadopago.failure-url:conjuntosapp://pago/fallo}")
    private String failureUrl;

    @Value("${mercadopago.pending-url:conjuntosapp://pago/pendiente}")
    private String pendingUrl;

    @Value("${mercadopago.sandbox:true}")
    private boolean sandbox;

    private final CobroRepository cobroRepo;
    private final PagoRepository pagoRepo;
    private final PagoService pagoService;
    private final PeriodoCobroRepository periodoRepo;

    @PostConstruct
    public void init() {
        MercadoPagoConfig.setAccessToken(accessToken);
    }

    // ─── Crear preferencia de pago ────────────────────────────────────────────

    /**
     * Crea una preferencia de MercadoPago para pagar un cobro específico.
     * Devuelve la URL del checkout (init_point para producción, sandbox_init_point para pruebas).
     *
     * external_reference codifica: {tenantId}|{cobroId}|{usuarioId}
     * para que el webhook pueda recuperar el contexto multitenant sin header.
     */
    public String crearPreferencia(Long cobroId, Long usuarioId, String tenantId) {
        Cobro cobro = cobroRepo.findById(cobroId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cobro no encontrado"));

        if (cobro.getEstado() == EstadoCobro.PAGADO || cobro.getEstado() == EstadoCobro.EXONERADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este cobro ya está " + cobro.getEstado());
        }

        // Si hay un pago PENDIENTE_VERIFICACION de MP, puede ser un checkout abandonado.
        // Lo eliminamos para permitir un nuevo intento. Si el usuario SÍ pagó, el webhook
        // llegará igualmente y lo verificará automáticamente.
        // Si es un pago manual (comprobante), lo respetamos y bloqueamos.
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

        BigDecimal monto = cobro.getMontoPendiente();
        String periodoDesc = periodoRepo.findById(cobro.getPeriodoId())
                .map(p -> p.getMes() + "/" + p.getAnio())
                .orElse("Período " + cobro.getPeriodoId());
        String titulo = "Cuota administración - " + cobro.getPropiedadId() + " - " + periodoDesc;

        try {
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

            String externalRef = tenantId + "|" + cobroId + "|" + usuarioId;

            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .items(List.of(item))
                    .backUrls(backUrls)
                    .autoReturn("approved")
                    .externalReference(externalRef)
                    .notificationUrl(webhookUrl + "/api/mp/webhook")
                    .build();

            PreferenceClient client = new PreferenceClient();
            Preference preference = client.create(preferenceRequest);

            log.info("Preferencia MP creada: {} para cobro {} tenant {}", preference.getId(), cobroId, tenantId);

            // En sandbox usa sandbox_init_point; en producción usa init_point
            return sandbox ? preference.getSandboxInitPoint() : preference.getInitPoint();

        } catch (MPException | MPApiException e) {
            log.error("Error creando preferencia MP: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al crear preferencia de pago: " + e.getMessage());
        }
    }

    // ─── Procesar webhook ─────────────────────────────────────────────────────

    /**
     * Recibe la notificación de MercadoPago, consulta el pago real y,
     * si fue aprobado, registra y verifica el Pago automáticamente en el cobro.
     *
     * El webhook llega sin header X-Tenant-ID, por eso se decodifica desde external_reference.
     */
    @Transactional
    public void procesarWebhook(String paymentId) {
        if (paymentId == null || paymentId.isBlank()) {
            log.warn("Webhook MP recibido sin paymentId, ignorando");
            return;
        }

        try {
            MercadoPagoConfig.setAccessToken(accessToken);
            PaymentClient client = new PaymentClient();
            Payment mpPayment = client.get(Long.parseLong(paymentId));

            String status = mpPayment.getStatus();
            String externalRef = mpPayment.getExternalReference();

            log.info("Webhook MP - paymentId={} status={} externalRef={}", paymentId, status, externalRef);

            if (externalRef == null || !externalRef.contains("|")) {
                log.warn("external_reference inválido: {}", externalRef);
                return;
            }

            String[] partes = externalRef.split("\\|");
            if (partes.length != 3) {
                log.warn("external_reference con formato inesperado: {}", externalRef);
                return;
            }

            String tenantId = partes[0];
            Long cobroId   = Long.parseLong(partes[1]);
            Long usuarioId = Long.parseLong(partes[2]);

            // Configurar el contexto de tenant para que JPA use el schema correcto
            TenantContext.setTenant(tenantId);

            try {
                if (!"approved".equals(status)) {
                    log.info("Pago MP no aprobado (status={}), no se registra", status);
                    return;
                }

                // Evitar duplicados: si ya hay pago verificado para este cobro, ignorar
                boolean yaVerificado = pagoRepo.findAllByCobroId(cobroId).stream()
                        .anyMatch(p -> p.getEstado() == EstadoPago.VERIFICADO);
                if (yaVerificado) {
                    log.info("Cobro {} ya tiene pago verificado, webhook ignorado", cobroId);
                    return;
                }

                BigDecimal montoMP = mpPayment.getTransactionAmount();

                // Crear el pago en estado PENDIENTE_VERIFICACION
                Pago pago = new Pago();
                pago.setCobroId(cobroId);
                pago.setUsuarioId(usuarioId);
                pago.setMontoPagado(montoMP);
                pago.setFechaPago(LocalDate.now());
                pago.setMetodoPago(MetodoPago.MERCADO_PAGO);
                pago.setReferencia("MP-" + paymentId);
                pago.setNotas("Pago automático vía MercadoPago");
                pago.setEstado(EstadoPago.PENDIENTE_VERIFICACION);
                Pago saved = pagoRepo.save(pago);

                // Auto-verificar: MP ya confirmó el pago, no requiere revisión manual
                pagoService.autoVerificar(saved.getId());

                log.info("Pago {} registrado y verificado automáticamente para cobro {} tenant {}",
                        saved.getId(), cobroId, tenantId);

            } finally {
                TenantContext.clear();
            }

        } catch (MPException | MPApiException e) {
            log.error("Error consultando pago MP {}: {}", paymentId, e.getMessage());
        } catch (NumberFormatException e) {
            log.error("paymentId no numérico: {}", paymentId);
        }
    }
}
