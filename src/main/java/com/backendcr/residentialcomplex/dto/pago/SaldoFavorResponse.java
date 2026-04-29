package com.backendcr.residentialcomplex.dto.pago;

import com.backendcr.residentialcomplex.entity.SaldoFavor;
import java.math.BigDecimal;

public record SaldoFavorResponse(
        Long propiedadId,
        BigDecimal saldo
) {
    public static SaldoFavorResponse from(SaldoFavor sf) {
        return new SaldoFavorResponse(sf.getPropiedadId(), sf.getSaldo());
    }

    public static SaldoFavorResponse sinSaldo(Long propiedadId) {
        return new SaldoFavorResponse(propiedadId, BigDecimal.ZERO);
    }
}
