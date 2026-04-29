package com.backendcr.residentialcomplex.dto.pago;

import com.backendcr.residentialcomplex.entity.MovimientoAbono;
import java.math.BigDecimal;

public record MovimientoAbonoDto(
        Long cobroId,
        BigDecimal montoAplicado,
        String descripcion
) {
    public static MovimientoAbonoDto from(MovimientoAbono m) {
        return new MovimientoAbonoDto(m.getCobroId(), m.getMontoAplicado(), m.getDescripcion());
    }
}
