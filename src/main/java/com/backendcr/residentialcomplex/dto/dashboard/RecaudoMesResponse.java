package com.backendcr.residentialcomplex.dto.dashboard;

import java.math.BigDecimal;

public record RecaudoMesResponse(
        int anio,
        int mes,
        int porcentaje,
        int puntosVariacion,
        BigDecimal recaudado,
        BigDecimal esperado
) {}
