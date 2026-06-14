package com.backendcr.residentialcomplex.dto.pago;

import com.backendcr.residentialcomplex.entity.Pago;
import com.backendcr.residentialcomplex.entity.enums.EstadoPago;
import com.backendcr.residentialcomplex.entity.enums.MetodoPago;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public record PagoResponse(
        Long id,
        Long cobroId,
        Long usuarioId,
        String usuarioNombre,
        BigDecimal montoPagado,
        String fechaPago,
        MetodoPago metodoPago,
        String referencia,
        String urlComprobante,
        EstadoPago estado,
        String motivoRechazo,
        Instant fechaVerificacion,
        Instant creadoEn
) {
	private static final DateTimeFormatter FORMATTER_yyyyMMdd = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static PagoResponse from(Pago p, String usuarioNombre) {
        return new PagoResponse(
                p.getId(), 
                p.getCobroId(), 
                p.getUsuarioId(), 
                usuarioNombre,
                p.getMontoPagado(), 
                p.getFechaPago() != null ? p.getFechaPago().format(FORMATTER_yyyyMMdd) : null, 
                p.getMetodoPago(),
                p.getReferencia(), 
                p.getUrlComprobante(),
                p.getEstado(), 
                p.getMotivoRechazo(),
                p.getFechaVerificacion(),
                p.getCreadoEn()
        );
    }
}
