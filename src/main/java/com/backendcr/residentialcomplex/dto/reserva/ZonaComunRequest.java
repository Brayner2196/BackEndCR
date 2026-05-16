package com.backendcr.residentialcomplex.dto.reserva;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public record ZonaComunRequest(
        @NotBlank @Size(max = 100) String nombre,
        @Size(max = 500) String descripcion,
        @PositiveOrZero Integer capacidad,
        Boolean activa,

        // Horario estándar
        LocalTime horaApertura,
        LocalTime horaCierre,

        /** CSV de días: LUNES,MARTES,... Null = todos los días. */
        String diasDisponibles,

        // Reglas de duración (minutos)
        @PositiveOrZero Integer duracionMinMinutos,
        @PositiveOrZero Integer duracionMaxMinutos,

        // Reglas de anticipación (días)
        @PositiveOrZero Integer anticipacionMinDias,
        @PositiveOrZero Integer anticipacionMaxDias,

        Boolean requiereAprobacion
) {}
