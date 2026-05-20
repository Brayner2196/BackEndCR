package com.backendcr.residentialcomplex.service.pasarela;

import com.backendcr.residentialcomplex.dto.pasarela.*;
import com.backendcr.residentialcomplex.entity.Tenant;
import com.backendcr.residentialcomplex.entity.TenantPasarela;
import com.backendcr.residentialcomplex.entity.enums.TipoPasarela;
import com.backendcr.residentialcomplex.repository.TenantPasarelaRepository;
import com.backendcr.residentialcomplex.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

/**
 * Orquestador de alto nivel para pagos multi-pasarela.
 * Delega la lógica específica a la implementación correcta via PasarelaFactory.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasarelaOrchestrator {

    private final PasarelaFactory factory;
    private final TenantPasarelaRepository pasarelaRepo;
    private final TenantRepository tenantRepo;

    // ─── Checkout ─────────────────────────────────────────────────────────────

    public CheckoutResponse crearCheckout(
            String tenantSchema,
            Long cobroId,
            Long usuarioId,
            TipoPasarela tipoPasarela,
            BigDecimal monto) {

        PasarelaFactory.PasarelaConConfig par = factory.resolver(tenantSchema, tipoPasarela);
        log.info("Iniciando checkout {} para cobro {} tenant {}", tipoPasarela, cobroId, tenantSchema);
        return par.servicio().crearCheckout(par.config(), cobroId, usuarioId, tenantSchema, monto);
    }

    // ─── Pasarelas disponibles ─────────────────────────────────────────────────

    public List<PasarelaDisponibleResponse> obtenerDisponibles(String tenantSchema) {
        return pasarelaRepo.findActivasByTenantSchema(tenantSchema)
                .stream()
                .map(PasarelaDisponibleResponse::from)
                .toList();
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    /** Obtiene el email del usuario autenticado para registro de auditoría. */
    private String emailActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return "system";
        return auth.getName(); // en JWT suele ser el email/username
    }

    // ─── CRUD de pasarelas (admin/superadmin) ──────────────────────────────────

    @Transactional
    public PasarelaConfigResponse crearOActualizarPasarela(Long tenantId, PasarelaConfigRequest request) {
        Tenant tenant = tenantRepo.findById(tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant no encontrado"));

        TenantPasarela pasarela = pasarelaRepo
                .findByTenantSchemaAndTipo(tenant.getSchemaName(), request.tipoPasarela())
                .orElseGet(TenantPasarela::new);

        pasarela.setTenant(tenant);
        pasarela.setTipoPasarela(request.tipoPasarela());
        pasarela.setPublicKey(request.publicKey());
        pasarela.setPrivateKey(request.privateKey());
        pasarela.setWebhookSecret(request.webhookSecret());
        pasarela.setSandbox(request.sandbox());
        pasarela.setPrioridad(request.prioridad() != null ? request.prioridad() : 1);
        pasarela.setActiva(true);
        pasarela.setUpdatedBy(emailActual());
        if (request.successUrl() != null) pasarela.setSuccessUrl(request.successUrl());
        if (request.failureUrl() != null) pasarela.setFailureUrl(request.failureUrl());
        if (request.pendingUrl() != null)  pasarela.setPendingUrl(request.pendingUrl());

        return PasarelaConfigResponse.from(pasarelaRepo.save(pasarela));
    }

    public List<PasarelaConfigResponse> listarPasarelas(Long tenantId) {
        return pasarelaRepo.findByTenantIdOrderByPrioridad(tenantId)
                .stream()
                .map(PasarelaConfigResponse::from)
                .toList();
    }

    @Transactional
    public void toggleActiva(Long pasarelaId, boolean activa) {
        TenantPasarela p = pasarelaRepo.findById(pasarelaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pasarela no encontrada"));
        p.setActiva(activa);
        p.setUpdatedBy(emailActual());
        log.info("Pasarela {} {} por {}", pasarelaId, activa ? "activada" : "desactivada", emailActual());
        pasarelaRepo.save(p);
    }

    @Transactional
    public void eliminarPasarela(Long pasarelaId) {
        if (!pasarelaRepo.existsById(pasarelaId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pasarela no encontrada");
        }
        pasarelaRepo.deleteById(pasarelaId);
    }
}
