package com.backendcr.residentialcomplex.dto.presupuesto;

import com.backendcr.residentialcomplex.entity.CategoriaPresupuesto;

import java.math.BigDecimal;
import java.util.List;

public record CategoriaPresupuestoResponse(
        Long id,
        Long presupuestoId,
        String nombre,
        String descripcion,
        BigDecimal montoAsignado,
        /** Suma de todos los gastos registrados en esta categoría */
        BigDecimal montoEjecutado,
        /** montoAsignado - montoEjecutado */
        BigDecimal montoPendiente,
        /** Porcentaje de ejecución (0-100) */
        double porcentajeEjecucion,
        String color,
        String icono,
        /** Gastos individuales (solo se incluyen en la vista detalle) */
        List<GastoRegistradoResponse> gastos
) {
    /** Sin gastos — usado en listas y respuesta de presupuesto */
    public static CategoriaPresupuestoResponse from(CategoriaPresupuesto c, BigDecimal ejecutado) {
        return from(c, ejecutado, List.of());
    }

    /** Con gastos — usado en la vista detalle de la categoría */
    public static CategoriaPresupuestoResponse from(
            CategoriaPresupuesto c,
            BigDecimal ejecutado,
            List<GastoRegistradoResponse> gastos) {

        BigDecimal asignado = c.getMontoAsignado();
        BigDecimal pendiente = asignado.subtract(ejecutado);
        double pct = asignado.compareTo(BigDecimal.ZERO) > 0
                ? ejecutado.doubleValue() / asignado.doubleValue() * 100
                : 0.0;

        return new CategoriaPresupuestoResponse(
                c.getId(),
                c.getPresupuestoId(),
                c.getNombre(),
                c.getDescripcion(),
                asignado,
                ejecutado,
                pendiente,
                Math.min(pct, 100.0),
                c.getColor(),
                c.getIcono(),
                gastos
        );
    }
}
