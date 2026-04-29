package com.backendcr.residentialcomplex.dto.pago;

import java.math.BigDecimal;
import java.util.List;

/**
 * Preview de cómo se distribuirá un abono antes de confirmarlo.
 * El residente puede ver exactamente qué cobros se saldarán y si quedará saldo a favor.
 */
public record SimularAbonoResponse(
        BigDecimal montoAbono,
        BigDecimal saldoFavorPrevio,
        BigDecimal totalDisponible,
        List<MovimientoAbonoDto> distribucion,
        BigDecimal saldoFavorResultante
) {}
