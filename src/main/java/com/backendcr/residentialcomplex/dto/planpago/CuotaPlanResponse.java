package com.backendcr.residentialcomplex.dto.planpago;

import com.backendcr.residentialcomplex.config.TenantClock;
import com.backendcr.residentialcomplex.entity.CuotaPlan;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CuotaPlanResponse(
        Long id,
        Long planId,
        int numeroCuota,
        BigDecimal monto,
        LocalDate fechaVencimiento,
        String estado,
        LocalDate fechaPago,
        String notaPago,
        boolean vencida
) {
    public static CuotaPlanResponse from(CuotaPlan e) {
        boolean vencida = e.getEstado().name().equals("PENDIENTE")
                && e.getFechaVencimiento().isBefore(TenantClock.hoy());
        return new CuotaPlanResponse(
                e.getId(),
                e.getPlanId(),
                e.getNumeroCuota(),
                e.getMonto(),
                e.getFechaVencimiento(),
                e.getEstado().name(),
                e.getFechaPago(),
                e.getNotaPago(),
                vencida
        );
    }
}
