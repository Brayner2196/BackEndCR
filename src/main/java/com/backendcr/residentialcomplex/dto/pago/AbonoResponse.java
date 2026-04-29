package com.backendcr.residentialcomplex.dto.pago;

import com.backendcr.residentialcomplex.entity.Abono;
import com.backendcr.residentialcomplex.entity.enums.EstadoPago;
import com.backendcr.residentialcomplex.entity.enums.MetodoPago;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AbonoResponse(
        Long id,
        Long propiedadId,
        Long usuarioId,
        String usuarioNombre,
        BigDecimal montoTotal,
        LocalDate fechaPago,
        MetodoPago metodoPago,
        String referencia,
        String urlComprobante,
        String notas,
        EstadoPago estado,
        String motivoRechazo,
        String fechaVerificacion,
        String creadoEn,
        List<MovimientoAbonoDto> movimientos
) {
    public static AbonoResponse from(Abono a, String usuarioNombre, List<MovimientoAbonoDto> movimientos) {
        return new AbonoResponse(
                a.getId(), a.getPropiedadId(), a.getUsuarioId(), usuarioNombre,
                a.getMontoTotal(), a.getFechaPago(), a.getMetodoPago(),
                a.getReferencia(), a.getUrlComprobante(), a.getNotas(),
                a.getEstado(), a.getMotivoRechazo(),
                a.getFechaVerificacion() != null ? a.getFechaVerificacion().toString() : null,
                a.getCreadoEn().toString(),
                movimientos
        );
    }
}
