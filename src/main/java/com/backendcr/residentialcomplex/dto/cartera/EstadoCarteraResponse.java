package com.backendcr.residentialcomplex.dto.cartera;

import java.math.BigDecimal;
import java.time.Instant;

/** Estado de cartera vigente de una propiedad. */
public record EstadoCarteraResponse(
        Long propiedadId,
        String estadoCodigo,
        String estadoNombre,
        String color,
        boolean esPositivo,
        int diasVencidoMax,
        BigDecimal montoAdeudado,
        Instant calculadoEn
) {}
