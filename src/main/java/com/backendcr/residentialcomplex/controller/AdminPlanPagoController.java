package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.config.SecurityUtils;
import com.backendcr.residentialcomplex.dto.planpago.*;
import com.backendcr.residentialcomplex.service.ConfiguracionPlanPagoService;
import com.backendcr.residentialcomplex.service.PlanPagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/planes-pago")
@PreAuthorize("hasRole('TENANT_ADMIN')")
@RequiredArgsConstructor
public class AdminPlanPagoController {

    private final PlanPagoService planService;
    private final ConfiguracionPlanPagoService configService;
    private final SecurityUtils securityUtils;

    // ── Configuración ─────────────────────────────────────────────

    @GetMapping("/configuracion")
    public ConfiguracionPlanPagoResponse obtenerConfig() {
        return configService.obtener();
    }

    @PutMapping("/configuracion")
    public ConfiguracionPlanPagoResponse guardarConfig(
            @Valid @RequestBody ConfiguracionPlanPagoRequest req) {
        return configService.guardar(req);
    }

    // ── Planes ────────────────────────────────────────────────────

    /** Lista todos los planes. Filtra por estado si se pasa ?estado=PENDIENTE */
    @GetMapping
    public List<PlanPagoResponse> listar(
            @RequestParam(required = false) String estado) {
        return planService.listarTodos(estado);
    }

    @GetMapping("/{id}")
    public PlanPagoResponse detalle(@PathVariable Long id) {
        return planService.obtenerDetalle(id);
    }

    /** Aprobar o rechazar una solicitud de plan */
    @PostMapping("/{id}/decidir")
    public PlanPagoResponse decidir(
            @PathVariable Long id,
            @Valid @RequestBody DecidirPlanRequest req,
            @AuthenticationPrincipal String email) {
        Long adminId = securityUtils.resolverUsuarioId(email);
        return planService.decidir(id, adminId, req);
    }

    /** Marcar una cuota individual como pagada */
    @PostMapping("/{planId}/cuotas/{cuotaId}/pagar")
    public CuotaPlanResponse marcarCuotaPagada(
            @PathVariable Long planId,
            @PathVariable Long cuotaId,
            @RequestBody(required = false) Map<String, String> body) {
        String nota = body != null ? body.get("nota") : null;
        return planService.marcarCuotaPagada(planId, cuotaId, nota);
    }

    /** Cancelar un plan activo o pendiente */
    @PostMapping("/{id}/cancelar")
    public PlanPagoResponse cancelar(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        String nota = body != null ? body.get("nota") : null;
        return planService.cancelarPlan(id, nota);
    }
}
