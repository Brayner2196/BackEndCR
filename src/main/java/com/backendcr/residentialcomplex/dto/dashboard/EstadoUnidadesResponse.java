package com.backendcr.residentialcomplex.dto.dashboard;

public record EstadoUnidadesResponse(
        long total,
        long alDia,
        long porVencer,
        long enMora
) {}
