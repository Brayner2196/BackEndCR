package com.backendcr.residentialcomplex.dto.planpago;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record ConfiguracionPlanPagoRequest(
        boolean activo,

        @Min(1) @Max(24)
        int maxCuotas,

        boolean recargoFraccionamiento,

        @DecimalMin("0.00")
        BigDecimal porcentajeRecargo,

        boolean moraCongeladaDurantePlan,

        boolean aprobacionAutomatica
) {}
