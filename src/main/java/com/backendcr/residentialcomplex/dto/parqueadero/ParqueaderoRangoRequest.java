package com.backendcr.residentialcomplex.dto.parqueadero;

import com.backendcr.residentialcomplex.entity.enums.TipoParqueadero;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ParqueaderoRangoRequest(
        @NotBlank String prefijo,
        @Min(1) int numeroInicio,
        @Min(1) int numeroFin,
        int padding,              // 0 = sin padding, 2 = "P-01", 3 = "P-001"
        @NotNull TipoParqueadero tipo
) {}
