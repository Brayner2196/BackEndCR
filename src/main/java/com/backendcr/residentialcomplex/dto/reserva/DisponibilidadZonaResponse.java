package com.backendcr.residentialcomplex.dto.reserva;

import java.time.LocalTime;
import java.util.List;

/**
 * Respuesta de disponibilidad horaria de una zona común para una fecha específica.
 * Incluye las franjas configuradas con su estado de ocupación real.
 */
public record DisponibilidadZonaResponse(
        Long zonaId,
        String fecha,
        String grupoEtiqueta,
        String grupoDias,
        String grupoNota,
        int bufferLimpiezaMinutos,
        List<FranjaDisponibilidad> franjas
) {
    public record FranjaDisponibilidad(
            String horaInicio,
            String horaFin,
            boolean libre,
            int capacidad,
            long ocupados,
            List<RangoOcupado> rangosOcupados
    ) {}

    public record RangoOcupado(
            String horaInicio,
            String horaFin
    ) {}
}
