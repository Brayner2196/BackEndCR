package com.backendcr.residentialcomplex.dto.cartera;

import com.backendcr.residentialcomplex.entity.enums.CampoCartera;
import com.backendcr.residentialcomplex.entity.enums.OperadorComparacion;

import java.math.BigDecimal;

/** Condición atómica de una regla de cartera. */
public record CondicionDto(
        CampoCartera campo,
        OperadorComparacion operador,
        BigDecimal valor
) {}
