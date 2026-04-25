package com.backendcr.residentialcomplex.dto.pago;

import java.math.BigDecimal;
import java.util.List;

public record EstadoCuentaResponse(
        BigDecimal totalPendiente,
        BigDecimal totalVencido,
        int cobrosVencidos,
        int cobrosPendientes,
        String ultimoPago,
        List<CobroResponse> cobrosActivos
) {}
