package com.backendcr.residentialcomplex.service.cartera;

import com.backendcr.residentialcomplex.entity.enums.CampoCartera;

import java.math.BigDecimal;

/**
 * Métricas financieras congeladas de una propiedad en un instante, usadas por el
 * motor para resolver su estado de cartera.
 */
public record MetricasCartera(
        int diasVencidoMax,
        BigDecimal montoAdeudado,
        int numCobrosVencidos,
        int numPeriodosVencidos
) {

    /** Resuelve el valor numérico de un campo evaluable. */
    public BigDecimal valor(CampoCartera campo) {
        return switch (campo) {
            case DIAS_VENCIDO_MAX      -> BigDecimal.valueOf(diasVencidoMax);
            case MONTO_ADEUDADO        -> montoAdeudado != null ? montoAdeudado : BigDecimal.ZERO;
            case NUM_PERIODOS_VENCIDOS -> BigDecimal.valueOf(numPeriodosVencidos);
            case NUM_COBROS_VENCIDOS   -> BigDecimal.valueOf(numCobrosVencidos);
        };
    }

    public static MetricasCartera vacia() {
        return new MetricasCartera(0, BigDecimal.ZERO, 0, 0);
    }
}
