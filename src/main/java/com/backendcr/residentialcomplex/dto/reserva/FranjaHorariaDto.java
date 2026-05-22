package com.backendcr.residentialcomplex.dto.reserva;

import jakarta.validation.constraints.NotNull;

public record FranjaHorariaDto(
        Long id,
        @NotNull String horaInicio,   // "HH:mm"
        @NotNull String horaFin,      // "HH:mm"
        int orden
) {}
