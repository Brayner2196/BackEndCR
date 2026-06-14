package com.backendcr.residentialcomplex.dto.presupuesto;

import com.backendcr.residentialcomplex.entity.Presupuesto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PresupuestoResponse(
        Long id,
        int anio,
        String titulo,
        BigDecimal montoTotalPresupuestado,
        /** Suma de todos los gastos del presupuesto */
        BigDecimal montoTotalEjecutado,
        BigDecimal montoTotalPendiente,
        double porcentajeEjecucionGeneral,
        boolean activo,
        Instant creadoEn,
        Instant actualizadoEn,
        List<CategoriaPresupuestoResponse> categorias
) {
    /** Lista (sin categorías detalladas) */
    public static PresupuestoResponse fromSummary(Presupuesto p, BigDecimal ejecutado) {
        return build(p, ejecutado, List.of());
    }

    /** Detalle (con categorías y sus gastos) */
    public static PresupuestoResponse fromDetail(
            Presupuesto p,
            BigDecimal ejecutado,
            List<CategoriaPresupuestoResponse> categorias) {
        return build(p, ejecutado, categorias);
    }

    private static PresupuestoResponse build(
            Presupuesto p,
            BigDecimal ejecutado,
            List<CategoriaPresupuestoResponse> categorias) {

        BigDecimal presupuestado = p.getMontoTotalPresupuestado();
        BigDecimal pendiente = presupuestado.subtract(ejecutado);
        double pct = presupuestado.compareTo(BigDecimal.ZERO) > 0
                ? ejecutado.doubleValue() / presupuestado.doubleValue() * 100
                : 0.0;

        return new PresupuestoResponse(
                p.getId(),
                p.getAnio(),
                p.getTitulo(),
                presupuestado,
                ejecutado,
                pendiente,
                Math.min(pct, 100.0),
                p.isActivo(),
                p.getCreadoEn(),
                p.getActualizadoEn(),
                categorias
        );
    }
}
