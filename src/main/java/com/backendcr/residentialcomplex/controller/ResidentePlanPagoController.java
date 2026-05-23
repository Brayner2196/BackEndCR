package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.config.SecurityUtils;
import com.backendcr.residentialcomplex.dto.planpago.ConfiguracionPlanPagoResponse;
import com.backendcr.residentialcomplex.dto.planpago.PlanPagoResponse;
import com.backendcr.residentialcomplex.dto.planpago.SolicitarPlanRequest;
import com.backendcr.residentialcomplex.service.ConfiguracionPlanPagoService;
import com.backendcr.residentialcomplex.service.PlanPagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/residente/planes-pago")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ResidentePlanPagoController {

    private final PlanPagoService planService;
    private final ConfiguracionPlanPagoService configService;
    private final SecurityUtils securityUtils;

    /** Configuración pública: ¿está activo? ¿cuántas cuotas? ¿hay recargo? */
    @GetMapping("/configuracion")
    public ConfiguracionPlanPagoResponse obtenerConfig() {
        return configService.obtener();
    }

    /** Solicitar un nuevo plan de pago */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PlanPagoResponse solicitar(
            @Valid @RequestBody SolicitarPlanRequest req,
            @AuthenticationPrincipal String email) {
        Long residenteId = securityUtils.resolverUsuarioId(email);
        return planService.solicitar(residenteId, req);
    }

    /** Historial de todos mis planes */
    @GetMapping
    public List<PlanPagoResponse> misPlanes(@AuthenticationPrincipal String email) {
        Long residenteId = securityUtils.resolverUsuarioId(email);
        return planService.misPlanes(residenteId);
    }

    /** Mi plan activo con cuotas */
    @GetMapping("/activo")
    public PlanPagoResponse miPlanActivo(@AuthenticationPrincipal String email) {
        Long residenteId = securityUtils.resolverUsuarioId(email);
        return planService.miPlanActivo(residenteId);
    }
}
