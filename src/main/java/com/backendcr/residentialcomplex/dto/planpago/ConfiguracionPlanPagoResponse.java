package com.backendcr.residentialcomplex.dto.planpago;

import com.backendcr.residentialcomplex.entity.ConfiguracionPlanPago;

import java.math.BigDecimal;
import java.time.Instant;

public record ConfiguracionPlanPagoResponse(
        Long id,
        boolean activo,
        int maxCuotas,
        boolean recargoFraccionamiento,
        BigDecimal porcentajeRecargo,
        boolean moraCongeladaDurantePlan,
        boolean aprobacionAutomatica,
        Instant actualizadoEn
) {
    public static ConfiguracionPlanPagoResponse from(ConfiguracionPlanPago e) {
        return new ConfiguracionPlanPagoResponse(
                e.getId(),
                e.isActivo(),
                e.getMaxCuotas(),
                e.isRecargoFraccionamiento(),
                e.getPorcentajeRecargo(),
                e.isMoraCongeladaDurantePlan(),
                e.isAprobacionAutomatica(),
                e.getActualizadoEn()
        );
    }
}
