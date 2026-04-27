package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.dto.dashboard.*;
import com.backendcr.residentialcomplex.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('TENANT_ADMIN')")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardResumenResponse resumen() {
        return dashboardService.resumen();
    }

    @GetMapping("/pendientes")
    public PendientesHoyResponse pendientes() {
        return dashboardService.pendientesHoy();
    }

    @GetMapping("/recaudo")
    public RecaudoMesResponse recaudo() {
        return dashboardService.recaudoMes();
    }

    @GetMapping("/cartera")
    public CarteraVencidaResponse cartera() {
        return dashboardService.carteraVencida();
    }

    @GetMapping("/tendencia")
    public TendenciaResponse tendencia() {
        return dashboardService.tendencia();
    }

    @GetMapping("/unidades")
    public EstadoUnidadesResponse unidades() {
        return dashboardService.estadoUnidades();
    }

    @GetMapping("/pagos-por-verificar")
    public PagosPorVerificarResponse pagosPorVerificar() {
        return dashboardService.pagosPorVerificar();
    }
}
