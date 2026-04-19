package com.backendcr.residentialcomplex.dto.propiedad;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;

public record PropiedadRequest(
        @NotEmpty List<PropiedadPathItemDto> propiedadPath
) {}
