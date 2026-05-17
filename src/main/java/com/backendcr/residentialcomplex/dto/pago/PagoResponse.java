package com.backendcr.residentialcomplex.dto.pago;

import com.backendcr.residentialcomplex.entity.Pago;
import com.backendcr.residentialcomplex.entity.enums.EstadoPago;
import com.backendcr.residentialcomplex.entity.enums.MetodoPago;
import java.math.BigDecimal;
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
        String fechaVerificacion,
        String creadoEn
) {
	private static final DateTimeFormatter FORMATTER_yyyyMMdd = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final DateTimeFormatter FORMATTER_yyyyMMddHHmmss = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	
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
                p.getFechaVerificacion() != null ? p.getFechaVerificacion().format(FORMATTER_yyyyMMddHHmmss) : null,
                p.getCreadoEn().toString()
        );
    }
}
