package com.backendcr.residentialcomplex.dto.propiedad;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PropiedadPathItemDto(
        @NotNull Long tipoId,
        @NotBlank String valor
) {}
