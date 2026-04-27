package com.backendcr.residentialcomplex.dto.reserva;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public record ReservaRequest(
        @NotNull Long zonaComunId,
        Long propiedadId,
        @NotNull @Future LocalDate fecha,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFin,
        @Size(max = 500) String observaciones
) {}
