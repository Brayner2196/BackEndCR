package com.backendcr.residentialcomplex.dto.cartera;

import com.backendcr.residentialcomplex.entity.enums.OperadorLogico;

import java.util.List;

/** Regla de entrada a un estado, con sus condiciones. */
public record ReglaDto(
        Long id,
        String nombre,
        OperadorLogico operadorLogico,
        Integer orden,
        List<CondicionDto> condiciones
) {}
