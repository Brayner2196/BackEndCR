package com.backendcr.residentialcomplex.dto.presupuesto;

import com.backendcr.residentialcomplex.entity.GastoRegistrado;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

public record GastoRegistradoResponse(
        Long id,
        Long categoriaId,
        String descripcion,
        BigDecimal monto,
        LocalDate fecha,
        String comprobante,
        Long registradoPor,
        Instant creadoEn
) {
    public static GastoRegistradoResponse from(GastoRegistrado g) {
        return new GastoRegistradoResponse(
                g.getId(),
                g.getCategoriaId(),
                g.getDescripcion(),
                g.getMonto(),
                g.getFecha(),
                g.getComprobante(),
                g.getRegistradoPor(),
                g.getCreadoEn()
        );
    }
}
