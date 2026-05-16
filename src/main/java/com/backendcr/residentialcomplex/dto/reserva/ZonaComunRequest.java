package com.backendcr.residentialcomplex.dto.reserva;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ZonaComunRequest(
        @NotBlank @Size(max = 100) String nombre,
        @Size(max = 500) String descripcion,
        @PositiveOrZero Integer capacidad,
        Boolean activa,

        // Horario estándar — formato "HH:mm" o "HH:mm:ss" (String para evitar problemas de timezone con Jackson)
        String horaApertura,
        String horaCierre,

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
