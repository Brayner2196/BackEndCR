package com.backendcr.residentialcomplex.dto.pago;

import com.backendcr.residentialcomplex.entity.Pago;
import com.backendcr.residentialcomplex.entity.enums.EstadoPago;
import com.backendcr.residentialcomplex.entity.enums.MetodoPago;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PagoResponse(
        Long id,
        Long cobroId,
        Long usuarioId,
        String usuarioNombre,
        BigDecimal montoPagado,
        LocalDate fechaPago,
        MetodoPago metodoPago,
        String referencia,
        String urlComprobante,
        String notas,
        EstadoPago estado,
        String motivoRechazo,
        String fechaVerificacion,
        String creadoEn
) {
    public static PagoResponse from(Pago p, String usuarioNombre) {
        return new PagoResponse(
                p.getId(), p.getCobroId(), p.getUsuarioId(), usuarioNombre,
                p.getMontoPagado(), p.getFechaPago(), p.getMetodoPago(),
                p.getReferencia(), p.getUrlComprobante(), p.getNotas(),
                p.getEstado(), p.getMotivoRechazo(),
                p.getFechaVerificacion() != null ? p.getFechaVerificacion().toString() : null,
                p.getCreadoEn().toString()
        );
    }
}
