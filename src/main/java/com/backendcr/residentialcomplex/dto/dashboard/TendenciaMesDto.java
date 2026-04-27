package com.backendcr.residentialcomplex.dto.dashboard;

public record TendenciaMesDto(
        int anio,
        int mes,
        String etiqueta,
        int porcentaje
) {}
