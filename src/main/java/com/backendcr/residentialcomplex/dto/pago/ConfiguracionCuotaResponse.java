package com.backendcr.residentialcomplex.dto.pago;

import com.backendcr.residentialcomplex.entity.ConfiguracionCuota;
import com.backendcr.residentialcomplex.entity.enums.Periodicidad;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ConfiguracionCuotaResponse(
        Long id,
        Long tipoPropiedadId,
        Long propiedadId,
        Long tipoPropiedadCondicionId,
        Integer numeroDesde,
        Integer numeroHasta,
        BigDecimal monto,
        Periodicidad periodicidad,
        LocalDate fechaVigenciaDesde,
        LocalDate fechaVigenciaHasta,
        boolean activo
) {
    public static ConfiguracionCuotaResponse from(ConfiguracionCuota c) {
        return new ConfiguracionCuotaResponse(
                c.getId(),
                c.getTipoPropiedadId(),
                c.getPropiedadId(),
                c.getTipoPropiedadCondicionId(),
                c.getNumeroDesde(),
                c.getNumeroHasta(),
                c.getMonto(),
                c.getPeriodicidad(),
                c.getFechaVigenciaDesde(),
                c.getFechaVigenciaHasta(),
                c.isActivo()
        );
    }
}
