package com.backendcr.residentialcomplex.dto.planpago;

import com.backendcr.residentialcomplex.entity.PlanPago;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PlanPagoResponse(
        Long id,
        Long propiedadId,
        Long residenteId,
        String residenteNombre,
        String propiedadIdentificador,
        BigDecimal montoTotalDeuda,
        int numeroCuotas,
        BigDecimal montoRecargo,
        BigDecimal montoTotalPlan,
        String estado,
        String cobrosIncluidos,
        String observaciones,
        String motivoRechazo,
        String notaAdmin,
        Instant fechaDecision,
        Instant creadoEn,
        List<CuotaPlanResponse> cuotas
) {
    /** Sin cuotas — para listados. */
    public static PlanPagoResponse from(PlanPago e, String residenteNombre, String propiedadIdentificador) {
        return new PlanPagoResponse(
                e.getId(),
                e.getPropiedadId(),
                e.getResidenteId(),
                residenteNombre,
                propiedadIdentificador,
                e.getMontoTotalDeuda(),
                e.getNumeroCuotas(),
                e.getMontoRecargo(),
                e.getMontoTotalPlan(),
                e.getEstado().name(),
                e.getCobrosIncluidos(),
                e.getObservaciones(),
                e.getMotivoRechazo(),
                e.getNotaAdmin(),
                e.getFechaDecision(),
                e.getCreadoEn(),
                List.of()
        );
    }

    /** Con cuotas — para detalle. */
    public static PlanPagoResponse from(PlanPago e, String residenteNombre,
                                        String propiedadIdentificador, List<CuotaPlanResponse> cuotas) {
        return new PlanPagoResponse(
                e.getId(),
                e.getPropiedadId(),
                e.getResidenteId(),
                residenteNombre,
                propiedadIdentificador,
                e.getMontoTotalDeuda(),
                e.getNumeroCuotas(),
                e.getMontoRecargo(),
                e.getMontoTotalPlan(),
                e.getEstado().name(),
                e.getCobrosIncluidos(),
                e.getObservaciones(),
                e.getMotivoRechazo(),
                e.getNotaAdmin(),
                e.getFechaDecision(),
                e.getCreadoEn(),
                cuotas
        );
    }
}
