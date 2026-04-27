package com.backendcr.residentialcomplex.dto.dashboard;

import java.util.List;

public record TendenciaResponse(
        List<TendenciaMesDto> meses,
        String tendencia
) {}
