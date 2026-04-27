package com.backendcr.residentialcomplex.dto.reserva;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ZonaComunRequest(
        @NotBlank @Size(max = 100) String nombre,
        @Size(max = 500) String descripcion,
        @PositiveOrZero Integer capacidad,
        Boolean activa
) {}
