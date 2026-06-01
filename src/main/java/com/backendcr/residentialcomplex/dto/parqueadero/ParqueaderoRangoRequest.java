package com.backendcr.residentialcomplex.dto.parqueadero;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

// Solo para parqueaderos PRIVADOS. Los comunales son un conteo en config.
public record ParqueaderoRangoRequest(
        @NotBlank String prefijo,
        @Min(1) int numeroInicio,
        @Min(1) int numeroFin,
        int padding              // 0 = sin padding, 2 = "P-01", 3 = "P-001"
) {}
