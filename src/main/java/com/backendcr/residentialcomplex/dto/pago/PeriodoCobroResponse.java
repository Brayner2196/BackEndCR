package com.backendcr.residentialcomplex.dto.pago;

import com.backendcr.residentialcomplex.entity.PeriodoCobro;
import com.backendcr.residentialcomplex.entity.enums.EstadoPeriodo;
import java.time.LocalDate;

public record PeriodoCobroResponse(
        Long id,
        int anio,
        int mes,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        LocalDate fechaLimitePago,
        EstadoPeriodo estado,
        String creadoEn
) {
    public static PeriodoCobroResponse from(PeriodoCobro p) {
        return new PeriodoCobroResponse(
                p.getId(), p.getAnio(), p.getMes(),
                p.getFechaInicio(), p.getFechaFin(), p.getFechaLimitePago(),
                p.getEstado(), p.getCreadoEn().toString()
        );
    }
}
