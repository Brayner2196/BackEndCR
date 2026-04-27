package com.backendcr.residentialcomplex.dto.dashboard;

public record DashboardResumenResponse(
        PendientesHoyResponse pendientesHoy,
        RecaudoMesResponse recaudoMes,
        CarteraVencidaResponse carteraVencida,
        PagosPorVerificarResponse pagosPorVerificar,
        TendenciaResponse tendencia,
        EstadoUnidadesResponse estadoUnidades
) {}
