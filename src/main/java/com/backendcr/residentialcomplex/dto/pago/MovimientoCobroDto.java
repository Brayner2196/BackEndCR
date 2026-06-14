package com.backendcr.residentialcomplex.dto.pago;

import com.backendcr.residentialcomplex.entity.Abono;
import com.backendcr.residentialcomplex.entity.MovimientoAbono;
import com.backendcr.residentialcomplex.entity.Pago;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Representa un movimiento de pago sobre un cobro específico.
 * Unifica tanto los Pagos directos (tipo=PAGO) como los
 * MovimientosAbono distribuidos a ese cobro (tipo=ABONO).
 */
public record MovimientoCobroDto(
        Long id,
        String tipo,             // "PAGO" | "ABONO"
        BigDecimal monto,
        String estado,           // estado del Pago o del Abono padre
        String fecha,            // fechaPago del pago/abono
        String metodoPago,       // puede ser null en movimientos de abono
        String referencia,
        String motivoRechazo,
        Instant creadoEn
) {
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static MovimientoCobroDto fromPago(Pago pago) {
        return new MovimientoCobroDto(
                pago.getId(),
                "PAGO",
                pago.getMontoPagado(),
                pago.getEstado().name(),
                pago.getFechaPago() != null ? pago.getFechaPago().format(DATE_FMT) : null,
                pago.getMetodoPago() != null ? pago.getMetodoPago().name() : null,
                pago.getReferencia(),
                pago.getMotivoRechazo(),
                pago.getCreadoEn()
        );
    }

    public static MovimientoCobroDto fromAbono(MovimientoAbono mov, Abono abono) {
        return new MovimientoCobroDto(
                mov.getId(),
                "ABONO",
                mov.getMontoAplicado(),
                abono.getEstado().name(),
                abono.getFechaPago() != null ? abono.getFechaPago().format(DATE_FMT) : null,
                abono.getMetodoPago() != null ? abono.getMetodoPago().name() : null,
                abono.getReferencia(),
                null,
                mov.getCreadoEn()
        );
    }
}
