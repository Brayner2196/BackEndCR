package com.backendcr.residentialcomplex.dto.dashboard;

import java.math.BigDecimal;

public record CarteraVencidaResponse(
        BigDecimal monto,
        BigDecimal variacionMonto,
        long unidadesEnMora
) {}
